// Pre-delivery hygiene check (docs/WORKFLOW.md section 9). It fails when task
// scratch is left behind: a non-empty work/ folder, untracked files, or a
// registered worktree whose folder no longer exists. It is a manual final
// check and is not part of CI, because CI checkouts never contain scratch.
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function git(...args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" });
}

const problems = [];

const workDirectory = path.join(root, "work");
// The scheduled review-patrol automation keeps its persistent state file in
// work/ (see docs/REPOSITORY_HYGIENE.md). It is a standing fixture, not task
// scratch, so it is the only allowed resident; anything else fails the check.
const allowedWorkResidents = new Set(["agent-watch-state.json"]);
if (fs.existsSync(workDirectory)) {
  const leftovers = fs.readdirSync(workDirectory).filter((name) => !allowedWorkResidents.has(name));
  if (leftovers.length > 0) {
    problems.push(`work/ is not empty: ${leftovers.join(", ")}`);
  }
}

const untracked = git("status", "--porcelain=v1", "--untracked-files=all")
  .split("\n")
  .filter((line) => line.startsWith("?? "))
  .map((line) => line.slice(3));
if (untracked.length > 0) {
  problems.push(`untracked files: ${untracked.join(", ")}`);
}

const staleWorktrees = git("worktree", "list", "--porcelain")
  .split(/\n\s*\n/)
  .filter((block) => /^prunable/m.test(block))
  .map((block) => block.match(/^worktree (.+)$/m)?.[1] ?? block.split("\n", 1)[0]);
if (staleWorktrees.length > 0) {
  problems.push(`stale worktrees (check, then run git worktree prune): ${staleWorktrees.join(", ")}`);
}

if (problems.length > 0) {
  console.error("Workspace hygiene check failed:");
  for (const problem of problems) {
    console.error(`- ${problem}`);
  }
  process.exitCode = 1;
} else {
  console.log("Workspace hygiene check passed: work/ is empty, no untracked files, no stale worktrees.");
}
