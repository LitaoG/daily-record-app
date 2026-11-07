const REGION = "asia-east1";

/**
 * Default options for standard callable functions (e.g. writeDailyCountRecord).
 * In Cloud Functions v2 (Cloud Run backend), each instance can handle multiple
 * concurrent requests (concurrency: 80).
 * Limiting maxInstances prevents runaway scaling and billing spikes during DDoS.
 */
const CALLABLE_OPTIONS = Object.freeze({
  region: REGION,
  maxInstances: 10,
  timeoutSeconds: 30,
  concurrency: 80,
  memory: "256MiB",
});

/**
 * Options for high-cost batch operations (e.g. deleteAccountData).
 * Slightly longer timeout and lower concurrency to protect database bandwidth.
 */
const DELETION_CALLABLE_OPTIONS = Object.freeze({
  region: REGION,
  maxInstances: 5,
  timeoutSeconds: 60,
  concurrency: 10,
  memory: "256MiB",
});

/**
 * Options for Firestore write event triggers (e.g. validateWrittenRecord).
 * Critical: retry: false prevents Eventarc from infinitely retrying triggers
 * on unhandled exceptions or malformed data crashes.
 */
const TRIGGER_OPTIONS = Object.freeze({
  region: REGION,
  maxInstances: 5,
  timeoutSeconds: 30,
  retry: false,
});

module.exports = {
  REGION,
  CALLABLE_OPTIONS,
  DELETION_CALLABLE_OPTIONS,
  TRIGGER_OPTIONS,
};
