const KNOWN_COLLECTIONS = Object.freeze([
  "handBrewRecords",
  "sexRecords",
]);

const KNOWN_VALIDATION_MESSAGES = Object.freeze([
  "record input is invalid",
  "collection is invalid",
  "localDate is invalid",
  "id is invalid",
  "count is invalid",
  "createdAtMillis is invalid",
  "clientUpdatedAtMillis is invalid",
  "deleted is invalid",
  "remoteRevision is invalid",
  "stored record is invalid",
  "stored localDate is invalid",
  "stored count is invalid",
  "stored createdAtMillis is invalid",
  "stored clientUpdatedAtMillis is invalid",
  "stored record metadata is invalid",
  "details is required",
  "details list is invalid",
  "deleted records cannot carry details",
  "details contains an invalid item",
]);

function deleteLogEntry(collectionName, reason) {
  const message = reason instanceof Error ? reason.message : String(reason);
  return {
    collection: KNOWN_COLLECTIONS.includes(collectionName)
      ? collectionName
      : "unknown",
    errorCode: KNOWN_VALIDATION_MESSAGES.includes(message)
      ? message
      : "unclassified-validation-failure",
  };
}

module.exports = { deleteLogEntry };
