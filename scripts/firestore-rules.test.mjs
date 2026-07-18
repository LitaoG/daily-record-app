import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";

const projectId = "demo-daily-record-app";
const testEnv = await initializeTestEnvironment({
  projectId,
  firestore: {
    rules: readFileSync("firestore.rules", "utf8"),
  },
});

const recordPath = "users/user-a/handBrewRecords/2026-07-16";
const validRecord = {
  id: "record-2026-07-16",
  localDate: "2026-07-16",
  brewCount: 2,
  createdAtMillis: 1784160000000,
  clientUpdatedAtMillis: 1784160000000,
  deleted: false,
  revision: 1,
  schemaVersion: 1,
  serverUpdatedAt: serverTimestamp(),
};

try {
  const userA = testEnv.authenticatedContext("user-a").firestore();
  const userB = testEnv.authenticatedContext("user-b").firestore();
  const anonymous = testEnv.unauthenticatedContext().firestore();
  const record = doc(userA, recordPath);

  await assertSucceeds(setDoc(record, validRecord));
  await assertSucceeds(getDoc(record));
  await assertFails(getDoc(doc(userB, recordPath)));
  await assertFails(getDoc(doc(anonymous, recordPath)));
  await assertFails(
    setDoc(doc(userB, "users/user-a/handBrewRecords/2026-07-17"), {
      ...validRecord,
      localDate: "2026-07-17",
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-18"), {
      ...validRecord,
      localDate: "2026-07-18",
      brewCount: -1,
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-19"), {
      ...validRecord,
      localDate: "2026-07-19",
      clientUpdatedAtMillis: validRecord.createdAtMillis - 1,
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/not-a-date"), {
      ...validRecord,
      localDate: "not-a-date",
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-20"), {
      ...validRecord,
      localDate: "2026-07-20",
      unexpectedField: true,
    }),
  );
  await assertFails(
    setDoc(record, {
      ...validRecord,
      brewCount: 3,
      revision: 3,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    setDoc(record, {
      ...validRecord,
      brewCount: 3,
      revision: 2,
      clientUpdatedAtMillis: 1784160001000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(record, {
      ...validRecord,
      id: "replacement-id",
      brewCount: 4,
      revision: 3,
      clientUpdatedAtMillis: 1784160002000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(record, {
      ...validRecord,
      createdAtMillis: 1784160000500,
      brewCount: 4,
      revision: 3,
      clientUpdatedAtMillis: 1784160002000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(deleteDoc(record));
  console.log("Firestore security rules: all ownership, shape, revision, and tombstone checks passed.");
} finally {
  await testEnv.cleanup();
}
