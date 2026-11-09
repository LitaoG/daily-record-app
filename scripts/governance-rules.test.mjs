import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const filePath = path.join(root, relativePath);
  assert.ok(fs.existsSync(filePath), `Required document does not exist: ${relativePath}`);
  return fs.readFileSync(filePath, "utf8");
}

function getAllKotlinFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...getAllKotlinFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith(".kt")) {
      files.push(fullPath);
    }
  }
  return files;
}

// ── 1. 最小改动 · 优先复用两档制门禁 ──
test("minimal change two-tier rule is declared across documents with required tokens", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md", "docs/ENGINEERING_STANDARDS.md"];
  const requiredTokens = ["最小改动", "就地改", "优先复用", "同一语义", "常规档", "重构档", "豁免"];
  const forbiddenPhrases = [
    "无需最小改动",
    "不必最小改动",
    "不必复用",
    "可以重复实现",
    "允许新建平行实现",
  ];

  for (const site of declarationSites) {
    const text = read(site);
    for (const token of requiredTokens) {
      assert.ok(
        text.includes(token),
        `Missing required token "${token}" in minimal change rule at ${site}`,
      );
    }
    for (const forbidden of forbiddenPhrases) {
      assert.ok(
        !text.includes(forbidden),
        `Found forbidden phrase "${forbidden}" in ${site}`,
      );
    }
  }
});

// ── 2. 中间产物与工作区清理门禁 ──
test("temporary artifacts and workspace cleanup rule is declared and covers external directories", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md", "docs/REPOSITORY_HYGIENE.md"];
  const requiredTokens = [
    "中间产物",
    "临时脚本",
    "work/",
    "桌面",
    "连同目录本身一起删除",
    "workspace-hygiene.mjs",
  ];
  const forbiddenPhrases = [
    "不必清理中间产物",
    "无需清理中间产物",
    "允许残留",
    "不必清理桌面",
  ];

  for (const site of declarationSites) {
    const text = read(site);
    for (const token of requiredTokens) {
      assert.ok(
        text.includes(token),
        `Missing required token "${token}" in cleanup rule at ${site}`,
      );
    }
    for (const forbidden of forbiddenPhrases) {
      assert.ok(
        !text.includes(forbidden),
        `Found forbidden phrase "${forbidden}" in ${site}`,
      );
    }
  }
});

// ── 3. 收尾必做三件事门禁 ──
test("project closeout three obligations are declared and boundary is locked", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md"];
  const actions = [
    "清理中间产物",
    "该评论则评论",
    "该关闭则关闭",
  ];
  const boundaryTokens = [
    "严禁关闭",
    "OPEN",
    "需人拍板",
  ];
  const forbiddenPhrases = [
    "不必关闭",
    "无需关闭",
    "未完成也可以关闭",
    "可以关掉未完成",
    "不必评论",
    "无需评论",
  ];

  for (const site of declarationSites) {
    const text = read(site);
    for (const action of actions) {
      assert.ok(text.includes(action), `Missing closeout action "${action}" in ${site}`);
    }
    for (const boundary of boundaryTokens) {
      assert.ok(text.includes(boundary), `Missing closeout boundary token "${boundary}" in ${site}`);
    }
    for (const forbidden of forbiddenPhrases) {
      assert.ok(!text.includes(forbidden), `Found forbidden phrase "${forbidden}" in ${site}`);
    }
  }
});

// ── 4. 单文件行数硬顶扫描与规则门禁 ──
test("max file lines rule is declared and enforces physical line limits on Kotlin source", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md", "docs/ENGINEERING_STANDARDS.md"];
  for (const site of declarationSites) {
    const text = read(site);
    assert.ok(text.includes("400"), `Missing 400 target lines in ${site}`);
    assert.ok(text.includes("800"), `Missing 800 hard line limit in ${site}`);
  }

  // 物理行数真实扫描
  const ktDir = path.join(root, "app", "src", "main", "java");
  const ktFiles = getAllKotlinFiles(ktDir);
  assert.ok(ktFiles.length > 0, "No Kotlin files found in app/src/main/java");

  // 历史超长文件唯一豁免白名单与基线行数
  const historicalOversized = {
    "SettingsScreen.kt": 1450,
    "StatisticsMonthCards.kt": 850,
    "DailyRecordComponents.kt": 835,
  };

  const violations = [];
  for (const file of ktFiles) {
    const fileName = path.basename(file);
    const content = fs.readFileSync(file, "utf8");
    const lineCount = content.split(/\r?\n/).length;

    if (fileName in historicalOversized) {
      const allowedMax = historicalOversized[fileName];
      if (lineCount > allowedMax) {
        violations.push(`${fileName}: historical baseline exceeded (${lineCount} > ${allowedMax})`);
      }
    } else if (lineCount > 800) {
      violations.push(`${path.relative(root, file)}: ${lineCount} lines exceeds hard ceiling of 800`);
    }
  }

  assert.deepEqual(violations, [], `Kotlin source files exceeded maximum physical lines:\n${violations.join("\n")}`);
});

// ── 5. AI 证据标准与防形式主义门禁 ──
test("AI evidence standard is comprehensive and keeps anti-formalism escape hatches", () => {
  const evidenceDoc = read("docs/AI-EVIDENCE-STANDARD.md");

  // 必须声明的三档主张
  assert.ok(evidenceDoc.includes("〔已验证〕"), "Missing 〔已验证〕 in evidence standard");
  assert.ok(evidenceDoc.includes("〔推断〕"), "Missing 〔推断〕 in evidence standard");
  assert.ok(evidenceDoc.includes("〔未知〕"), "Missing 〔未知〕 in evidence standard");

  // 测试五态
  const testStates = ["已执行且通过", "已执行但失败", "尚未执行", "无法执行", "通过但覆盖不足"];
  for (const state of testStates) {
    assert.ok(evidenceDoc.includes(state), `Missing test state "${state}" in evidence standard`);
  }

  // 交付五类 A~E
  assert.ok(evidenceDoc.includes("| **A** |"), "Missing category A in delivery classification");
  assert.ok(evidenceDoc.includes("| **B** |"), "Missing category B in delivery classification");
  assert.ok(evidenceDoc.includes("| **C** |"), "Missing category C in delivery classification");
  assert.ok(evidenceDoc.includes("| **D** |"), "Missing category D in delivery classification");
  assert.ok(evidenceDoc.includes("| **E** |"), "Missing category E in delivery classification");

  // 必须保留防形式主义豁免条款
  assert.ok(evidenceDoc.includes("防形式主义"), "Missing anti-formalism heading");
  assert.ok(evidenceDoc.includes("不要求每句话都贴标签"), "Missing label escape hatch");
  assert.ok(evidenceDoc.includes("不要求输出大篇幅过程报告"), "Missing report brevity escape hatch");
});

// ── 6. 假定用户会出错与当场指出门禁 ──
test("user error correction rule is declared with criteria and non-overstepping boundaries", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md"];
  const tokens = ["假定用户会出错", "当场指出", "决策冲突", "违反", "指出 ≠ 越权"];

  for (const site of declarationSites) {
    const text = read(site);
    for (const token of tokens) {
      assert.ok(text.includes(token), `Missing user error correction token "${token}" in ${site}`);
    }
  }
});

// ── 7. 子代理分工与抽查复核门禁 ──
test("subagent orchestration rule mandates summary and first-hand sampling", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md"];
  const tokens = ["总代理", "子代理", "抽查", "原样转述"];

  for (const site of declarationSites) {
    const text = read(site);
    for (const token of tokens) {
      assert.ok(text.includes(token), `Missing subagent token "${token}" in ${site}`);
    }
  }
});

// ── 8. 改码同步文档门禁 ──
test("code-change doc sync rule is declared and forbids leaving stale docs", () => {
  const declarationSites = ["AGENTS.md", "docs/WORKFLOW.md"];
  const requiredTokens = [
    "改码必同步文档",
    "不留过时描述",
    "受影响",
    "旧行为",
  ];
  const forbiddenPhrases = [
    "无需同步文档",
    "不必更新文档",
    "文档可以以后再改",
    "允许文档过时",
  ];

  for (const site of declarationSites) {
    const text = read(site);
    for (const token of requiredTokens) {
      assert.ok(
        text.includes(token),
        `Missing required token "${token}" in doc sync rule at ${site}`,
      );
    }
    for (const forbidden of forbiddenPhrases) {
      assert.ok(
        !text.includes(forbidden),
        `Found forbidden phrase "${forbidden}" in ${site}`,
      );
    }
  }
});
