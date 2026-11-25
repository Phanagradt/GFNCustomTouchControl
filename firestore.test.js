const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sampleControlMap() {
  return {
    id: "ctrl_wasd",
    label: "WASD",
    controlType: "WASD_CLUSTER",
    shape: "CROSS",
    xPercent: 0.16,
    yPercent: 0.68,
    sizeDp: 128.0,
    opacity: 0.85,
    accent: "CYAN",
    behavior: "HOLD",
    turboRateHz: 10,
    deadzone: 0.12,
    sensitivity: 1.0,
    hapticOnPress: true,
    invertY: false,
    binding: {
      id: "cluster_wasd",
      domain: "KEYBOARD",
      primaryKeyCode: 51,
      secondaryKeyCode: 0,
      modifierMetaState: 0,
      codeLabel: "WASD",
      commandTitle: "WASD Directional Cluster",
      axisPair: "WASD_KEYS",
    },
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write users, layouts, or shared_layouts", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("layouts").get());
  await assertFails(unauth.collection("shared_layouts").doc("NXP48K").get());
  await assertFails(
    unauth.collection("shared_layouts").doc("NXP48K").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      name: "Test",
      gameTitle: "DBD",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      controls: [sampleControlMap()],
      createdAt: new Date(Date.now() - 1000),
      expiresAt: new Date(Date.now() + 300000),
    })
  );
});

test("User account isolation: owner CRUD succeeds, cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Gamer",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: now }));

  // Ghost field & future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User layouts subcollection: owner create/update/list succeed, @DocumentId id injection & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("layouts").doc("layout_1").set({
      userId: ALICE_UID,
      name: "Dead by Daylight PC",
      gameTitle: "Dead by Daylight",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      active: true,
      controls: [sampleControlMap()],
      clientUpdatedAt: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("layouts").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("layouts").doc("layout_1").update({
      name: "DBD Survivor Updated",
      active: false,
      controls: [sampleControlMap()],
      clientUpdatedAt: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt tamper or @DocumentId 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("layouts").doc("layout_1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("layouts").doc("layout_2").set({
      id: "layout_2",
      userId: ALICE_UID,
      name: "Extra Id",
      gameTitle: "DBD",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      active: false,
      controls: [],
      clientUpdatedAt: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot list, get, or write Alice's layouts
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("layouts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("layouts").doc("layout_1").get());
});

test("Shared layouts: valid 5-minute code works for friends; expired code & >6min lifetime & cross-user list/delete fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const fiveMinLater = new Date(Date.now() + 5 * 60 * 1000 - 1000);

  // Alice exports active 5-minute share code
  await assertSucceeds(
    aliceDb.collection("shared_layouts").doc("NXP48K").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      name: "DBD Pro Layout",
      gameTitle: "Dead by Daylight",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      controls: [sampleControlMap()],
      createdAt: now,
      expiresAt: fiveMinLater,
    })
  );

  // Bob can fetch the active share code by exact ID
  await assertSucceeds(bobDb.collection("shared_layouts").doc("NXP48K").get());

  // Bob can query an unknown code cleanly (resource == null) without PERMISSION_DENIED
  await assertSucceeds(bobDb.collection("shared_layouts").doc("UNKNOWN9").get());

  // Bob cannot list all shared_layouts or delete Alice's code or update it
  await assertFails(bobDb.collection("shared_layouts").get());
  await assertFails(bobDb.collection("shared_layouts").doc("NXP48K").delete());
  await assertFails(bobDb.collection("shared_layouts").doc("NXP48K").update({ name: "Stolen" }));

  // Alice cannot create a code lasting 1 hour (> 6m ceiling)
  await assertFails(
    aliceDb.collection("shared_layouts").doc("LONG99").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      name: "Too Long",
      gameTitle: "DBD",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      controls: [sampleControlMap()],
      createdAt: now,
      expiresAt: new Date(Date.now() + 60 * 60 * 1000),
    })
  );

  // Seed an expired share code directly and confirm Bob is denied at rules layer
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("shared_layouts").doc("OLD555").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      name: "Expired Layout",
      gameTitle: "DBD",
      genre: "FPS",
      globalOpacity: 0.85,
      gridSnap: true,
      controls: [sampleControlMap()],
      createdAt: new Date(Date.now() - 400000),
      expiresAt: new Date(Date.now() - 60000),
    });
  });

  await assertFails(bobDb.collection("shared_layouts").doc("OLD555").get());
  // Owner can still clean up or list their own shares
  await assertSucceeds(aliceDb.collection("shared_layouts").where("ownerId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("shared_layouts").doc("OLD555").delete());
});

test("User settings subcollection: owner CRUD succeeds, unauth/cross-user/ghost-field/immutable tamper fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const validPayload = {
    userId: ALICE_UID,
    gameFilterJson: '{"activeSlot":1,"styles":[]}',
    crosshairJson: '{"enabled":true,"style":"CLASSIC_PLUS"}',
    colorSchemeJson: '{"preset":"GEFORCE_LIME"}',
    fontConfigJson: '{"preset":"NEXUS_DEFAULT"}',
    floatingBubbleJson: '{"style":"TACTICAL_RING"}',
    darkTheme: true,
    clientUpdatedAt: Date.now(),
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set(validPayload));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set(validPayload)
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").update({
      darkTheme: false,
      clientUpdatedAt: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").update({
      darkTheme: true,
      updatedAt: now,
    })
  );

  // Ghost field & immutable createdAt tamper rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("extra_doc").set({
      ...validPayload,
      ghostFlag: true,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").delete()
  );
});

