import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const VERSION_NAME_PATTERN = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/;

export function parseProperties(text) {
  const result = new Map();
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const separator = line.indexOf("=");
    if (separator <= 0) continue;
    result.set(line.slice(0, separator).trim(), line.slice(separator + 1).trim());
  }
  return result;
}

export function readReleaseMetadata(propertiesText, expectedTag) {
  const properties = parseProperties(propertiesText);
  const versionName = properties.get("dailyRecord.versionName") ?? "";
  const versionCodeText = properties.get("dailyRecord.versionCode") ?? "";
  const versionCode = Number(versionCodeText);

  if (!VERSION_NAME_PATTERN.test(versionName)) {
    throw new Error(`Invalid dailyRecord.versionName: ${versionName || "<missing>"}`);
  }
  if (!Number.isSafeInteger(versionCode) || versionCode <= 0) {
    throw new Error(`Invalid dailyRecord.versionCode: ${versionCodeText || "<missing>"}`);
  }
  if (expectedTag && expectedTag !== `v${versionName}`) {
    throw new Error(`Tag ${expectedTag} does not match v${versionName}`);
  }

  return {
    versionName,
    versionCode,
    apkName: `hand-brew-calendar-v${versionName}.apk`,
    notesPath: `docs/releases/v${versionName}.md`,
  };
}

/**
 * Android only accepts an overlay install when the new APK has a strictly
 * higher `versionCode`, so reusing or lowering it silently strands every
 * installed user (docs/RELEASE.md). This turns that prose rule into a gate.
 */
export function assertVersionCodeIncreases(versionCode, previousVersionCode, previousTag) {
  if (previousVersionCode == null) return;
  if (versionCode <= previousVersionCode) {
    throw new Error(
      `dailyRecord.versionCode ${versionCode} must be greater than ` +
        `${previousVersionCode} from ${previousTag}; Android rejects an overlay ` +
        "install that reuses or lowers it.",
    );
  }
}

/**
 * Reads `dailyRecord.versionCode` out of a previous release tag. Returns null
 * when the tag has no readable gradle.properties, so callers can distinguish
 * "no earlier release" from "a version code that fails validation".
 */
export function versionCodeForTag(tag, gitShow) {
  let text;
  try {
    text = gitShow(tag);
  } catch {
    return null;
  }
  if (typeof text !== "string") return null;
  const value = Number(parseProperties(text).get("dailyRecord.versionCode"));
  return Number.isSafeInteger(value) && value > 0 ? value : null;
}

function argumentValue(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

export function appendGitHubOutput(file, metadata) {
  const lines = [
    `version_name=${metadata.versionName}`,
    `version_code=${metadata.versionCode}`,
    `apk_name=${metadata.apkName}`,
    `notes_path=${metadata.notesPath}`,
  ];
  fs.appendFileSync(file, `${lines.join("\n")}\n`, "utf8");
}

/**
 * The highest release tag other than the tag being published. Sorted by
 * version so v1.0.0-beta.10 outranks v1.0.0-beta.9. Requires full tag
 * history; release.yml checks out with fetch-depth: 0 for exactly this.
 */
export function previousReleaseTag(tags, currentTag) {
  return tags
    .filter((tag) => tag !== currentTag)
    .sort((left, right) =>
      right.localeCompare(left, "en", { numeric: true, sensitivity: "base" }),
    )[0] ?? null;
}

function gitOutput(args) {
  return execFileSync("git", args, { encoding: "utf8" });
}

function main() {
  const repositoryRoot = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "..",
  );
  const propertiesText = fs.readFileSync(
    path.join(repositoryRoot, "gradle.properties"),
    "utf8",
  );
  const tag = argumentValue("--tag");
  const metadata = readReleaseMetadata(propertiesText, tag);
  if (!fs.existsSync(path.join(repositoryRoot, metadata.notesPath))) {
    throw new Error(`Release notes do not exist: ${metadata.notesPath}`);
  }
  if (tag) {
    const tags = gitOutput(["tag", "--list", "v*"])
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);
    const previousTag = previousReleaseTag(tags, tag);
    if (previousTag) {
      const previousVersionCode = versionCodeForTag(previousTag, (name) =>
        gitOutput(["show", `${name}:gradle.properties`]),
      );
      assertVersionCodeIncreases(metadata.versionCode, previousVersionCode, previousTag);
    }
  }
  const outputFile = argumentValue("--github-output");
  if (outputFile) appendGitHubOutput(outputFile, metadata);
  process.stdout.write(`${JSON.stringify(metadata)}\n`);
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href
) {
  main();
}
