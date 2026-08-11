package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.account.AccountRemoteDataStore
import io.github.litaog.dailyrecord.core.account.CombinedAccountRemoteDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedAccountRemoteDataStoreTest {
    @Test
    fun permanentAccountDeletionCoversEveryCloudCollection() = runBlocking {
        val calls = mutableListOf<String>()
        val store = CombinedAccountRemoteDataStore(
            stores = listOf(
                DeleteOnlyRemote("hand-brew", calls),
                DeleteOnlyRemote("sex", calls),
                DeleteOnlyRemote("future-module", calls),
            ),
        )

        store.deleteAll("owner")

        assertEquals(listOf("hand-brew", "sex", "future-module"), calls)
    }

    @Test
    fun deletionStillAttemptsLaterCollectionsAfterOneFails() = runBlocking {
        val calls = mutableListOf<String>()
        val store = CombinedAccountRemoteDataStore(
            stores = listOf(
                DeleteOnlyRemote("first", calls, failure = IllegalStateException("first failed")),
                DeleteOnlyRemote("second", calls),
                DeleteOnlyRemote("third", calls, failure = IllegalArgumentException("third failed")),
            ),
        )

        val failure = runCatching { store.deleteAll("owner") }.exceptionOrNull()

        assertEquals(listOf("first", "second", "third"), calls)
        assertEquals("first failed", failure?.message)
        assertEquals(listOf("third failed"), failure?.suppressed?.map { it.message })
    }
}

private class DeleteOnlyRemote(
    private val name: String,
    private val calls: MutableList<String>,
    private val failure: Exception? = null,
) : AccountRemoteDataStore {
    override suspend fun deleteAll(ownerId: String) {
        calls += name
        failure?.let { throw it }
    }
}
