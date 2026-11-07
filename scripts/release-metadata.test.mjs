import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import {
  assertVersionCodeIncreases,
  parseProperties,
  previousReleaseTag,
  readReleaseMetadata,
  versionCodeForTag,
} from "./release-metadata.mjs";

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

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

test("a reused or lowered versionCode is rejected", () => {
  assert.throws(
    () => assertVersionCodeIncreases(4, 4, "v1.0.0-beta.3"),
    /must be greater than 4/,
  );
  assert.throws(
    () => assertVersionCodeIncreases(3, 4, "v1.0.0-beta.3"),
    /must be greater than 4/,
  );
});

test("a higher versionCode passes and the first release has no predecessor", () => {
  assert.doesNotThrow(() => assertVersionCodeIncreases(5, 4, "v1.0.0-beta.3"));
  assert.doesNotThrow(() => assertVersionCodeIncreases(1, null, null));
});

test("previous release tag ignores the tag being published and sorts by version", () => {
  const tags = ["v1.0.0-beta.1", "v1.0.0-beta.9", "v1.0.0-beta.10"];
  assert.equal(previousReleaseTag(tags, "v1.0.0-beta.11"), "v1.0.0-beta.10");
  assert.equal(previousReleaseTag(tags, "v1.0.0-beta.10"), "v1.0.0-beta.9");
  assert.equal(previousReleaseTag(["v1.0.0-beta.1"], "v1.0.0-beta.1"), null);
  assert.equal(previousReleaseTag([], "v1.0.0-beta.1"), null);
});

test("an unreadable predecessor tag yields no comparison value", () => {
  assert.equal(
    versionCodeForTag("v1.0.0-beta.9", () => "dailyRecord.versionCode=9\n"),
    9,
  );
  assert.equal(
    versionCodeForTag("v1.0.0-beta.9", () => {
      throw new Error("unknown revision");
    }),
    null,
  );
  assert.equal(versionCodeForTag("v1.0.0-beta.9", () => "no such key\n"), null);
});

test("the CLI writes every key release.yml consumes to GITHUB_OUTPUT", () => {
  const outputFile = path.join(
    fs.mkdtempSync(path.join(os.tmpdir(), "daily-record-release-")),
    "github-output.txt",
  );
  const stdout = execFileSync(
    process.execPath,
    [
      path.join(repositoryRoot, "scripts", "release-metadata.mjs"),
      "--tag",
      "v1.0.0-beta.3",
      "--github-output",
      outputFile,
    ],
    { cwd: repositoryRoot, encoding: "utf8" },
  );
  const written = fs.readFileSync(outputFile, "utf8");
  // These exact key names are read by .github/workflows/release.yml; a rename
  // here would otherwise only surface as an empty path during publish.
  for (const key of ["version_name", "version_code", "apk_name", "notes_path"]) {
    assert.match(written, new RegExp(`^${key}=.+$`, "m"), `missing ${key}`);
  }
  assert.match(written, /^notes_path=docs\/releases\/v1\.0\.0-beta\.3\.md$/m);
  assert.match(stdout, /"versionName":"1\.0\.0-beta\.3"/);
});

test("the CLI refuses a tag whose release notes do not exist", () => {
  assert.throws(
    () =>
      execFileSync(
        process.execPath,
        [
          path.join(repositoryRoot, "scripts", "release-metadata.mjs"),
          "--tag",
          "v9.9.9",
        ],
        { cwd: repositoryRoot, encoding: "utf8", stdio: "pipe" },
      ),
    /Command failed/,
  );
});
