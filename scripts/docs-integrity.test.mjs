import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ignoredDirectories = new Set([".git", "build", "node_modules"]);

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

function markdownFiles(directory = root) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    if (entry.isDirectory() && ignoredDirectories.has(entry.name)) return [];
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) return markdownFiles(absolutePath);
    return entry.isFile() && entry.name.endsWith(".md") ? [absolutePath] : [];
  });
}

function localTargets(markdown) {
  const targets = [];
  for (const match of markdown.matchAll(/\[[^\]]*]\(([^)]+)\)/g)) {
    targets.push(match[1]);
  }
  for (const match of markdown.matchAll(/<(?:a|img)\b[^>]*(?:href|src)="([^"]+)"/g)) {
    targets.push(match[1]);
  }
  return targets
    .map((target) => target.trim().replace(/^<|>$/g, "").split(/\s+"/, 1)[0])
    .filter((target) => target && !/^(?:https?:\/\/|mailto:|#)/.test(target));
}

function property(properties, name) {
  const match = properties.match(new RegExp(`^${name}=(.+)$`, "m"));
  assert.ok(match, `Missing ${name} in gradle.properties`);
  return match[1].trim();
}

test("all local Markdown links and images resolve", () => {
  const missing = [];
  for (const file of markdownFiles()) {
    const markdown = fs.readFileSync(file, "utf8");
    for (const target of localTargets(markdown)) {
      const withoutFragment = target.split("#", 1)[0];
      if (!withoutFragment) continue;
      const resolved = path.resolve(path.dirname(file), decodeURIComponent(withoutFragment));
      if (!fs.existsSync(resolved)) {
        missing.push(`${path.relative(root, file)} -> ${target}`);
      }
    }
  }
  assert.deepEqual(missing, []);
});

test("current documentation matches release and Android configuration", () => {
  const properties = read("gradle.properties");
  const versionName = property(properties, "dailyRecord.versionName");
  const build = read("app/build.gradle.kts");
  const database = read(
    "app/src/main/java/io/github/litaog/dailyrecord/core/database/DailyRecordDatabase.kt",
  );
  const readme = read("README.md");
  const product = read("docs/PRODUCT.md");
  const dataModel = read("docs/DATA_MODEL.md");

  const minSdk = build.match(/\bminSdk\s*=\s*(\d+)/)?.[1];
  const targetSdk = build.match(/\btargetSdk\s*=\s*(\d+)/)?.[1];
  const roomVersion = database.match(/\bversion\s*=\s*(\d+)/)?.[1];
  assert.ok(minSdk, "Could not read minSdk");
  assert.ok(targetSdk, "Could not read targetSdk");
  assert.ok(roomVersion, "Could not read Room version");

  assert.ok(readme.includes(`v${versionName}`), "README release version is stale");
  assert.ok(product.includes(`v${versionName}`), "Product contract version is stale");
  assert.ok(readme.includes(`minSdk ${minSdk}`), "README minSdk is stale");
  assert.ok(readme.includes(`targetSdk ${targetSdk}`), "README targetSdk is stale");
  assert.ok(readme.includes(`Room v${roomVersion}`), "README Room schema is stale");
  assert.ok(dataModel.includes(`Room v${roomVersion}`), "DATA_MODEL Room schema is stale");
  assert.ok(
    fs.existsSync(path.join(root, `docs/releases/v${versionName}.md`)),
    `Missing release notes for v${versionName}`,
  );
});
