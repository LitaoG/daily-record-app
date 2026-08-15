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
  details: [],
};
const sexRecordPath = "users/user-a/sexRecords/2026-07-16";
const validSexRecord = {
  id: "sex-record-2026-07-16",
  localDate: "2026-07-16",
  sexCount: 1,
  createdAtMillis: 1784160000000,
  clientUpdatedAtMillis: 1784160000000,
  deleted: false,
  revision: 1,
  schemaVersion: 1,
  serverUpdatedAt: serverTimestamp(),
  details: [],
};

try {
  const userA = testEnv.authenticatedContext("user-a").firestore();
  const userB = testEnv.authenticatedContext("user-b").firestore();
  const anonymous = testEnv.unauthenticatedContext().firestore();
  const record = doc(userA, recordPath);

  await assertSucceeds(setDoc(record, validRecord));
  const { details: _ignoredDetails, ...recordWithoutDetails } = validRecord;
  await assertSucceeds(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-15"), {
      ...recordWithoutDetails,
      localDate: "2026-07-15",
    }),
  );
  await assertSucceeds(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-15"), {
      ...recordWithoutDetails,
      localDate: "2026-07-15",
      deleted: true,
      revision: 2,
      clientUpdatedAtMillis: 1784160001000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-14"), {
      ...validRecord,
      localDate: "2026-07-14",
      deleted: true,
      details: [
        {
          id: "detail-1",
          occurrenceIndex: 1,
          startTime: null,
          endTime: null,
          feeling: "",
        },
      ],
    }),
  );
  await assertSucceeds(getDoc(record));
  await assertFails(getDoc(doc(userB, recordPath)));
  await assertFails(getDoc(doc(anonymous, recordPath)));
  await assertFails(
    setDoc(doc(userB, "users/user-a/handBrewRecords/2026-07-17"), {
      ...validRecord,
      localDate: "2026-07-17",
    }),
  );
  await assertFails(deleteDoc(doc(userB, recordPath)));
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
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-07-21"), {
      ...validRecord,
      localDate: "2026-07-21",
      createdAtMillis: 253402300800000,
      clientUpdatedAtMillis: 253402300800000,
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/not-a-date"), {
      ...validRecord,
      localDate: "not-a-date",
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/handBrewRecords/2026-00-20"), {
      ...validRecord,
      localDate: "2026-00-20",
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
  await assertSucceeds(
    setDoc(record, {
      ...validRecord,
      brewCount: 4,
      revision: 3,
      clientUpdatedAtMillis: 1784160000500,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(record, {
      ...validRecord,
      id: "replacement-id",
      brewCount: 4,
      revision: 4,
      clientUpdatedAtMillis: 1784160002000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(record, {
      ...validRecord,
      createdAtMillis: 1784160000500,
      brewCount: 4,
      revision: 4,
      clientUpdatedAtMillis: 1784160002000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(deleteDoc(record));

  const sexRecord = doc(userA, sexRecordPath);
  await assertSucceeds(setDoc(sexRecord, validSexRecord));
  await assertSucceeds(getDoc(sexRecord));
  await assertFails(getDoc(doc(userB, sexRecordPath)));
  await assertFails(
    setDoc(doc(userB, "users/user-a/sexRecords/2026-07-17"), {
      ...validSexRecord,
      localDate: "2026-07-17",
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/sexRecords/2026-07-18"), {
      ...validSexRecord,
      localDate: "2026-07-18",
      sexCount: -1,
    }),
  );
  await assertFails(
    setDoc(doc(userA, "users/user-a/sexRecords/2026-07-19"), {
      ...validSexRecord,
      localDate: "2026-07-19",
      brewCount: 1,
    }),
  );
  await assertFails(
    setDoc(sexRecord, {
      ...validSexRecord,
      sexCount: 2,
      revision: 3,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    setDoc(sexRecord, {
      ...validSexRecord,
      sexCount: 2,
      revision: 2,
      clientUpdatedAtMillis: 1784160001000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    setDoc(sexRecord, {
      ...validSexRecord,
      id: "replacement-sex-id",
      sexCount: 3,
      revision: 3,
      clientUpdatedAtMillis: 1784160002000,
      serverUpdatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(deleteDoc(sexRecord));
  console.log("Firestore security rules: both module collections passed ownership, shape, revision, and deletion checks.");
} finally {
  await testEnv.cleanup();
}
