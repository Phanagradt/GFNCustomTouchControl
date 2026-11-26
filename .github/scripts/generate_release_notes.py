#!/usr/bin/env python3
"""
Generates per-version GitHub Release notes for XenosPad.

Rules:
- Only includes RELEASE_NOTES.md if it was actually modified in THIS release (PREV_TAG..HEAD).
  Stale release notes from older versions are never repeated.
- Automatically categorizes every commit since the previous release tag into
  New Features & Enhancements, Bug Fixes, and Improvements & Minor Changes so even
  small bug-fix or polish updates get accurate, concise version-specific notes.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path


CONVENTIONAL_RE = re.compile(
    r"^(?P<type>feat|fix|bug|hotfix|perf|refactor|style|docs|chore|ci|build|test|revert)"
    r"(?:\((?P<scope>[^)]+)\))?(?P<breaking>!)?:\s*(?P<subject>.+)$",
    re.IGNORECASE,
)

BUG_KEYWORDS = (
    "fix",
    "fixed",
    "fixes",
    "bug",
    "crash",
    "error",
    "issue",
    "resolve",
    "resolved",
    "repair",
    "patch",
    "broken",
    "duplicate",
    "prevent",
    "fail",
    "failed",
    "stuck",
    "leak",
    "doesn't work",
    "not working",
    "can't",
    "cannot",
)

FEATURE_KEYWORDS = (
    "add ",
    "added ",
    "adding ",
    "new ",
    "implement",
    "upgrade",
    "full-featured",
    "full ",
    "support ",
    "introduce",
    "enable ",
    "built-in",
    "build-in",
    "customizable",
    "studio",
)


def run_git(*args: str) -> str:
    proc = subprocess.run(
        ["git", *args],
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        return ""
    return proc.stdout.strip()


def find_previous_tag(current_tag: str) -> str:
    head_sha = run_git("rev-parse", "HEAD")
    raw_tags = run_git("tag", "--list", "v*", "--merged", "HEAD", "--sort=-v:refname")
    tags = [t.strip() for t in raw_tags.splitlines() if t.strip()]

    fallback = ""
    for tag in tags:
        if tag == current_tag:
            continue
        tag_sha = run_git("rev-list", "-n", "1", tag)
        if tag_sha and tag_sha != head_sha:
            return tag
        if not fallback:
            fallback = tag

    # Fallback to git describe on HEAD^
    described = run_git("describe", "--tags", "--abbrev=0", "HEAD^")
    if described and described != current_tag:
        return described
    return fallback


def was_file_changed_in_range(prev_tag: str, filename: str) -> bool:
    if prev_tag:
        out = run_git("diff", "--name-only", f"{prev_tag}..HEAD", "--", filename)
        return filename in {line.strip() for line in out.splitlines() if line.strip()}
    out = run_git("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD", "--", filename)
    return filename in {line.strip() for line in out.splitlines() if line.strip()}


def get_commits_in_range(prev_tag: str) -> list[str]:
    if prev_tag:
        raw = run_git("log", f"{prev_tag}..HEAD", "--no-merges", "--pretty=format:%s")
    else:
        raw = run_git("log", "-n", "5", "--no-merges", "--pretty=format:%s")
    commits = [line.strip() for line in raw.splitlines() if line.strip()]
    if not commits:
        latest = run_git("log", "-n", "1", "--no-merges", "--pretty=format:%s")
        if latest:
            commits = [latest]
    return commits


def clean_subject(raw_subject: str) -> tuple[str | None, str]:
    """Returns (conventional_type_or_none, cleaned_display_subject)."""
    match = CONVENTIONAL_RE.match(raw_subject.strip())
    if match:
        ctype = match.group("type").lower()
        scope = (match.group("scope") or "").strip()
        subject = match.group("subject").strip()
        if subject:
            subject = subject[0].upper() + subject[1:]
        if scope:
            return ctype, f"**{scope}**: {subject}"
        return ctype, subject

    subject = raw_subject.strip()
    if subject:
        subject = subject[0].upper() + subject[1:]
    return None, subject


def classify_commit(raw_subject: str) -> tuple[str, str]:
    """Classifies a commit into ('fix' | 'feat' | 'improvement', formatted_text)."""
    ctype, text = clean_subject(raw_subject)
    lower = raw_subject.lower()

    if ctype in ("fix", "bug", "hotfix"):
        return "fix", text
    if ctype == "feat":
        return "feat", text
    if ctype in ("perf", "refactor", "style", "docs", "chore", "ci", "build", "test", "revert"):
        if any(k in lower for k in ("fix", "bug", "crash", "error", "broken")):
            return "fix", text
        return "improvement", text

    # Non-conventional commit messages: inspect wording
    if any(k in lower for k in BUG_KEYWORDS):
        return "fix", text
    if any(k in lower for k in FEATURE_KEYWORDS):
        return "feat", text
    return "improvement", text


def dedupe(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        key = item.lower()
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result


GENERIC_COMMIT_RE = re.compile(
    r"^(update|sync|save|wip|changes?|minor\s+update|update\s+from\s+.*ai\s+studio|push\s+from\s+.*)$",
    re.IGNORECASE,
)


def summarize_changed_areas(prev_tag: str) -> list[str]:
    if prev_tag:
        raw = run_git("diff", "--name-only", f"{prev_tag}..HEAD")
    else:
        raw = run_git("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD")
    files = [f.strip() for f in raw.splitlines() if f.strip()]
    if not files:
        return []

    area_rules = [
        ("webplayer/", "GeForce NOW Web Player & mouse/stream controls"),
        ("service/OverlayControllerService", "Floating controller overlay & Live Edit"),
        ("LayoutStudioSheets", "Layout Studio & Live Edit customization"),
        ("LayoutEditorScreen", "Layout Studio editor"),
        ("VirtualControlWidgets", "Virtual touch controls & trackpad"),
        ("InputBridge", "Virtual gamepad/keyboard/mouse input routing"),
        ("AppLocalization", "App text & multi-language translations"),
        ("GameFilter", "Freestyle visual filters"),
        ("Crosshair", "Custom crosshair overlay"),
        ("GitHubUpdate", "In-app updater"),
        ("CloudLayoutSync", "Cloud Save & layout sharing"),
        (".github/workflows", "Release build & publishing workflow"),
    ]
    matched: list[str] = []
    for needle, label in area_rules:
        if any(needle in path for path in files):
            matched.append(label)
    return dedupe(matched)


def build_release_body() -> str:
    tag = os.environ.get("TAG", "").strip()
    version_name = os.environ.get("VERSION_NAME", tag.lstrip("v")).strip()
    version_code = os.environ.get("VERSION_CODE", "").strip()
    apk_name = os.environ.get("APK_NAME", f"XenosPad-{tag}.apk").strip()
    cert_sha256 = os.environ.get("CERT_SHA256", "").strip()

    prev_tag = find_previous_tag(tag)
    notes_path = Path("RELEASE_NOTES.md")
    has_fresh_manual_notes = (
        notes_path.is_file()
        and notes_path.stat().st_size > 0
        and was_file_changed_in_range(prev_tag, "RELEASE_NOTES.md")
    )

    commits = get_commits_in_range(prev_tag)
    features: list[str] = []
    fixes: list[str] = []
    improvements: list[str] = []

    for msg in commits:
        category, formatted = classify_commit(msg)
        if category == "feat":
            features.append(formatted)
        elif category == "fix":
            fixes.append(formatted)
        else:
            improvements.append(formatted)

    features = dedupe(features)
    fixes = dedupe(fixes)
    improvements = dedupe(improvements)

    lines: list[str] = []
    lines.append(f"## XenosPad v{version_name}")
    lines.append("")
    lines.append(
        f"Download `{apk_name}` below (or update directly inside **XenosPad → Setup → App Updates** without uninstalling)."
    )
    lines.append("")
    meta_parts = [f"`v{version_name}`"]
    if version_code:
        meta_parts.append(f"`versionCode: {version_code}`")
    meta_parts.append("Package: `com.aistudio.nexuspad.vktrmz`")
    lines.append("**Build Metadata:** " + " • ".join(meta_parts))
    if cert_sha256:
        lines.append(f"**Signing SHA-256:** `{cert_sha256}`")
    lines.append("")

    if prev_tag:
        lines.append(f"## What's New in v{version_name} (since `{prev_tag}`)")
    else:
        lines.append(f"## What's New in v{version_name}")
    lines.append("")

    if has_fresh_manual_notes:
        manual_text = notes_path.read_text(encoding="utf-8").strip()
        lines.append(manual_text)
        lines.append("")
        if commits:
            lines.append("### Commits in This Version")
            for msg in commits:
                _, formatted = classify_commit(msg)
                lines.append(f"- {formatted}")
            lines.append("")
    else:
        all_generic = bool(commits) and all(GENERIC_COMMIT_RE.match(c.strip()) for c in commits)
        if all_generic:
            areas = summarize_changed_areas(prev_tag)
            if areas:
                lines.append("### Updates & Fixes in This Version")
                for area in areas:
                    lines.append(f"- Updated {area}")
                lines.append("")
            else:
                for item in improvements:
                    lines.append(f"- {item}")
                lines.append("")
        else:
            if features:
                lines.append("### New Features & Enhancements")
                for item in features:
                    lines.append(f"- {item}")
                lines.append("")
            if fixes:
                lines.append("### Bug Fixes")
                for item in fixes:
                    lines.append(f"- {item}")
                lines.append("")
            if improvements:
                lines.append("### Improvements & Minor Changes")
                for item in improvements:
                    lines.append(f"- {item}")
                lines.append("")
            if not features and not fixes and not improvements:
                lines.append("- Minor maintenance update and stability improvements.")
                lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    output_file = Path(os.environ.get("OUTPUT_FILE", "release_body.md"))
    body = build_release_body()
    output_file.write_text(body, encoding="utf-8")
    print(f"Generated version-specific release notes -> {output_file}")
    print("--- Preview ---")
    print(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
