import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const catalogPath = path.join(root, "docs", "DOCUMENTATION_CATALOG.md");

function trackedDocumentationFiles() {
  return execFileSync("git", ["ls-files", "-z"], { cwd: root })
    .toString("utf8")
    .split("\0")
    .filter(Boolean)
    .filter((file) => /\.(?:md|markdown|doc|docx)$/iu.test(file))
    .map((file) => file.replaceAll("\\", "/"));
}

function catalogEntries() {
  const catalog = fs.readFileSync(catalogPath, "utf8");
  const entries = [];
  const entryPattern = /^\| `([^`]+)` \| ([^|]+) \| (P[0-3]) \| (必需|建议|按范围|留档) \| (是|按范围|否) \| (是|按范围|否) \|/gmu;
  for (const match of catalog.matchAll(entryPattern)) {
    entries.push({
      file: match[1],
      status: match[2].trim(),
      priority: match[3],
      necessity: match[4],
      mustRead: match[5],
      aiMustRead: match[6],
    });
  }
  return entries;
}

test("documentation catalog covers every tracked Markdown/document file exactly once", () => {
  const expected = trackedDocumentationFiles();
  const catalogRelativePath = "docs/DOCUMENTATION_CATALOG.md";
  if (fs.existsSync(catalogPath) && !expected.includes(catalogRelativePath)) {
    expected.push(catalogRelativePath);
  }
  expected.sort();
  const entries = catalogEntries();
  const listed = entries.map((entry) => entry.file);

  assert.equal(new Set(listed).size, listed.length, "catalog contains duplicate paths");
  assert.deepEqual(listed.slice().sort(), expected);
  assert.ok(entries.every((entry) => entry.status.length > 0), "every entry needs a status");
});

test("documentation catalog uses explicit AI routing for every entry", () => {
  const entries = catalogEntries();
  assert.ok(entries.length > 0, "catalog is empty");
  assert.ok(entries.some((entry) => entry.aiMustRead === "是"), "catalog needs AI-required documents");
  assert.ok(entries.some((entry) => entry.aiMustRead === "按范围"), "catalog needs scoped AI documents");
  assert.ok(entries.some((entry) => entry.aiMustRead === "否"), "catalog needs non-required historical documents");
});
