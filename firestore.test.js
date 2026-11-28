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

test("Public profiles: signed-in get allowed, list blocked, owner-only write & strict field validation", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("public_profiles").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "LoopMaster_DBD",
      bio: "Survivor main & custom button artist",
      avatarBase64: "iVBORw0KGgo=",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can view Alice's public profile by UID, but cannot list all profiles or overwrite hers
  await assertSucceeds(bobDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("public_profiles").get());
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      username: "Hijacked",
      updatedAt: now,
    })
  );

  // Owner update succeeds; ghost field fails
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      username: "EntityRunner",
      bio: "New bio",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      email: "leak@example.com",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Button Theme Workshop: publish, query published, like/favorite/download/comment, and block spoofed counters", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const validTheme = {
    ownerId: ALICE_UID,
    authorName: "Alice",
    name: "DBD Bloodweb Style",
    gameTag: "Dead by Daylight",
    description: "Crimson skill-check icons & dark fog stroke",
    category: "Horror",
    themeJson: '{"defaultStyle":{"showStroke":true},"buttonStyles":{}}',
    published: true,
    likesCount: 0,
    favoritesCount: 0,
    downloadsCount: 0,
    commentsCount: 0,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(unauthDb.collection("button_themes").doc("theme_dbd").set(validTheme));
  await assertSucceeds(aliceDb.collection("button_themes").doc("theme_dbd").set(validTheme));

  // Bob can browse published themes with limit <= 50, but cannot query unbounded (>50)
  await assertSucceeds(bobDb.collection("button_themes").where("published", "==", true).limit(25).get());
  await assertFails(bobDb.collection("button_themes").where("published", "==", true).limit(100).get());

  // Bob can like (+1), favorite (+1), download (+1), and comment (+1)
  await assertSucceeds(
    bobDb.collection("button_themes").doc("theme_dbd").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("button_themes").doc("theme_dbd").update({ likesCount: 1 }));
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("favorite_themes").doc("theme_dbd").set({
      userId: BOB_UID,
      themeId: "theme_dbd",
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("button_themes").doc("theme_dbd").update({ favoritesCount: 1 }));
  await assertSucceeds(bobDb.collection("button_themes").doc("theme_dbd").update({ downloadsCount: 1 }));
  await assertSucceeds(
    bobDb.collection("button_themes").doc("theme_dbd").collection("comments").doc("c1").set({
      authorUid: BOB_UID,
      authorName: "Bob",
      text: "Love these DBD icons!",
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("button_themes").doc("theme_dbd").update({ commentsCount: 1 }));

  // Bob cannot jump likesCount by +10, decrement downloadsCount, edit theme name, or delete Alice's theme
  await assertFails(bobDb.collection("button_themes").doc("theme_dbd").update({ likesCount: 11 }));
  await assertFails(bobDb.collection("button_themes").doc("theme_dbd").update({ downloadsCount: 0 }));
  await assertFails(bobDb.collection("button_themes").doc("theme_dbd").update({ name: "Stolen", updatedAt: now }));
  await assertFails(bobDb.collection("button_themes").doc("theme_dbd").delete());

  // Owner (Alice) can delete Bob's comment on her theme, and delete her own theme
  await assertSucceeds(aliceDb.collection("button_themes").doc("theme_dbd").collection("comments").doc("c1").delete());
  await assertSucceeds(aliceDb.collection("button_themes").doc("theme_dbd").delete());
});

test("Profile clips & community topic posts: owner create/edit/delete, cross-user like & comment, block spoofed writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("profile_clips").doc("clip_1").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      title: "360 Flashlight Save!",
      gameTag: "Dead by Daylight",
      caption: "Recorded using XenosPad Web Play",
      durationMs: 14500,
      thumbnailBase64: "AAAA",
      storyboardFrames: ["AAAA", "BBBB"],
      likesCount: 0,
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("profile_clips").where("ownerId", "==", ALICE_UID).limit(20).get());
  await assertSucceeds(bobDb.collection("profile_clips").doc("clip_1").update({ likesCount: 1 }));
  await assertFails(bobDb.collection("profile_clips").doc("clip_1").update({ likesCount: 50 }));
  await assertFails(bobDb.collection("profile_clips").doc("clip_1").delete());

  await assertSucceeds(
    aliceDb.collection("community_posts").doc("post_1").set({
      ownerId: ALICE_UID,
      authorName: "Alice",
      topic: "Dead by Daylight",
      title: "Best touch layout for killer?",
      body: "Share your favorite drag-to-aim sensitivity for Huntress!",
      likesCount: 0,
      commentsCount: 0,
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("community_posts").limit(25).get());
  await assertSucceeds(bobDb.collection("community_posts").doc("post_1").update({ likesCount: 1 }));
  await assertSucceeds(
    bobDb.collection("community_posts").doc("post_1").collection("comments").doc("r1").set({
      authorUid: BOB_UID,
      authorName: "Bob",
      text: "Try 1.8x drag aim on M2!",
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("community_posts").doc("post_1").update({ commentsCount: 1 }));
  await assertFails(bobDb.collection("community_posts").doc("post_1").update({ title: "Edited by Bob" }));
  await assertSucceeds(aliceDb.collection("community_posts").doc("post_1").delete());
});

test("Admin Management System: primary email gate, custom cosmetics, free shop vs exclusive grants, user search, and role badges", async () => {
  const ADMIN_UID = "primary_admin_uid";
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "november112561@gmail.com",
    email_verified: true,
  }).firestore();
  const unverifiedAdminDb = testEnv.authenticatedContext("fake_admin_uid", {
    email: "november112561@gmail.com",
    email_verified: false,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice creates her public profile (cannot self-assign adminRole or verifiedRole on create)
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AlicePro",
      usernameLower: "alicepro",
      bio: "Trying to hack admin role",
      adminRole: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AlicePro",
      usernameLower: "alicepro",
      bio: "DBD survivor",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-admins and unverified email cannot create cosmetic items
  const freeFrame = {
    name: "Neon Lime Ring",
    description: "Glowing tactical lime frame for everyone",
    type: "FRAME",
    availability: "FREE_SHOP",
    primaryColorHex: 0xff8ce000,
    secondaryColorHex: 0xff00e5ff,
    textColorHex: 0xffffffff,
    borderWidthDp: 3.0,
    tagText: "",
    iconPreset: "NONE",
    createdByUid: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
  };
  const exclusiveTag = {
    name: "Founder Gold Tag",
    description: "Admin-granted exclusive founder tag",
    type: "NAME_TAG",
    availability: "EXCLUSIVE",
    primaryColorHex: 0xffd4af37,
    secondaryColorHex: 0xff3b2900,
    textColorHex: 0xffffffff,
    borderWidthDp: 1.5,
    tagText: "FOUNDER",
    iconPreset: "CROWN",
    createdByUid: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(
    aliceDb.collection("cosmetic_items").doc("frame_lime").set({
      ...freeFrame,
      createdByUid: ALICE_UID,
    })
  );
  await assertFails(
    unverifiedAdminDb.collection("cosmetic_items").doc("frame_lime").set({
      ...freeFrame,
      createdByUid: "fake_admin_uid",
    })
  );

  // Primary admin creates both a FREE_SHOP frame and an EXCLUSIVE name tag
  await assertSucceeds(adminDb.collection("cosmetic_items").doc("frame_lime").set(freeFrame));
  await assertSucceeds(adminDb.collection("cosmetic_items").doc("tag_founder").set(exclusiveTag));

  // Signed-in users can list cosmetic items (limit <= 100)
  await assertSucceeds(aliceDb.collection("cosmetic_items").limit(50).get());

  // Only primary admin can search/list public profiles by username
  await assertFails(aliceDb.collection("public_profiles").limit(20).get());
  await assertFails(unverifiedAdminDb.collection("public_profiles").limit(20).get());
  await assertSucceeds(adminDb.collection("public_profiles").limit(50).get());

  // Alice CAN claim a FREE_SHOP item, and then equip it
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).collection("owned_items").doc("frame_lime").set({
      itemId: "frame_lime",
      grantedByAdmin: false,
      grantedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      equippedFrameId: "frame_lime",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice CANNOT claim an EXCLUSIVE item from the shop, and CANNOT equip an unowned exclusive item
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).collection("owned_items").doc("tag_founder").set({
      itemId: "tag_founder",
      grantedByAdmin: false,
      grantedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      equippedNameTagId: "tag_founder",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Alice CANNOT grant herself adminRole or verifiedRole
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      adminRole: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      verifiedRole: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Primary admin CAN grant the EXCLUSIVE tag to Alice, and Alice can then equip it!
  await assertSucceeds(
    adminDb.collection("public_profiles").doc(ALICE_UID).collection("owned_items").doc("tag_founder").set({
      itemId: "tag_founder",
      grantedByAdmin: true,
      grantedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      equippedNameTagId: "tag_founder",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Primary admin CAN assign Admin & Verified roles to Alice; Bob cannot!
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      verifiedRole: true,
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertSucceeds(
    adminDb.collection("public_profiles").doc(ALICE_UID).update({
      adminRole: true,
      verifiedRole: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Primary admin can delete cosmetic items
  await assertFails(aliceDb.collection("cosmetic_items").doc("frame_lime").delete());
  await assertSucceeds(adminDb.collection("cosmetic_items").doc("frame_lime").delete());
});



