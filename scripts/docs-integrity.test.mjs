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
  const functionsPackage = JSON.parse(read("functions/package.json"));
  const functionsSource = read("functions/index.js");
  const ciWorkflow = read(".github/workflows/android-ci.yml");
  const database = read(
    "app/src/main/java/io/github/litaog/dailyrecord/core/database/DailyRecordDatabase.kt",
  );
  const readme = read("README.md");
  const product = read("docs/PRODUCT.md");
  const dataModel = read("docs/DATA_MODEL.md");

  const minSdk = build.match(/\bminSdk\s*=\s*(\d+)/)?.[1];
  const compileSdk = build.match(/release\((\d+)\)/)?.[1];
  const targetSdk = build.match(/\btargetSdk\s*=\s*(\d+)/)?.[1];
  const roomVersion = database.match(/const val SCHEMA_VERSION\s*=\s*(\d+)/)?.[1];
  assert.ok(minSdk, "Could not read minSdk");
  assert.ok(compileSdk, "Could not read compileSdk");
  assert.ok(targetSdk, "Could not read targetSdk");
  assert.ok(roomVersion, "Could not read Room version");

  assert.ok(readme.includes(`v${versionName}`), "README release version is stale");
  assert.ok(product.includes(`v${versionName}`), "Product contract version is stale");
  assert.ok(readme.includes(`minSdk ${minSdk}`), "README minSdk is stale");
  assert.ok(readme.includes(`compileSdk ${compileSdk}`), "README compileSdk is stale");
  assert.ok(readme.includes(`targetSdk ${targetSdk}`), "README targetSdk is stale");
  assert.ok(readme.includes(`Room v${roomVersion}`), "README Room schema is stale");
  assert.ok(dataModel.includes(`Room v${roomVersion}`), "DATA_MODEL Room schema is stale");
  assert.ok(readme.includes("docs/product/assets/readme/calendar-current.png"), "README current calendar screenshot is stale");
  assert.ok(readme.includes("docs/product/assets/readme/record-current.png"), "README current record screenshot is stale");
  assert.ok(readme.includes("docs/product/assets/readme/statistics-current.png"), "README current statistics screenshot is stale");
  assert.doesNotMatch(readme, /docs\/product\/audit\//, "README must not use historical audit screenshots as current images");
  assert.match(readme, /Node\.js 22/, "README Node.js runtime is stale");
  assert.match(readme, /Cloud Functions 2nd gen/, "README Functions generation is stale");
  assert.match(readme, /asia-east1/, "README Functions region is stale");
  assert.equal(functionsPackage.engines?.node, "22", "Functions Node engine is stale");
  assert.match(functionsSource, /firebase-functions\/v2\//, "Functions must use 2nd gen APIs");
  assert.match(functionsSource, /region:\s*["']asia-east1["']/, "Functions region is stale");
  assert.match(ciWorkflow, /api-level:\s*34/, "Android CI connected API level is stale");
  assert.ok(
    fs.existsSync(path.join(root, `docs/releases/v${versionName}.md`)),
    `Missing release notes for v${versionName}`,
  );
});

const LOOKUP_TABLE_HEADER = "| 我不确定的事 | 去查的权威文档 |";

function lookupTable(relativePath) {
  const lines = read(relativePath).split(/\r?\n/);
  const start = lines.indexOf(LOOKUP_TABLE_HEADER);
  assert.notEqual(start, -1, `${relativePath} has no lookup table`);
  const rows = [];
  for (let index = start; index < lines.length && lines[index].startsWith("|"); index += 1) {
    rows.push(lines[index]);
  }
  return rows;
}

function linkedRepoPaths(relativePath, rows) {
  const directory = path.dirname(path.join(root, relativePath));
  const linked = new Set();
  for (const row of rows) {
    for (const match of row.matchAll(/\]\(([^)#]+)(?:#[^)]*)?\)/g)) {
      linked.add(path.relative(root, path.resolve(directory, match[1])).replaceAll("\\", "/"));
    }
  }
  return linked;
}

function currentMustReadDocuments() {
  const catalog = read("docs/DOCUMENTATION_CATALOG.md");
  return [...catalog.matchAll(/^\| `([^`]+)` \| 当前必读 \|/gmu)].map((match) => match[1]);
}

test("workflow lookup table links every current must-read document", () => {
  const linked = linkedRepoPaths("docs/WORKFLOW.md", lookupTable("docs/WORKFLOW.md"));
  const mustRead = currentMustReadDocuments();
  assert.ok(mustRead.length > 0, "catalog lists no current must-read documents");
  assert.deepEqual(mustRead.filter((file) => !linked.has(file)), []);
});

test("docs README repeats the workflow lookup table row for row", () => {
  assert.deepEqual(lookupTable("docs/README.md"), lookupTable("docs/WORKFLOW.md"));
});

const PITFALL_SECTIONS = ["背景与证据", "影响与代价", "根因", "正确做法", "修复状态", "防复发机制", "权威规则"];

test("AI pitfalls keep sequential ids, one index row per entry and every section", () => {
  const pitfalls = read("docs/AI-PITFALLS.md").replace(/```[\s\S]*?```/g, "");
  const indexIds = [...pitfalls.matchAll(/^\| \[(P\d+)\]\(#/gm)].map((match) => match[1]);
  const headings = [...pitfalls.matchAll(/^## (P\d+) /gm)];
  const headingIds = headings.map((match) => match[1]);
  assert.ok(headingIds.length > 0, "no pitfall entries found");
  assert.deepEqual(
    headingIds,
    headingIds.map((_, index) => `P${index + 1}`),
    "pitfall ids must run P1..Pn without gaps, reuse or reordering",
  );
  assert.deepEqual(indexIds, headingIds, "index table rows and entry headings must match");
  headings.forEach((match, index) => {
    const end = index + 1 < headings.length ? headings[index + 1].index : pitfalls.length;
    const body = pitfalls.slice(match.index, end);
    const missing = PITFALL_SECTIONS.filter((section) => !body.includes(`**${section}**`));
    assert.deepEqual(missing, [], `${match[1]} is missing sections`);
  });
});
