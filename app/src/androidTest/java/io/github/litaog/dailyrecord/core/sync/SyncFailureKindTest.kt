package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.functions.FirebaseFunctionsException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Device coverage for the Cloud Functions status table. The Code enum touches
 * Android-only static state (SparseArray), so this lives in androidTest; the
 * JVM-safe ClassifiedSyncException integration is covered by unit tests.
 * Instances are built reflectively because the public SDK constructors are
 * package-private.
 */
class SyncFailureKindTest {
    @Test
    fun functionsTransportCodesAreNetwork() {
        assertEquals(
            SyncFailureKind.Network,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.UNAVAILABLE),
        )
        assertEquals(
            SyncFailureKind.Network,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.DEADLINE_EXCEEDED),
        )
    }

    @Test
    fun functionsIdentityCodesKeepTheirKinds() {
        assertEquals(
            SyncFailureKind.Authentication,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.UNAUTHENTICATED),
        )
        assertEquals(
            SyncFailureKind.Permission,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.PERMISSION_DENIED),
        )
        assertEquals(
            SyncFailureKind.Quota,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED),
        )
    }

    @Test
    fun functionsServerCodesAreService() {
        assertEquals(
            SyncFailureKind.Service,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.INTERNAL),
        )
        assertEquals(
            SyncFailureKind.Service,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.ABORTED),
        )
        assertEquals(
            SyncFailureKind.Service,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.CANCELLED),
        )
    }

    @Test
    fun functionsRequestCodesAreData() {
        assertEquals(
            SyncFailureKind.Data,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.INVALID_ARGUMENT),
        )
    }

    @Test
    fun missingFunctionEndpointIsService() {
        assertEquals(
            SyncFailureKind.Service,
            syncFailureKindForFunctionsCode(FirebaseFunctionsException.Code.NOT_FOUND),
        )
    }

    @Test
    fun rawFunctionsExceptionsReachTheClassifierThroughCommitTranslation() {
        // The shared classifier only honors ClassifiedSyncException; the raw
        // SDK type is translated at the commit boundary (see commit()).
        val translated = try {
            throw functionsError(FirebaseFunctionsException.Code.PERMISSION_DENIED)
        } catch (error: FirebaseFunctionsException) {
            ClassifiedSyncException(syncFailureKindForFunctionsCode(error.code), error)
        }

        assertEquals(SyncFailureKind.Permission, translated.syncFailureKind())
        assertFalse(translated.isNetworkRelatedSyncFailure())
    }

    @Test
    fun translatedTransportFailuresKeepVpnGuidance() {
        val translated = try {
            throw functionsError(FirebaseFunctionsException.Code.UNAVAILABLE)
        } catch (error: FirebaseFunctionsException) {
            ClassifiedSyncException(syncFailureKindForFunctionsCode(error.code), error)
        }

        assertEquals(SyncFailureKind.Network, translated.syncFailureKind())
        assertTrue(translated.isNetworkRelatedSyncFailure())
    }

    private fun functionsError(code: FirebaseFunctionsException.Code): FirebaseFunctionsException {
        // The public SDK constructors are package-private; reflection keeps
        // this device test independent of SDK internals beyond the signature.
        // Neither details nor cause may be null.
        val constructor = FirebaseFunctionsException::class.java.getDeclaredConstructor(
            String::class.java,
            FirebaseFunctionsException.Code::class.java,
            Any::class.java,
            Throwable::class.java,
        )
        constructor.isAccessible = true
        return constructor.newInstance(
            "test failure",
            code,
            emptyMap<String, Any>(),
            RuntimeException("test cause"),
        ) as FirebaseFunctionsException
    }
}
