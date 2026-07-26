package io.github.litaog.dailyrecord.ui.account

import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFailurePresentationTest {
    @Test
    fun everyFailureKindHasDistinctActionableCopy() {
        val presentations = SyncFailureKind.entries.map { it.presentation() }

        assertEquals(SyncFailureKind.entries.size, presentations.map { it.title }.distinct().size)
        assertTrue(presentations.all { it.guidance.isNotBlank() })
        assertTrue(presentations.all { it.actionLabel.isNotBlank() })
    }

    @Test
    fun accountFailuresRequireReauthentication() {
        assertEquals(
            SyncFailureAction.Reauthenticate,
            SyncFailureKind.Authentication.presentation().action,
        )
        assertEquals(
            SyncFailureAction.Reauthenticate,
            SyncFailureKind.Permission.presentation().action,
        )
    }

    @Test
    fun recoverableFailuresOfferRetry() {
        val retryableKinds = setOf(
            SyncFailureKind.Network,
            SyncFailureKind.Quota,
            SyncFailureKind.Service,
            SyncFailureKind.Data,
            SyncFailureKind.Unknown,
        )

        assertTrue(
            retryableKinds.all {
                it.presentation().action == SyncFailureAction.Retry
            },
        )
    }
}
