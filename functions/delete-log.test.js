const test = require("node:test");
const assert = require("node:assert/strict");
const { deleteLogEntry } = require("./delete-log");

test("delete log entries only carry a known collection and a stable error code", () => {
  const entry = deleteLogEntry("handBrewRecords", new Error("stored localDate is invalid"));
  assert.deepEqual(Object.keys(entry).sort(), ["collection", "errorCode"]);
  assert.equal(entry.collection, "handBrewRecords");
  assert.equal(entry.errorCode, "stored localDate is invalid");
});

test("delete log entries never contain a path, uid, date, or document content", () => {
  const serialized = JSON.stringify(
    deleteLogEntry("sexRecords", new Error("stored count is invalid")),
  );
  assert.ok(!serialized.includes("users/"), "must not include a Firestore path");
  assert.ok(!serialized.includes("/"), "must not include a slash");
  assert.ok(!/\d{4}-\d{2}-\d{2}/.test(serialized), "must not include a local date");
  assert.ok(!serialized.includes("user-a"), "must not include an owner id");
});

test("unclassified reasons collapse to a stable code instead of passing input through", () => {
  const entry = deleteLogEntry(
    "handBrewRecords",
    new Error("users/user-a/sexRecords/2026-07-16 brewCount 99"),
  );
  assert.equal(entry.errorCode, "unclassified-validation-failure");
  const serialized = JSON.stringify(entry);
  assert.ok(!serialized.includes("users/"), "must not include a Firestore path");
  assert.ok(!serialized.includes("2026-07-16"), "must not include a local date");
  assert.ok(!serialized.includes("99"), "must not include input values");
});

test("unknown collections and non-error reasons are neutralized", () => {
  assert.equal(deleteLogEntry("otherRecords", new Error("stored localDate is invalid")).collection, "unknown");
  assert.equal(deleteLogEntry("handBrewRecords", "stored localDate is invalid").errorCode, "stored localDate is invalid");
  assert.equal(deleteLogEntry("handBrewRecords", 42).errorCode, "unclassified-validation-failure");
  assert.equal(deleteLogEntry("handBrewRecords", undefined).errorCode, "unclassified-validation-failure");
});
