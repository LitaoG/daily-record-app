import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const productionRoot = path.join(root, "app", "src", "main");
const allowedCopyFile = path.join(
  productionRoot,
  "java",
  "io",
  "github",
  "litaog",
  "dailyrecord",
  "core",
  "common",
  "AppCopy.kt",
);
const appNameResource = path.join(productionRoot, "res", "values", "strings.xml");
const textExtensions = new Set([".kt", ".java", ".xml", ".kts", ".gradle", ".properties"]);
const cjk = /[\u3400-\u9fff]/u;

function filesUnder(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) return filesUnder(absolutePath);
    return [absolutePath];
  });
}

test("production Chinese copy stays in AppCopy or Android resources", () => {
  const violations = [];
  for (const file of filesUnder(productionRoot)) {
    if (!textExtensions.has(path.extname(file).toLowerCase())) continue;
    const text = fs.readFileSync(file, "utf8");
    if (file === allowedCopyFile) continue;

    for (const [index, line] of text.split(/\r?\n/).entries()) {
      if (!cjk.test(line)) continue;
      if (file === appNameResource && /<string\s+name="app_name">/.test(line)) continue;
      violations.push(`${path.relative(root, file)}:${index + 1}`);
    }
  }

  assert.deepEqual(
    violations,
    [],
    "Move user-facing Chinese copy into AppCopy.kt; keep only app_name in strings.xml.",
  );
});
