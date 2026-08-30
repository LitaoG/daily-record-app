package io.github.litaog.dailyrecord.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SyncModelsIdTest {
    @Test
    fun localCopyIdRoundTripRestoresSourceId() {
        val id = "2026-08-16"
        assertEquals(id, localCopySourceId(localCopyId(id)))
        assertEquals("__local__-copy-2026-08-16", localCopyId(id))
    }

    @Test
    fun recoveryOwnerIdNeverCollidesWithRealOwnerIds() {
        val owner = "abc123"
        assertEquals("__recovery__:abc123", recoveryOwnerId(owner))
        assertNotEquals(recoveryOwnerId(owner), localCopyId(owner))
        assertNotEquals(recoveryOwnerId(owner), owner)
    }

    @Test
    fun localCopySourceIdIsNonDestructiveForPlainIds() {
        assertEquals("2026-08-16", localCopySourceId("2026-08-16"))
        assertEquals("__local__-copy-x", localCopySourceId("__local__-copy-__local__-copy-x"))
    }
}