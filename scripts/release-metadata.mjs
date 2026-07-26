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

function argumentValue(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

function appendGitHubOutput(file, metadata) {
  const lines = [
    `version_name=${metadata.versionName}`,
    `version_code=${metadata.versionCode}`,
    `apk_name=${metadata.apkName}`,
    `notes_path=${metadata.notesPath}`,
  ];
  fs.appendFileSync(file, `${lines.join("\n")}\n`, "utf8");
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
  const metadata = readReleaseMetadata(propertiesText, argumentValue("--tag"));
  if (!fs.existsSync(path.join(repositoryRoot, metadata.notesPath))) {
    throw new Error(`Release notes do not exist: ${metadata.notesPath}`);
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
