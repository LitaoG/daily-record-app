import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  InMemoryRateLimiter,
  WRITE_RULES,
  DELETION_RULES,
} from "../functions/rate-limiter.js";
import {
  getCircuitBreakerState,
  requireWritesAllowed,
  requireDeletionsAllowed,
  areTriggersAllowed,
  resetCircuitBreakerCacheForTesting,
} from "../functions/circuit-breaker.js";
import { verifyAppCheck } from "../functions/app-check-gate.js";
import {
  REGION,
  CALLABLE_OPTIONS,
  DELETION_CALLABLE_OPTIONS,
  TRIGGER_OPTIONS,
} from "../functions/resource-config.js";

// Source of functions/index.js, loaded once: the wiring tests below must fail
// loudly if a resource-config option set stops being passed to the runtime.
const INDEX_SOURCE = readFileSync(
  new URL("../functions/index.js", import.meta.url),
  "utf8",
);

test("Resource config locks the compute quartet documented in the cost matrix", () => {
  assert.equal(REGION, "asia-east1");
  // Row 3 of the cost matrix: bounded instances, timeouts and concurrency so
  // a traffic flood cannot inflate the Cloud Run compute bill.
  assert.deepEqual(CALLABLE_OPTIONS, {
    region: "asia-east1",
    maxInstances: 10,
    timeoutSeconds: 30,
    concurrency: 80,
    memory: "256MiB",
  });
  assert.deepEqual(DELETION_CALLABLE_OPTIONS, {
    region: "asia-east1",
    maxInstances: 5,
    timeoutSeconds: 60,
    concurrency: 10,
    memory: "256MiB",
  });
  assert.deepEqual(TRIGGER_OPTIONS, {
    region: "asia-east1",
    maxInstances: 5,
    timeoutSeconds: 30,
    retry: false,
    memory: "256MiB",
  });
});

test("Every callable and trigger actually receives its resource options", () => {
  assert.match(
    INDEX_SOURCE,
    /exports\.writeDailyCountRecord = onCall\(CALLABLE_OPTIONS,/,
  );
  assert.match(
    INDEX_SOURCE,
    /exports\.deleteAccountData = onCall\(DELETION_CALLABLE_OPTIONS,/,
  );
  // Triggers spread TRIGGER_OPTIONS into their per-trigger options object.
  assert.match(
    INDEX_SOURCE,
    /exports\.validateHandBrewRecord = onDocumentWritten\(\s*\{[\s\S]*?\.\.\.TRIGGER_OPTIONS,/,
  );
  assert.match(
    INDEX_SOURCE,
    /exports\.validateSexRecord = onDocumentWritten\(\s*\{[\s\S]*?\.\.\.TRIGGER_OPTIONS,/,
  );
});

test("Zero-I/O App Check gate runs before the circuit breaker read and rate limiter", () => {
  const assertGateOrder = (section, name) => {
    const authIndex = section.indexOf("= requireAuthenticated(request)");
    const appCheckIndex = section.indexOf("verifyAppCheck(request");
    const breakerIndex = section.indexOf("Allowed(db)");
    assert.ok(authIndex !== -1 && appCheckIndex !== -1 && breakerIndex !== -1, name);
    assert.ok(appCheckIndex < breakerIndex, `${name}: App Check must precede the breaker read`);
  };

  const writeStart = INDEX_SOURCE.indexOf("exports.writeDailyCountRecord");
  const writeEnd = INDEX_SOURCE.indexOf("exports.deleteAccountData");
  assertGateOrder(INDEX_SOURCE.slice(writeStart, writeEnd), "writeDailyCountRecord");

  const deletionStart = writeEnd;
  const deletionEnd = INDEX_SOURCE.indexOf("exports.validateHandBrewRecord");
  assertGateOrder(INDEX_SOURCE.slice(deletionStart, deletionEnd), "deleteAccountData");
});

test("Rate limiter allows requests within threshold and blocks bursts", () => {
  const limiter = new InMemoryRateLimiter();
  const uid = "test-user-1";

  // First 5 requests within 5s window should succeed
  for (let i = 0; i < 5; i++) {
    assert.doesNotThrow(() => {
      limiter.checkRateLimit(`write:${uid}`, WRITE_RULES, "writeDailyCountRecord", uid);
    });
  }

  // 6th request within 5s window should be blocked with resource-exhausted
  assert.throws(
    () => {
      limiter.checkRateLimit(`write:${uid}`, WRITE_RULES, "writeDailyCountRecord", uid);
    },
    (err) => {
      assert.equal(err.code, "resource-exhausted");
      return true;
    },
  );

  // A different user is NOT blocked (per-UID isolation)
  const otherUid = "test-user-2";
  assert.doesNotThrow(() => {
    limiter.checkRateLimit(`write:${otherUid}`, WRITE_RULES, "writeDailyCountRecord", otherUid);
  });
});

test("Rate limiter hashes UIDs and never reveals cleartext identity", () => {
  const limiter = new InMemoryRateLimiter();
  const uid = "sensitive-user-uid-12345";
  const hash = limiter.hashUid(uid);

  assert.notEqual(hash, uid);
  assert.equal(typeof hash, "string");
  assert.equal(hash.length, 16);
  assert.equal(limiter.hashUid(uid), hash); // Deterministic
});

test("Deletion rate limiter enforces strict low-frequency calling", () => {
  const limiter = new InMemoryRateLimiter();
  const uid = "deletion-user";

  // Max 2 calls per minute
  assert.doesNotThrow(() => {
    limiter.checkRateLimit(`delete:${uid}`, DELETION_RULES, "deleteAccountData", uid);
  });
  assert.doesNotThrow(() => {
    limiter.checkRateLimit(`delete:${uid}`, DELETION_RULES, "deleteAccountData", uid);
  });

  // 3rd call in same minute must be rejected
  assert.throws(
    () => {
      limiter.checkRateLimit(`delete:${uid}`, DELETION_RULES, "deleteAccountData", uid);
    },
    (err) => {
      assert.equal(err.code, "resource-exhausted");
      return true;
    },
  );
});

test("Circuit breaker defaults to open/allowed when no kill switch is set", async () => {
  resetCircuitBreakerCacheForTesting();
  const mockDb = null; // No Firestore config doc
  const state = await getCircuitBreakerState(mockDb);

  assert.equal(state.writesAllowed, true);
  assert.equal(state.deletionsAllowed, true);
  assert.equal(state.triggersAllowed, true);

  await assert.doesNotReject(requireWritesAllowed(mockDb));
  await assert.doesNotReject(requireDeletionsAllowed(mockDb));
  assert.equal(await areTriggersAllowed(mockDb), true);
});

test("Circuit breaker emergency environment kill switch blocks writes and deletions", async () => {
  resetCircuitBreakerCacheForTesting();
  process.env.DAILY_RECORD_KILL_SWITCH_WRITES = "true";

  try {
    const mockDb = null;
    await assert.rejects(
      requireWritesAllowed(mockDb),
      (err) => {
        assert.equal(err.code, "unavailable");
        return true;
      },
    );
    // Deletions are not disabled by writes kill switch
    await assert.doesNotReject(requireDeletionsAllowed(mockDb));
  } finally {
    delete process.env.DAILY_RECORD_KILL_SWITCH_WRITES;
    resetCircuitBreakerCacheForTesting();
  }
});

test("Circuit breaker global kill switch blocks all operations immediately", async () => {
  resetCircuitBreakerCacheForTesting();
  process.env.DAILY_RECORD_KILL_SWITCH_ALL = "true";

  try {
    const mockDb = null;
    await assert.rejects(
      requireWritesAllowed(mockDb),
      (err) => {
        assert.equal(err.code, "unavailable");
        return true;
      },
    );
    await assert.rejects(
      requireDeletionsAllowed(mockDb),
      (err) => {
        assert.equal(err.code, "unavailable");
        return true;
      },
    );
    assert.equal(await areTriggersAllowed(mockDb), false);
  } finally {
    delete process.env.DAILY_RECORD_KILL_SWITCH_ALL;
    resetCircuitBreakerCacheForTesting();
  }
});

test("Circuit breaker Firestore doc overrides runtime when cached", async () => {
  resetCircuitBreakerCacheForTesting();
  const mockDbWithKillSwitch = {
    collection: () => ({
      doc: () => ({
        get: async () => ({
          exists: true,
          data: () => ({ writesDisabled: true, deletionsDisabled: true, triggersDisabled: true }),
        }),
      }),
    }),
  };

  const state = await getCircuitBreakerState(mockDbWithKillSwitch);
  assert.equal(state.writesAllowed, false);
  assert.equal(state.deletionsAllowed, false);
  assert.equal(state.triggersAllowed, false);

  await assert.rejects(requireWritesAllowed(mockDbWithKillSwitch), (err) => err.code === "unavailable");
  await assert.rejects(requireDeletionsAllowed(mockDbWithKillSwitch), (err) => err.code === "unavailable");
  resetCircuitBreakerCacheForTesting();
});

test("App Check gate allows unverified requests in monitoring mode with log", () => {
  const requestWithoutAppCheck = { auth: { uid: "user-test" } };
  const verified = verifyAppCheck(requestWithoutAppCheck, "testOperation", { mode: "monitoring" });
  assert.equal(verified, false); // Returned false (unverified), but didn't throw
});

test("App Check gate rejects unverified requests in enforced mode", () => {
  const requestWithoutAppCheck = { auth: { uid: "user-test" } };
  assert.throws(
    () => {
      verifyAppCheck(requestWithoutAppCheck, "testOperation", { mode: "enforced" });
    },
    (err) => {
      assert.equal(err.code, "unauthenticated");
      return true;
    },
  );
});

test("App Check gate accepts valid attestation token in enforced mode", () => {
  const requestWithAppCheck = {
    auth: { uid: "user-test" },
    app: { appId: "io.github.litaog.dailyrecord", alreadyConsumed: false },
  };
  const verified = verifyAppCheck(requestWithAppCheck, "testOperation", { mode: "enforced" });
  assert.equal(verified, true);
});
