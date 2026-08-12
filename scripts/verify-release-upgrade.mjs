import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const PACKAGE_NAME = "io.github.litaog.dailyrecord";
const ACTIVITY_NAME = `${PACKAGE_NAME}/.MainActivity`;

function argumentValue(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

function requiredArgument(name) {
  const value = argumentValue(name);
  if (!value) throw new Error(`Missing ${name}`);
  return path.resolve(value);
}

function findAdb() {
  const sdkRoot = process.env.ANDROID_SDK_ROOT
    ?? process.env.ANDROID_HOME
    ?? (process.env.LOCALAPPDATA
      ? path.join(process.env.LOCALAPPDATA, "Android", "Sdk")
      : undefined);
  if (!sdkRoot) throw new Error("ANDROID_SDK_ROOT or ANDROID_HOME is required");
  return path.join(sdkRoot, "platform-tools", os.platform() === "win32" ? "adb.exe" : "adb");
}

function decodeXml(value) {
  return value
    .replaceAll("&quot;", "\"")
    .replaceAll("&apos;", "'")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&amp;", "&");
}

function parseNodes(xml) {
  return [...xml.matchAll(/<node\b[^>]*>/g)].map((match) => {
    const attributes = {};
    for (const attribute of match[0].matchAll(/([\w-]+)="([^"]*)"/g)) {
      attributes[attribute[1]] = decodeXml(attribute[2]);
    }
    return attributes;
  });
}

function boundsCenter(bounds) {
  const match = bounds?.match(/^\[(\d+),(\d+)]\[(\d+),(\d+)]$/);
  if (!match) throw new Error(`Invalid node bounds: ${bounds}`);
  return {
    x: Math.round((Number(match[1]) + Number(match[3])) / 2),
    y: Math.round((Number(match[2]) + Number(match[4])) / 2),
  };
}

function sleep(milliseconds) {
  const end = Date.now() + milliseconds;
  while (Date.now() < end) {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, end - Date.now());
  }
}

function main() {
  const device = argumentValue("--device") ?? "emulator-5554";
  if (!/^emulator-\d+$/.test(device)) {
    throw new Error("This destructive upgrade verifier only runs on an Android emulator");
  }
  const baselineApk = requiredArgument("--baseline");
  const candidateApk = requiredArgument("--candidate");
  for (const apk of [baselineApk, candidateApk]) {
    if (!fs.existsSync(apk) || !fs.statSync(apk).isFile()) {
      throw new Error(`APK not found: ${apk}`);
    }
  }

  const adb = findAdb();
  const runAdb = (args, options = {}) => execFileSync(
    adb,
    ["-s", device, ...args],
    { encoding: "utf8", ...options },
  ).trim();
  const dumpNodes = () => {
    runAdb(["shell", "uiautomator", "dump", "/sdcard/daily-record-window.xml"]);
    return parseNodes(runAdb(["exec-out", "cat", "/sdcard/daily-record-window.xml"]));
  };
  const waitForNode = (predicate, description, timeoutMillis = 30_000) => {
    const deadline = Date.now() + timeoutMillis;
    while (Date.now() < deadline) {
      const node = dumpNodes().find(predicate);
      if (node) return node;
      sleep(500);
    }
    throw new Error(`Timed out waiting for ${description}`);
  };
  const clickNode = (node) => {
    const center = boundsCenter(node.bounds);
    runAdb(["shell", "input", "tap", String(center.x), String(center.y)]);
  };
  const clickText = (text) => clickNode(waitForNode(
    (node) => node.text === text || node["content-desc"] === text,
    `text ${text}`,
  ));
  const clickDescription = (description) => clickNode(waitForNode(
    (node) => node["content-desc"] === description,
    `content description ${description}`,
  ));
  const startApp = () => {
    runAdb(["shell", "am", "force-stop", PACKAGE_NAME]);
    runAdb(["shell", "am", "start", "-W", "-n", ACTIVITY_NAME]);
  };

  try {
    runAdb(["uninstall", PACKAGE_NAME]);
  } catch {
    // A clean emulator may not have the package yet.
  }
  runAdb(["install", "-r", baselineApk]);
  startApp();

  const initialNodes = dumpNodes();
  if (!initialNodes.some((node) => node.tag === "calendar_screen")) {
    clickText("暂不登录，先使用“本机记录”");
  }

  const dateText = runAdb(["shell", "date", "+%Y-%m-%d"]);
  const [year, month, day] = dateText.split("-").map(Number);
  const datePrefix = `${year}年${month}月${day}日，`;
  clickNode(waitForNode(
    (node) => node["content-desc"]?.startsWith(datePrefix),
    "today calendar cell",
  ));
  clickDescription("增加一次");
  clickText("保存记录");
  waitForNode(
    (node) => node["content-desc"]?.startsWith(datePrefix)
      && node["content-desc"].includes("自慰，1 次"),
    "saved one-count calendar state",
  );

  runAdb(["shell", "am", "force-stop", PACKAGE_NAME]);
  const installOutput = runAdb(["install", "-r", candidateApk]);
  if (!installOutput.includes("Success")) {
    throw new Error(`Candidate overlay install failed: ${installOutput}`);
  }
  startApp();
  waitForNode(
    (node) => node["content-desc"]?.startsWith(datePrefix)
      && node["content-desc"].includes("自慰，1 次"),
    "preserved Room record after overlay",
  );

  process.stdout.write(`${JSON.stringify({
    device,
    overlayInstall: "success",
    roomRecordPreserved: true,
  })}\n`);
}

main();
