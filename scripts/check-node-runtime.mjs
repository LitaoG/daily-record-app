import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Read the required major from the declaration this guard is supposed to
// enforce. Hardcoding it here would let functions/package.json engines drift
// while the guard kept passing against a stale literal. The root package.json
// engines field must agree with the Functions declaration; docs-integrity
// additionally checks README/DEVELOPMENT/FIREBASE_SETUP mention the same major.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const functionsPackage = JSON.parse(
  fs.readFileSync(path.join(root, "functions", "package.json"), "utf8"),
);
const REQUIRED_NODE_MAJOR = Number(functionsPackage.engines?.node);
const rootPackage = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const currentMajor = Number(process.versions.node.split(".")[0]);

if (!Number.isInteger(REQUIRED_NODE_MAJOR)) {
  console.error(
    "functions/package.json must declare a numeric engines.node major " +
      "(CI and the emulator rely on it).",
  );
  process.exit(1);
}

if (rootPackage.engines?.node !== functionsPackage.engines?.node) {
  console.error(
    `package.json engines.node (${rootPackage.engines?.node}) must match ` +
      `functions/package.json engines.node (${functionsPackage.engines?.node}).`,
  );
  process.exit(1);
}

if (currentMajor !== REQUIRED_NODE_MAJOR) {
  console.error(
    `Firebase Emulator tests require Node.js ${REQUIRED_NODE_MAJOR} ` +
      `(declared in functions/package.json engines); found ${process.versions.node}. ` +
      "Version drift silently breaks the Functions emulator and invalidates the test results.",
  );
  process.exit(1);
}
