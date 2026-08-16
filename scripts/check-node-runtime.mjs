const REQUIRED_NODE_MAJOR = 22;
const currentMajor = Number(process.versions.node.split(".")[0]);

if (currentMajor !== REQUIRED_NODE_MAJOR) {
  console.error(
    `Firebase Emulator tests require Node.js ${REQUIRED_NODE_MAJOR} ` +
      `(declared in functions/package.json engines); found ${process.versions.node}. ` +
      "Version drift silently breaks the Functions emulator and invalidates the test results.",
  );
  process.exit(1);
}
