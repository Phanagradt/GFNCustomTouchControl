#!/usr/bin/env python3
"""
Deterministically generates a standard Android debug PKCS#12 keystore (alias:
androiddebugkey, password: android) from a repository seed when neither
NEXUSPAD_KEYSTORE_BASE64 nor debug.keystore.base64 is present in CI.

Using a fixed keypair + fixed X.509 certificate metadata guarantees every CI
build produces the exact same APK signing certificate SHA-256 so Android
in-place updates succeed without "App not installed as package appears to be
invalid". For private production releases, set the NEXUSPAD_KEYSTORE_BASE64
GitHub Actions secret instead.
"""

import base64
import hashlib
import hmac
import os
import subprocess
import sys
import tempfile

# NIST P-256 (secp256r1 / prime256v1) parameters
P = 0xFFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF
A = P - 3
N = 0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551
GX = 0x6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296
GY = 0x4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5


def ec_add(p1, p2):
    if p1 is None:
        return p2
    if p2 is None:
        return p1
    x1, y1 = p1
    x2, y2 = p2
    if x1 == x2:
        if (y1 + y2) % P == 0:
            return None
        lam = ((3 * x1 * x1 + A) * pow(2 * y1, -1, P)) % P
    else:
        lam = ((y2 - y1) * pow(x2 - x1, -1, P)) % P
    x3 = (lam * lam - x1 - x2) % P
    y3 = (lam * (x1 - x3) - y1) % P
    return (x3, y3)


def ec_mul(k, point):
    result = None
    addend = point
    while k:
        if k & 1:
            result = ec_add(result, addend)
        addend = ec_add(addend, addend)
        k >>= 1
    return result


def der_len(n: int) -> bytes:
    if n < 0x80:
        return bytes([n])
    b = n.to_bytes((n.bit_length() + 7) // 8, "big")
    return bytes([0x80 | len(b)]) + b


def der_tlv(tag: int, body: bytes) -> bytes:
    return bytes([tag]) + der_len(len(body)) + body


def der_int(val: int) -> bytes:
    if val == 0:
        return der_tlv(0x02, b"\x00")
    b = val.to_bytes((val.bit_length() + 7) // 8, "big")
    if b[0] & 0x80:
        b = b"\x00" + b
    return der_tlv(0x02, b)


def der_seq(*items: bytes) -> bytes:
    return der_tlv(0x30, b"".join(items))


def der_set(*items: bytes) -> bytes:
    return der_tlv(0x31, b"".join(items))


def der_bit_string(data: bytes, unused_bits: int = 0) -> bytes:
    return der_tlv(0x03, bytes([unused_bits]) + data)


def der_octet_string(data: bytes) -> bytes:
    return der_tlv(0x04, data)


def der_oid(oid: str) -> bytes:
    parts = [int(x) for x in oid.split(".")]
    out = bytearray([parts[0] * 40 + parts[1]])
    for p in parts[2:]:
        if p == 0:
            out.append(0)
        else:
            chunks = []
            while p:
                chunks.append(p & 0x7F)
                p >>= 7
            chunks.reverse()
            for i, c in enumerate(chunks):
                out.append(c | (0x80 if i < len(chunks) - 1 else 0))
    return der_tlv(0x06, bytes(out))


def der_rdn(oid: str, value: str, tag: int = 0x0C) -> bytes:
    return der_set(der_seq(der_oid(oid), der_tlv(tag, value.encode("utf-8"))))


def deterministic_ecdsa_sign(d: int, message: bytes) -> bytes:
    z = int.from_bytes(hashlib.sha256(message).digest(), "big")
    counter = 0
    while True:
        k_bytes = hmac.new(
            d.to_bytes(32, "big"),
            hashlib.sha256(message).digest() + counter.to_bytes(4, "big"),
            hashlib.sha256,
        ).digest()
        k = (int.from_bytes(k_bytes, "big") % (N - 1)) + 1
        rx, _ = ec_mul(k, (GX, GY))
        r = rx % N
        if r == 0:
            counter += 1
            continue
        s = (pow(k, -1, N) * (z + r * d)) % N
        if s == 0:
            counter += 1
            continue
        # Low-S canonical form
        if s > N // 2:
            s = N - s
        return der_seq(der_int(r), der_int(s))


def build_key_and_cert(seed: str):
    normalized = seed.strip().lower().encode("utf-8")
    d_digest = hashlib.sha256(b"nexuspad-ci-debug-key-v1:" + normalized).digest()
    d = (int.from_bytes(d_digest, "big") % (N - 1)) + 1
    qx, qy = ec_mul(d, (GX, GY))
    pub_bytes = b"\x04" + qx.to_bytes(32, "big") + qy.to_bytes(32, "big")

    oid_ec_pub = der_oid("1.2.840.10045.2.1")
    oid_prime256v1 = der_oid("1.2.840.10045.3.1.7")
    oid_ecdsa_sha256 = der_oid("1.2.840.10045.4.3.2")

    # RFC 5915 ECPrivateKey wrapped in PKCS#8 PrivateKeyInfo
    ec_private_key = der_seq(
        der_int(1),
        der_octet_string(d.to_bytes(32, "big")),
        der_tlv(0xA1, der_bit_string(pub_bytes)),
    )
    pkcs8_key = der_seq(
        der_int(0),
        der_seq(oid_ec_pub, oid_prime256v1),
        der_octet_string(ec_private_key),
    )

    # Deterministic positive 16-byte serial number
    serial_raw = bytearray(hashlib.sha256(b"nexuspad-ci-serial-v1:" + normalized).digest()[:16])
    serial_raw[0] = (serial_raw[0] & 0x7F) | 0x01
    serial_int = int.from_bytes(serial_raw, "big")

    # X.509 Name: CN=NexusPad Debug, O=NexusPad, C=US
    name = der_seq(
        der_rdn("2.5.4.6", "US", tag=0x13),
        der_rdn("2.5.4.10", "NexusPad"),
        der_rdn("2.5.4.3", "NexusPad Debug"),
    )

    # Fixed validity: 2025-01-01 to 2055-01-01 (RFC 5280: UTCTime < 2050, GeneralizedTime >= 2050)
    validity = der_seq(
        der_tlv(0x17, b"250101000000Z"),
        der_tlv(0x18, b"20550101000000Z"),
    )

    spki = der_seq(
        der_seq(oid_ec_pub, oid_prime256v1),
        der_bit_string(pub_bytes),
    )

    # Basic X.509 v3 extensions: BasicConstraints(CA=false) + KeyUsage(digitalSignature)
    ext_basic = der_seq(
        der_oid("2.5.29.19"),
        der_tlv(0x01, b"\xFF"),
        der_octet_string(der_seq()),
    )
    ext_key_usage = der_seq(
        der_oid("2.5.29.15"),
        der_tlv(0x01, b"\xFF"),
        der_octet_string(der_bit_string(b"\x80", unused_bits=7)),
    )
    extensions = der_tlv(0xA3, der_seq(ext_basic, ext_key_usage))

    sig_alg = der_seq(oid_ecdsa_sha256)
    tbs_cert = der_seq(
        der_tlv(0xA0, der_int(2)),  # X.509 v3
        der_int(serial_int),
        sig_alg,
        name,
        validity,
        name,
        spki,
        extensions,
    )

    sig_der = deterministic_ecdsa_sign(d, tbs_cert)
    cert_der = der_seq(tbs_cert, sig_alg, der_bit_string(sig_der))
    return pkcs8_key, cert_der


def to_pem(label: str, der: bytes) -> str:
    b64 = base64.encodebytes(der).decode("ascii")
    return f"-----BEGIN {label}-----\n{b64}-----END {label}-----\n"


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <seed> <output_keystore_path>", file=sys.stderr)
        sys.exit(2)

    seed = sys.argv[1]
    out_path = sys.argv[2]
    key_der, cert_der = build_key_and_cert(seed)

    with tempfile.TemporaryDirectory() as tmp:
        key_file = os.path.join(tmp, "key.pem")
        cert_file = os.path.join(tmp, "cert.pem")
        with open(key_file, "w", encoding="ascii") as f:
            f.write(to_pem("PRIVATE KEY", key_der))
        with open(cert_file, "w", encoding="ascii") as f:
            f.write(to_pem("CERTIFICATE", cert_der))

        subprocess.run(
            [
                "openssl",
                "pkcs12",
                "-export",
                "-inkey",
                key_file,
                "-in",
                cert_file,
                "-name",
                "androiddebugkey",
                "-passout",
                "pass:android",
                "-out",
                out_path,
            ],
            check=True,
        )

    sha256 = hashlib.sha256(cert_der).hexdigest().upper()
    formatted = ":".join(sha256[i : i + 2] for i in range(0, len(sha256), 2))
    print(f"Generated deterministic PKCS#12 keystore -> {out_path}")
    print(f"Certificate SHA-256: {formatted}")


if __name__ == "__main__":
    main()
