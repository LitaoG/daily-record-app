const { HttpsError } = require("firebase-functions/v2/https");
const { createHash } = require("node:crypto");

function hashUid(uid) {
  if (!uid) return "anonymous";
  return createHash("sha256").update(String(uid)).digest("hex").slice(0, 16);
}

/**
 * Checks App Check attestation token from the callable request.
 * Supports phased rollout:
 * - "monitoring" (default): logs unverified requests for audit without blocking legacy clients.
 * - "enforced": strictly rejects unverified requests with unauthenticated error.
 */
function verifyAppCheck(request, operationName, { mode = null } = {}) {
  const currentMode = mode ||
    (process.env.APP_CHECK_ENFORCEMENT === "true" ? "enforced" : "monitoring");

  const hasValidAppCheck = Boolean(request.app && request.app.appId);
  const uidHash = hashUid(request.auth && request.auth.uid);

  if (!hasValidAppCheck) {
    if (currentMode === "enforced") {
      console.warn(JSON.stringify({
        severity: "WARNING",
        event: "APP_CHECK_BLOCKED",
        operation: operationName,
        uidHash,
        message: "Request rejected due to missing or invalid App Check attestation token",
      }));
      throw new HttpsError(
        "unauthenticated",
        "App attestation failed. Please ensure the app is up to date and genuine.",
      );
    } else {
      // Monitoring mode: log the metric for Cloud Monitoring visibility
      console.info(JSON.stringify({
        severity: "INFO",
        event: "APP_CHECK_UNVERIFIED",
        operation: operationName,
        uidHash,
        message: "Request arrived without valid App Check token in monitoring mode",
      }));
      return false;
    }
  }

  return true;
}

module.exports = {
  verifyAppCheck,
};
