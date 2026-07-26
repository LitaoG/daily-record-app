import assert from "node:assert/strict";
import test from "node:test";
import { parseProperties, readReleaseMetadata } from "./release-metadata.mjs";

test("properties parser ignores comments and preserves prerelease versions", () => {
  const properties = parseProperties(`
    # comment
    dailyRecord.versionCode=2
    dailyRecord.versionName=1.0.0-beta.1
  `);
  assert.equal(properties.get("dailyRecord.versionCode"), "2");
  assert.equal(properties.get("dailyRecord.versionName"), "1.0.0-beta.1");
});

test("release metadata derives stable artifact names", () => {
  const metadata = readReleaseMetadata(
    "dailyRecord.versionCode=2\ndailyRecord.versionName=1.0.0-beta.1\n",
    "v1.0.0-beta.1",
  );
  assert.deepEqual(metadata, {
    versionName: "1.0.0-beta.1",
    versionCode: 2,
    apkName: "hand-brew-calendar-v1.0.0-beta.1.apk",
    notesPath: "docs/releases/v1.0.0-beta.1.md",
  });
});

test("tag mismatch is rejected before a release build", () => {
  assert.throws(
    () => readReleaseMetadata(
      "dailyRecord.versionCode=2\ndailyRecord.versionName=1.0.0-beta.1\n",
      "v1.0.0",
    ),
    /does not match/,
  );
});

test("invalid or missing version codes are rejected", () => {
  assert.throws(
    () => readReleaseMetadata("dailyRecord.versionName=1.0.0\n", "v1.0.0"),
    /versionCode/,
  );
});
