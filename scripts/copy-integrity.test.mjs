import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const productionRoot = path.join(root, "app", "src", "main");
const commonRoot = path.join(
  productionRoot,
  "java",
  "io",
  "github",
  "litaog",
  "dailyrecord",
  "core",
  "common",
);
// Bilingual contract: Chinese copy lives only in the Chinese language file,
// and the English language file may only carry the self-named "中文" option.
const allowedCopyFiles = new Set([
  path.join(commonRoot, "AppCopy.kt"),
  path.join(commonRoot, "ZhStrings.kt"),
]);
const englishLanguageFile = path.join(commonRoot, "EnStrings.kt");
const appNameResource = path.join(productionRoot, "res", "values", "strings.xml");
const textExtensions = new Set([".kt", ".java", ".xml", ".kts", ".gradle", ".properties"]);
const cjk = /[\u3400-\u9fff]/u;
const legacyHandBrewTerm = String.fromCodePoint(0x624b, 0x51b2);

function filesUnder(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) return filesUnder(absolutePath);
    return [absolutePath];
  });
}

test("production Chinese copy stays in the language files or Android resources", () => {
  const violations = [];
  for (const file of filesUnder(productionRoot)) {
    if (!textExtensions.has(path.extname(file).toLowerCase())) continue;
    const text = fs.readFileSync(file, "utf8");
    if (allowedCopyFiles.has(file)) continue;

    for (const [index, line] of text.split(/\r?\n/).entries()) {
      if (!cjk.test(line)) continue;
      if (file === appNameResource && /<string\s+name="app_name">/.test(line)) continue;
      // The English set intentionally shows the Chinese option in its own script.
      if (
        file === englishLanguageFile &&
        /languageZh\s*=\s*"/.test(line) &&
        line.includes(String.fromCodePoint(0x4e2d, 0x6587))
      ) {
        continue;
      }
      violations.push(`${path.relative(root, file)}:${index + 1}`);
    }
  }

  assert.deepEqual(
    violations,
    [],
    "Move user-facing Chinese copy into ZhStrings.kt; keep only app_name in strings.xml" +
      " and the self-named 中文 option in EnStrings.kt.",
  );
});

test("tracked files do not reintroduce the retired Chinese module term", () => {
  const trackedFiles = execFileSync("git", ["ls-files", "-z"], {
    cwd: root,
    encoding: "utf8",
  })
    .split("\0")
    .filter(Boolean);
  const forbiddenBytes = Buffer.from(legacyHandBrewTerm, "utf8");
  const violations = trackedFiles.filter((relativePath) => {
    const absolutePath = path.join(root, relativePath);
    return fs.existsSync(absolutePath) && fs.readFileSync(absolutePath).includes(forbiddenBytes);
  });

  assert.deepEqual(
    violations,
    [],
    "Replace the retired Chinese module term in every Git-tracked text artifact.",
  );
});
