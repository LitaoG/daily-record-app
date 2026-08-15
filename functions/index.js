const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { randomUUID } = require("node:crypto");

admin.initializeApp();

const db = getFirestore();
const MAX_SUPPORTED_EPOCH_MILLIS = 253402300799999;
const MAX_DETAIL_ID_LENGTH = 160;
const MAX_FEELING_CODE_POINTS = 100;
const MAX_DETAIL_COUNT = 1000;
const DELETE_BATCH_SIZE = 400;
const MAX_AUTH_AGE_SECONDS = 5 * 60;

const MODULES = Object.freeze({
  handBrewRecords: Object.freeze({ countField: "brewCount" }),
  sexRecords: Object.freeze({ countField: "sexCount" }),
});

const DETAIL_KEYS = Object.freeze([
  "id",
  "occurrenceIndex",
  "startTime",
  "endTime",
  "feeling",
]);

function httpsError(code, message) {
  return new functions.https.HttpsError(code, message);
}

function requireAuthenticated(context) {
  if (!context.auth || !context.auth.uid) {
    throw httpsError("unauthenticated", "Authentication is required.");
  }
  return context.auth;
}

function requireRecentlyAuthenticated(auth) {
  const authTime = Number(auth.token && auth.token.auth_time);
  const now = Math.floor(Date.now() / 1000);
  if (!Number.isSafeInteger(authTime) || now - authTime > MAX_AUTH_AGE_SECONDS) {
    throw httpsError(
      "failed-precondition",
      "A recent password confirmation is required for this operation.",
    );
  }
}

function isSafeInteger(value) {
  return Number.isSafeInteger(value);
}

function requireString(value, field, { minLength = 1, maxLength = 160 } = {}) {
  if (typeof value !== "string" || value.length < minLength || value.length > maxLength) {
    throw new Error(`${field} is invalid`);
  }
  return value;
}

function requireEpochMillis(value, field) {
  if (!isSafeInteger(value) || value < 0 || value > MAX_SUPPORTED_EPOCH_MILLIS) {
    throw new Error(`${field} is invalid`);
  }
  return value;
}

function isValidDateText(value) {
  if (typeof value !== "string" || !/^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/.test(value)) {
    return false;
  }
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day;
}

function isValidMinuteTime(value) {
  if (value === null) return true;
  if (typeof value !== "string") return false;
  const match = /^(?:[01]\d|2[0-3]):[0-5]\d(?::00)?$/.exec(value);
  return match !== null;
}

function minuteValue(value) {
  if (value === null) return null;
  const [hour, minute] = value.slice(0, 5).split(":").map(Number);
  return hour * 60 + minute;
}

function validDetail(detail, count, seenIds, seenOccurrences) {
  if (!detail || typeof detail !== "object" || Array.isArray(detail)) return false;
  const keys = Object.keys(detail).sort();
  if (keys.length !== DETAIL_KEYS.length ||
      !keys.every((key, index) => key === [...DETAIL_KEYS].sort()[index])) {
    return false;
  }
  if (typeof detail.id !== "string" ||
      detail.id.length < 1 ||
      detail.id.length > MAX_DETAIL_ID_LENGTH ||
      seenIds.has(detail.id)) {
    return false;
  }
  if (!isSafeInteger(detail.occurrenceIndex) ||
      detail.occurrenceIndex < 1 ||
      detail.occurrenceIndex > count ||
      seenOccurrences.has(detail.occurrenceIndex)) {
    return false;
  }
  if (!isValidMinuteTime(detail.startTime) || !isValidMinuteTime(detail.endTime)) {
    return false;
  }
  if (detail.startTime !== null && detail.endTime !== null &&
      minuteValue(detail.endTime) < minuteValue(detail.startTime)) {
    return false;
  }
  if (typeof detail.feeling !== "string" ||
      [...detail.feeling].length > MAX_FEELING_CODE_POINTS) {
    return false;
  }
  seenIds.add(detail.id);
  seenOccurrences.add(detail.occurrenceIndex);
  return true;
}

function validateDetails(details, record, countField, { required = false } = {}) {
  if (details === undefined) {
    if (required) throw new Error("details is required");
    return;
  }
  if (!Array.isArray(details) ||
      details.length > MAX_DETAIL_COUNT ||
      details.length > record[countField]) {
    throw new Error("details list is invalid");
  }
  if (record.deleted && details.length !== 0) {
    throw new Error("deleted records cannot carry details");
  }
  const seenIds = new Set();
  const seenOccurrences = new Set();
  if (!details.every((detail) => validDetail(
    detail,
    record[countField],
    seenIds,
    seenOccurrences,
  ))) {
    throw new Error("details contains an invalid item");
  }
}

function validateRecordInput(data) {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    throw new Error("record input is invalid");
  }
  const module = MODULES[data.collection];
  if (!module) throw new Error("collection is invalid");
  requireString(data.localDate, "localDate", { maxLength: 10 });
  if (!isValidDateText(data.localDate)) throw new Error("localDate is invalid");
  requireString(data.id, "id");
  if (!isSafeInteger(data.count) || data.count < 0 || data.count > 2147483647) {
    throw new Error("count is invalid");
  }
  const createdAtMillis = requireEpochMillis(data.createdAtMillis, "createdAtMillis");
  const clientUpdatedAtMillis = requireEpochMillis(
    data.clientUpdatedAtMillis,
    "clientUpdatedAtMillis",
  );
  if (clientUpdatedAtMillis < createdAtMillis) {
    throw new Error("clientUpdatedAtMillis is invalid");
  }
  if (typeof data.deleted !== "boolean") throw new Error("deleted is invalid");
  if (!isSafeInteger(data.remoteRevision) || data.remoteRevision < 0) {
    throw new Error("remoteRevision is invalid");
  }
  const record = {
    id: data.id,
    localDate: data.localDate,
    [module.countField]: data.count,
    createdAtMillis,
    clientUpdatedAtMillis,
    deleted: data.deleted,
    details: data.details,
  };
  validateDetails(data.details, record, module.countField, { required: true });
  return { module, record, remoteRevision: data.remoteRevision };
}

function validateStoredRecord(data, module) {
  if (!data || typeof data !== "object") throw new Error("stored record is invalid");
  requireString(data.id, "id");
  if (!isValidDateText(data.localDate)) throw new Error("stored localDate is invalid");
  if (!isSafeInteger(data[module.countField]) ||
      data[module.countField] < 0 ||
      data[module.countField] > 2147483647) {
    throw new Error("stored count is invalid");
  }
  requireEpochMillis(data.createdAtMillis, "stored createdAtMillis");
  requireEpochMillis(data.clientUpdatedAtMillis, "stored clientUpdatedAtMillis");
  if (data.clientUpdatedAtMillis < data.createdAtMillis ||
      typeof data.deleted !== "boolean" ||
      !isSafeInteger(data.revision) || data.revision < 1) {
    throw new Error("stored record metadata is invalid");
  }
  validateDetails(data.details, data, module.countField);
}

function callableRecord(data, module) {
  return {
    id: data.id,
    localDate: data.localDate,
    [module.countField]: data[module.countField],
    createdAtMillis: data.createdAtMillis,
    clientUpdatedAtMillis: data.clientUpdatedAtMillis,
    deleted: data.deleted,
    revision: data.revision,
    schemaVersion: 1,
    details: data.details || [],
  };
}

exports.writeDailyCountRecord = functions.https.onCall(async (data, context) => {
  const auth = requireAuthenticated(context);
  let validated;
  try {
    validated = validateRecordInput(data);
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
    throw httpsError("invalid-argument", "The record input is invalid.");
  }
  const { module, record, remoteRevision } = validated;
  const reference = db
    .collection("users")
    .doc(auth.uid)
    .collection(data.collection)
    .doc(record.localDate);

  const result = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    const current = snapshot.exists ? snapshot.data() : null;
    if (current) {
      try {
        validateStoredRecord(current, module);
      } catch (error) {
        throw httpsError("failed-precondition", "The cloud record is malformed.");
      }
      if (remoteRevision !== current.revision || record.id !== current.id) {
        return { written: false, record: callableRecord(current, module) };
      }
    }

    const revision = (current ? current.revision : 0) + 1;
    const stableId = current ? current.id :
      (remoteRevision > 0 ? randomUUID() : record.id);
    const stableCreatedAt = current ? current.createdAtMillis : record.createdAtMillis;
    const previousUpdatedAt = current ? current.clientUpdatedAtMillis : stableCreatedAt;
    const committedUpdatedAt = Math.max(
      record.clientUpdatedAtMillis,
      stableCreatedAt,
      previousUpdatedAt + 1,
    );
    if (committedUpdatedAt > MAX_SUPPORTED_EPOCH_MILLIS) {
      throw httpsError("failed-precondition", "The record timestamp is out of range.");
    }
    const stored = {
      id: stableId,
      localDate: record.localDate,
      [module.countField]: record[module.countField],
      createdAtMillis: stableCreatedAt,
      clientUpdatedAtMillis: committedUpdatedAt,
      deleted: record.deleted,
      revision,
      schemaVersion: 1,
      details: record.details,
      serverUpdatedAt: FieldValue.serverTimestamp(),
    };
    transaction.set(reference, stored);
    return { written: true, record: callableRecord(stored, module) };
  });

  return result;
});

exports.deleteAccountData = functions.https.onCall(async (data, context) => {
  const auth = requireAuthenticated(context);
  requireRecentlyAuthenticated(auth);
  if (!data || data.ownerId !== auth.uid) {
    throw httpsError("permission-denied", "The owner id must match the signed-in account.");
  }

  let deleted = 0;
  for (const collectionName of Object.keys(MODULES)) {
    const collection = db.collection("users").doc(auth.uid).collection(collectionName);
    while (true) {
      const snapshot = await collection.limit(DELETE_BATCH_SIZE).get();
      if (snapshot.empty) break;
      const batch = db.batch();
      snapshot.docs.forEach((document) => batch.delete(document.ref));
      await batch.commit();
      deleted += snapshot.size;
    }
  }
  return { ownerId: auth.uid, deleted };
});

async function deleteMalformedDocument(after, reason) {
  const current = await after.ref.get();
  if (!current.exists) return;
  const sameVersion = !after.updateTime || !current.updateTime ||
    current.updateTime.toMillis() === after.updateTime.toMillis();
  if (!sameVersion) return;
  console.warn("Deleting malformed Daily Record document", {
    path: after.ref.path,
    reason,
  });
  await after.ref.delete();
}

async function validateWrittenRecord(change, collectionName) {
  if (!change.after.exists) return;
  const after = change.after;
  const data = after.data();
  const module = MODULES[collectionName];
  try {
    validateStoredRecord(data, module);
  } catch (error) {
    await deleteMalformedDocument(after, error.message);
  }
}

exports.validateHandBrewRecord = functions.firestore
  .document("users/{userId}/handBrewRecords/{date}")
  .onWrite((change) => validateWrittenRecord(change, "handBrewRecords"));

exports.validateSexRecord = functions.firestore
  .document("users/{userId}/sexRecords/{date}")
  .onWrite((change) => validateWrittenRecord(change, "sexRecords"));
