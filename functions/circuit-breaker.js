const { HttpsError } = require("firebase-functions/v2/https");

/**
 * Cache duration for the circuit breaker state.
 * 30 seconds prevents read amplification during attack spikes while still
 * reacting promptly to admin kill switch activations.
 */
const CACHE_TTL_MILLIS = 30 * 1000;

let cachedState = null;
let cacheExpiresAt = 0;

function getEnvFlags() {
  return {
    allDisabled: process.env.DAILY_RECORD_KILL_SWITCH_ALL === "true",
    writesDisabled: process.env.DAILY_RECORD_KILL_SWITCH_WRITES === "true",
    deletionsDisabled: process.env.DAILY_RECORD_KILL_SWITCH_DELETIONS === "true",
    triggersDisabled: process.env.DAILY_RECORD_KILL_SWITCH_TRIGGERS === "true",
  };
}

/**
 * Fetches circuit breaker state from Firestore `_system/circuitBreaker` or memory cache.
 * Fails safely open (allowing operations) if Firestore read fails, unless env kill switch is set.
 */
async function getCircuitBreakerState(db) {
  const env = getEnvFlags();
  if (env.allDisabled) {
    return { writesAllowed: false, deletionsAllowed: false, triggersAllowed: false, source: "env" };
  }

  const now = Date.now();
  if (cachedState && now < cacheExpiresAt) {
    return cachedState;
  }

  let firestoreFlags = {};
  if (db) {
    try {
      const doc = await db.collection("_system").doc("circuitBreaker").get();
      if (doc.exists) {
        firestoreFlags = doc.data() || {};
      }
    } catch (error) {
      console.warn(JSON.stringify({
        severity: "WARNING",
        event: "CIRCUIT_BREAKER_CHECK_ERROR",
        message: "Failed to read circuit breaker config from Firestore, falling back to cached or default state",
      }));
    }
  }

  const writesAllowed = !env.writesDisabled && firestoreFlags.writesDisabled !== true;
  const deletionsAllowed = !env.deletionsDisabled && firestoreFlags.deletionsDisabled !== true;
  const triggersAllowed = !env.triggersDisabled && firestoreFlags.triggersDisabled !== true;

  cachedState = { writesAllowed, deletionsAllowed, triggersAllowed, source: "config" };
  cacheExpiresAt = now + CACHE_TTL_MILLIS;
  return cachedState;
}

function resetCircuitBreakerCacheForTesting() {
  cachedState = null;
  cacheExpiresAt = 0;
}

async function requireWritesAllowed(db) {
  const state = await getCircuitBreakerState(db);
  if (!state.writesAllowed) {
    console.warn(JSON.stringify({
      severity: "WARNING",
      event: "CIRCUIT_BREAKER_BLOCKED_WRITE",
      message: "Sync write was blocked by the backend circuit breaker kill switch",
    }));
    throw new HttpsError(
      "unavailable",
      "Cloud synchronization is temporarily paused for maintenance. Local records remain safe.",
    );
  }
}

async function requireDeletionsAllowed(db) {
  const state = await getCircuitBreakerState(db);
  if (!state.deletionsAllowed) {
    console.warn(JSON.stringify({
      severity: "WARNING",
      event: "CIRCUIT_BREAKER_BLOCKED_DELETION",
      message: "Account data deletion was blocked by the backend circuit breaker kill switch",
    }));
    throw new HttpsError(
      "unavailable",
      "Account data deletion is temporarily paused for maintenance. Please try again later.",
    );
  }
}

async function areTriggersAllowed(db) {
  const state = await getCircuitBreakerState(db);
  return state.triggersAllowed;
}

module.exports = {
  getCircuitBreakerState,
  requireWritesAllowed,
  requireDeletionsAllowed,
  areTriggersAllowed,
  resetCircuitBreakerCacheForTesting,
};
