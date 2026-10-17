const { HttpsError } = require("firebase-functions/v2/https");
const { createHash } = require("node:crypto");

/**
 * In-memory sliding window rate limiter.
 * Operates with ZERO database writes to satisfy the minimum-necessary-cost principle.
 * Combined with strict maxInstances in Cloud Functions v2, it bounds global invocation rates.
 */
class InMemoryRateLimiter {
  constructor() {
    this.buckets = new Map();
    // Periodically prune stale bucket entries every 5 minutes
    this.cleanupInterval = setInterval(() => this.pruneStaleBuckets(), 5 * 60 * 1000);
    if (this.cleanupInterval.unref) {
      this.cleanupInterval.unref();
    }
  }

  pruneStaleBuckets(now = Date.now()) {
    for (const [key, timestamps] of this.buckets.entries()) {
      const active = timestamps.filter((t) => now - t < 3600 * 1000);
      if (active.length === 0) {
        this.buckets.delete(key);
      } else {
        this.buckets.set(key, active);
      }
    }
  }

  /**
   * Hashes user UID to protect privacy in logs. Never logs cleartext UIDs or emails.
   */
  hashUid(uid) {
    if (!uid) return "anonymous";
    return createHash("sha256").update(String(uid)).digest("hex").slice(0, 16);
  }

  /**
   * Checks whether the given key is allowed under the specified rate limits.
   * @param {string} key - e.g. "write:user123"
   * @param {Array<{ windowMs: number, maxRequests: number }>} rules - List of window constraints
   * @param {string} operationName - Name for logging
   * @param {string} uid - User id for logging hash
   */
  checkRateLimit(key, rules, operationName, uid) {
    const now = Date.now();
    let timestamps = this.buckets.get(key) || [];

    // Filter to largest window in rules
    const maxWindow = Math.max(...rules.map((r) => r.windowMs));
    timestamps = timestamps.filter((t) => now - t < maxWindow);

    for (const rule of rules) {
      const windowStart = now - rule.windowMs;
      const count = timestamps.filter((t) => t >= windowStart).length;
      if (count >= rule.maxRequests) {
        console.warn(JSON.stringify({
          severity: "WARNING",
          event: "RATE_LIMIT_EXCEEDED",
          operation: operationName,
          uidHash: this.hashUid(uid),
          requestCount: count + 1,
          maxAllowed: rule.maxRequests,
          windowMs: rule.windowMs,
        }));
        throw new HttpsError(
          "resource-exhausted",
          "Too many requests. Please slow down and try again later.",
        );
      }
    }

    timestamps.push(now);
    this.buckets.set(key, timestamps);
  }

  resetForTesting() {
    this.buckets.clear();
  }
}

const rateLimiter = new InMemoryRateLimiter();

const WRITE_RULES = Object.freeze([
  { windowMs: 5 * 1000, maxRequests: 5 },       // Max 5 writes per 5 seconds (burst)
  { windowMs: 60 * 1000, maxRequests: 30 },     // Max 30 writes per minute
  { windowMs: 3600 * 1000, maxRequests: 120 },  // Max 120 writes per hour
]);

const DELETION_RULES = Object.freeze([
  { windowMs: 60 * 1000, maxRequests: 2 },      // Max 2 account deletion calls per minute
  { windowMs: 3600 * 1000, maxRequests: 5 },    // Max 5 account deletion calls per hour
]);

function enforceWriteRateLimit(uid) {
  rateLimiter.checkRateLimit(`write:${uid}`, WRITE_RULES, "writeDailyCountRecord", uid);
}

function enforceDeletionRateLimit(uid) {
  rateLimiter.checkRateLimit(`delete:${uid}`, DELETION_RULES, "deleteAccountData", uid);
}

module.exports = {
  InMemoryRateLimiter,
  rateLimiter,
  enforceWriteRateLimit,
  enforceDeletionRateLimit,
  WRITE_RULES,
  DELETION_RULES,
};
