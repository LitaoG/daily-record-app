package io.github.litaog.dailyrecord.core.account

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedAccountDeletionLocalStoreUnitTest {
    @Test
    fun accountLifecycleCoversAnyNumberOfLocalStores() = runBlocking {
        val calls = mutableListOf<String>()
        val store = CombinedAccountDeletionLocalStore(
            stores = listOf(
                RecordingLocalStore("hand-brew", calls),
                RecordingLocalStore("sex", calls),
                RecordingLocalStore("future-module", calls),
            ),
        )

        store.stageLocalRecoveryCopy("owner")
        store.deleteOwnerCache("owner")
        store.discardLocalRecoveryCopy("owner")
        store.promoteLocalRecoveryCopy("owner")
        store.markOwnerPendingForResync("owner")

        assertEquals(
            listOf(
                "stage:hand-brew",
                "stage:sex",
                "stage:future-module",
                "delete:hand-brew",
                "delete:sex",
                "delete:future-module",
                "discard:hand-brew",
                "discard:sex",
                "discard:future-module",
                "promote:hand-brew",
                "promote:sex",
                "promote:future-module",
                "mark-pending:hand-brew",
                "mark-pending:sex",
                "mark-pending:future-module",
            ),
            calls,
        )
    }

    @Test
    fun failedStagingRollsBackEveryStore() = runBlocking {
        val calls = mutableListOf<String>()
        val store = CombinedAccountDeletionLocalStore(
            stores = listOf(
                RecordingLocalStore("first", calls),
                RecordingLocalStore(
                    "second",
                    calls,
                    stageFailure = IllegalStateException("stage failed"),
                ),
                RecordingLocalStore("third", calls),
            ),
        )

        val failure = runCatching { store.stageLocalRecoveryCopy("owner") }.exceptionOrNull()

        assertEquals("stage failed", failure?.message)
        assertEquals(
            listOf(
                "stage:first",
                "stage:second",
                "discard:first",
                "discard:second",
                "discard:third",
            ),
            calls,
        )
    }
}

private class RecordingLocalStore(
    private val name: String,
    private val calls: MutableList<String>,
    private val stageFailure: Exception? = null,
) : AccountDeletionLocalStore {
    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        calls += "stage:$name"
        stageFailure?.let { throw it }
    }

    override suspend fun discardLocalRecoveryCopy(ownerId: String) {
        calls += "discard:$name"
    }

    override suspend fun promoteLocalRecoveryCopy(ownerId: String) {
        calls += "promote:$name"
    }

    override suspend fun hasLocalRecoveryConflict(ownerId: String): Boolean = false


    override suspend fun deleteOwnerCache(ownerId: String) {
        calls += "delete:$name"
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        calls += "mark-pending:$name"
    }
}
