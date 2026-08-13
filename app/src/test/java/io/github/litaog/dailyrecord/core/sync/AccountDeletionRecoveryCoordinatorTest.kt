package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryConflictException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.auth.AuthAccount
import io.github.litaog.dailyrecord.core.auth.AuthAccountPresence
import io.github.litaog.dailyrecord.core.auth.AuthDeletionResult
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.auth.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountDeletionRecoveryCoordinatorTest {
    private lateinit var stateStore: InMemoryDeletionStateStore
    private lateinit var localStore: FakeLocalStore
    private lateinit var coordinator: AccountDeletionRecoveryCoordinator

    @Before
    fun setUp() {
        stateStore = InMemoryDeletionStateStore()
        localStore = FakeLocalStore()
        DeletionBarrier.installStateStoreForTest(stateStore)
        DeletionBarrier.endDeletionBlock()
        coordinator = AccountDeletionRecoveryCoordinator(localStore)
    }

    @After
    fun tearDown() {
        DeletionBarrier.endDeletionBlock()
    }

    @Test
    fun authPresenceStillExistsRequeuesRowsAndReleasesJournal() = runBlocking {
        moveToAuthPending()

        val conflicts = coordinator.resolvePendingAuthDeletions(
            FakeAuthRepository(AuthAccountPresence.Exists),
        )

        assertTrue(conflicts.isEmpty())
        assertTrue(localStore.pendingForResync)
        assertTrue(stateStore.readJournal().isEmpty())
    }

    @Test
    fun confirmedAuthAbsencePromotesCopyThenCleansOwnerCache() = runBlocking {
        moveToAuthPending(recoveryCopyReady = true)

        val conflicts = coordinator.resolvePendingAuthDeletions(
            FakeAuthRepository(AuthAccountPresence.Absent),
        )

        assertTrue(conflicts.isEmpty())
        assertTrue(localStore.recoveryPromoted)
        assertTrue(localStore.ownerCacheDeleted)
        assertTrue(stateStore.readJournal().isEmpty())
    }

    @Test
    fun recoveryConflictKeepsAuthPendingJournalAndSurfacesOwner() = runBlocking {
        moveToAuthPending(recoveryCopyReady = true)
        localStore.promoteFailure = AccountDeletionLocalRecoveryConflictException(
            OWNER,
            IllegalStateException("local data exists"),
        )

        val conflicts = coordinator.resolvePendingAuthDeletions(
            FakeAuthRepository(AuthAccountPresence.Absent),
        )

        assertEquals(setOf(OWNER), conflicts)
        assertEquals(
            DeletionJournalPhase.AuthDeletionPending,
            stateStore.readJournal()[OWNER]?.phase,
        )
    }

    @Test
    fun cloudDeletedBeforeAuthIsRequeuedAndReleased() = runBlocking {
        DeletionBarrier.beginDeletionBlock(OWNER)
        DeletionBarrier.markCloudDeletionComplete(OWNER)
        DeletionBarrier.endDeletionBlock(OWNER, AccountDeletionOutcome.LocalRecoveryPending)

        coordinator.resolvePreAuthUntilSettled(
            shouldContinue = { false },
            delayBlock = {},
        )

        assertTrue(localStore.pendingForResync)
        assertTrue(stateStore.readJournal().isEmpty())
    }

    private suspend fun moveToAuthPending(recoveryCopyReady: Boolean = false) {
        DeletionBarrier.beginDeletionBlock(OWNER)
        DeletionBarrier.markCloudDeletionComplete(OWNER)
        if (recoveryCopyReady) {
            DeletionBarrier.markLocalRecoveryCopyPending(OWNER)
            DeletionBarrier.markLocalRecoveryCopyReady(OWNER)
        }
        DeletionBarrier.markAuthDeletionStarted(OWNER)
        DeletionBarrier.endDeletionBlock(OWNER, AccountDeletionOutcome.AuthDeletionPending)
    }

    private companion object {
        const val OWNER = "owner-a"
    }
}

private class FakeAuthRepository(
    private val presence: AuthAccountPresence,
) : AuthRepository {
    override val state: Flow<AuthState> = emptyFlow()

    override suspend fun signIn(email: String, password: String) = AuthAccount(OWNER, email)

    override suspend fun register(email: String, password: String) = AuthAccount(OWNER, email)

    override suspend fun sendPasswordResetEmail(email: String) = Unit

    override suspend fun reauthenticate(password: String) = Unit

    override suspend fun deleteCurrentAccount(): AuthDeletionResult = AuthDeletionResult.Completed

    override suspend fun inspectAccountPresence(ownerId: String): AuthAccountPresence = presence

    override fun signOut() = Unit

    private companion object {
        const val OWNER = "owner-a"
    }
}

private class FakeLocalStore : AccountDeletionLocalStore {
    var pendingForResync = false
    var recoveryPromoted = false
    var ownerCacheDeleted = false
    var promoteFailure: Exception? = null

    override suspend fun stageLocalRecoveryCopy(ownerId: String) = Unit

    override suspend fun discardLocalRecoveryCopy(ownerId: String) = Unit

    override suspend fun promoteLocalRecoveryCopy(ownerId: String) {
        promoteFailure?.let { throw it }
        recoveryPromoted = true
    }

    override suspend fun hasLocalRecoveryConflict(ownerId: String): Boolean = false

    override suspend fun deleteOwnerCache(ownerId: String) {
        ownerCacheDeleted = true
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        pendingForResync = true
    }
}
