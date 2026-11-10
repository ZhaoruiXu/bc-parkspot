const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("unauthenticated user cannot read or write saved lots", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).get());
  await assertFails(
    unauthedDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).set({
      lotId: "garibaldi_rubble_creek",
      parkName: "Garibaldi Provincial Park",
      lotName: "Rubble Creek Main Trailhead",
      region: "Sea-to-Sky",
      savedAt: new Date(),
    })
  );
});

test("alice can create and read her own saved lot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).set({
      lotId: "garibaldi_rubble_creek",
      parkName: "Garibaldi Provincial Park",
      lotName: "Rubble Creek Main Trailhead",
      region: "Sea-to-Sky",
      savedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).get());
});

test("bob cannot read or modify alice's saved lot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).set({
    lotId: "garibaldi_rubble_creek",
    parkName: "Garibaldi Provincial Park",
    lotName: "Rubble Creek Main Trailhead",
    region: "Sea-to-Sky",
    savedAt: new Date(),
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/savedLots/garibaldi_rubble_creek`).delete());
});

test("authenticated users can read and post community reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc("communityReports/report_1").set({
      userId: ALICE_UID,
      reporterName: "Alice Hiker",
      lotId: "joffre_lakes_main",
      parkName: "Joffre Lakes Provincial Park",
      status: "FILLING_FAST",
      comment: "Only 5 spots left in upper gravel lot",
      spotsEstimate: 5,
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.doc("communityReports/report_1").get());
});

test("user cannot post report with someone else's userId", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.doc("communityReports/fake_report").set({
      userId: ALICE_UID, // Spoofed userId
      reporterName: "Fake Alice",
      lotId: "joffre_lakes_main",
      parkName: "Joffre Lakes",
      status: "FULL",
      comment: "Fake report",
      spotsEstimate: 0,
      createdAt: new Date(),
    })
  );
});

test("comment must have at least 3 characters after trimming in firestore rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Fails on empty or spaces
  await assertFails(
    aliceDb.doc("communityReports/report_empty").set({
      userId: ALICE_UID,
      reporterName: "Alice Hiker",
      lotId: "joffre_lakes_main",
      parkName: "Joffre Lakes Provincial Park",
      status: "AVAILABLE",
      comment: "    ",
      createdAt: new Date(),
    })
  );

  // Fails on 2 characters after trim
  await assertFails(
    aliceDb.doc("communityReports/report_short").set({
      userId: ALICE_UID,
      reporterName: "Alice Hiker",
      lotId: "joffre_lakes_main",
      parkName: "Joffre Lakes Provincial Park",
      status: "AVAILABLE",
      comment: " ok ",
      createdAt: new Date(),
    })
  );

  // Succeeds on 3 characters after trim
  await assertSucceeds(
    aliceDb.doc("communityReports/report_valid").set({
      userId: ALICE_UID,
      reporterName: "Alice Hiker",
      lotId: "joffre_lakes_main",
      parkName: "Joffre Lakes Provincial Park",
      status: "AVAILABLE",
      comment: " Lot open ",
      createdAt: new Date(),
    })
  );
});
