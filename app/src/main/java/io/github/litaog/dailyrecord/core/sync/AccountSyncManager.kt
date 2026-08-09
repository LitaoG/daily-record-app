package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.litaog.dailyrecord.core.cloud.INTERACTIVE_CLOUD_TIMEOUT_MILLIS
import io.github.litaog.dailyrecord.core.cloud.InteractiveCloudTimeoutException
import io.github.litaog.dailyrecord.core.common.AppCopy
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout

internal class AccountSyncManager(
    private val ownerId: String,
    private val coordinator: AccountSyncOperations,
    private val productionConfigured: Boolean,
    private val networkAvailable: StateFlow<Boolean> = MutableStateFlow(true),
    private val remoteRetryDelayMillis: (Long) -> Long = ::remoteRetryDelayMillis,
    private val syncAttemptTimeoutMillis: Long = INTERACTIVE_CLOUD_TIMEOUT_MILLIS,
) {
    private val mutex = Mutex()
    private val followUpSyncRequested = AtomicBoolean(false)
    private val mutableStatus = MutableStateFlow<SyncStatus>(
        if (productionConfigured) SyncStatus.Syncing else SyncStatus.NotConfigured,
    )
    val status: StateFlow<SyncStatus> = mutableStatus

    fun start(scope: CoroutineScope): List<Job> {
        if (!productionConfigured) return emptyList()
        val remoteJob = scope.launch {
            coordinator.observeRemote(ownerId)
                .onEach { snapshot ->
                    coordinator.applySnapshot(ownerId, snapshot)
                    if (snapshot.rejectedRecordCount > 0) {
                        publishStatus(malformedRemoteRecordsFailure())
                    }
                    if (!snapshot.fromCache && networkAvailable.value) {
                        if (coordinator.pendingCount(ownerId) > 0) {
                            // A fresh server snapshot also proves Firebase is reachable. This
                            // catches VPN/proxy recovery even when Android's network state did
                            // not change and flushes edits that remained safely in Room.
                            syncNow(queueIfBusy = true)
                        } else if (snapshot.rejectedRecordCount == 0) {
                            updateIdleStatus()
                        }
                    }
                }
                .retryWhen { error, attempt ->
                    val retryable = error.isRetryableRemoteObservation()
                    publishStatus(if (networkAvailable.value) {
                        error.toSyncFailure()
                    } else {
                        SyncStatus.Offline
                    })
                    if (!retryable) return@retryWhen false
                    // Auth failures are transient (token refresh); wait for the
                    // network and re-subscribe instead of letting the realtime
                    // channel die permanently.
                    networkAvailable.first { it }
                    delay(remoteRetryDelayMillis(attempt))
                    true
                }
                .collect()
        }
        val pendingJob = scope.launch {
            coordinator.observePendingCount(ownerId).collectLatest { count ->
                if (
                    networkAvailable.value &&
                    mutableStatus.value !is SyncStatus.Syncing &&
                    mutableStatus.value !is SyncStatus.Failed
                ) {
                    publishStatus(if (count == 0) SyncStatus.UpToDate else SyncStatus.Pending(count))
                }
            }
        }
        val networkJob = scope.launch {
            networkAvailable.collectLatest { available ->
                if (available) syncNow(queueIfBusy = true) else publishStatus(SyncStatus.Offline)
            }
        }
        return listOf(remoteJob, pendingJob, networkJob)
    }

    suspend fun syncNow() = syncNow(queueIfBusy = false)

    private suspend fun syncNow(queueIfBusy: Boolean) {
        if (!productionConfigured) {
            publishStatus(SyncStatus.NotConfigured)
            return
        }
        if (!networkAvailable.value) {
            publishStatus(SyncStatus.Offline)
            return
        }
        if (!mutex.tryLock()) {
            if (queueIfBusy) followUpSyncRequested.set(true)
            return
        }
        try {
            do {
                followUpSyncRequested.set(false)
                performSyncAttempt()
            } while (
                productionConfigured &&
                networkAvailable.value &&
                followUpSyncRequested.getAndSet(false)
            )
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun performSyncAttempt() {
        val previousStatus = mutableStatus.value
        try {
            withTimeout(syncAttemptTimeoutMillis) {
                publishStatus(SyncStatus.Syncing)
                val result = coordinator.syncOnce(ownerId)
                publishStatus(if (result.rejectedRemoteRecords > 0) {
                    malformedRemoteRecordsFailure()
                } else if (result.pending == 0) {
                    SyncStatus.UpToDate
                } else {
                    SyncStatus.Pending(result.pending)
                })
            }
        } catch (error: TimeoutCancellationException) {
            publishStatus(if (networkAvailable.value) {
                InteractiveCloudTimeoutException(error).toSyncFailure()
            } else {
                SyncStatus.Offline
            })
        } catch (error: CancellationException) {
            publishStatus(previousStatus)
            throw error
        } catch (error: Exception) {
            publishStatus(if (networkAvailable.value) {
                error.toSyncFailure()
            } else {
                SyncStatus.Offline
            })
        }
    }

    private suspend fun updateIdleStatus() {
        val pending = coordinator.pendingCount(ownerId)
        publishStatus(if (pending == 0) SyncStatus.UpToDate else SyncStatus.Pending(pending))
    }

    private fun publishStatus(status: SyncStatus) {
        mutableStatus.value = status
    }
}

/**
 * The narrow coordinator surface needed by the account lifecycle.
 *
 * Keeping this boundary smaller than a module coordinator makes status and concurrency
 * behavior independently testable without coupling the account lifecycle to one record type.
 */
internal interface AccountSyncOperations {
    fun observeRemote(ownerId: String): Flow<RemoteSnapshot>

    fun observePendingCount(ownerId: String): Flow<Int>

    suspend fun pendingCount(ownerId: String): Int

    suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int

    suspend fun syncOnce(ownerId: String): SyncResult
}

private fun malformedRemoteRecordsFailure() = SyncStatus.Failed(
    message = AppCopy.Account.dataFormatFailure,
    kind = SyncFailureKind.Data,
)

private fun remoteRetryDelayMillis(attempt: Long): Long {
    val exponent = attempt.coerceAtMost(5).toInt()
    return (1_000L shl exponent).coerceAtMost(30_000L)
}

internal fun Throwable.isRetryableRemoteObservation(): Boolean =
    generateSequence(this) { it.cause }.any { cause ->
        // FirebaseAuthException and UNAUTHENTICATED cover transient token
        // refresh failures: the observer must re-subscribe after auth recovers
        // instead of terminating the realtime channel permanently.
        cause is FirebaseAuthException ||
            cause is FirebaseNetworkException ||
            cause is IOException ||
            cause is FirebaseFirestoreException && cause.code in setOf(
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.CANCELLED,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.INTERNAL,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.UNKNOWN,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
            )
    }

internal fun Throwable.isNetworkRelatedSyncFailure(): Boolean =
    syncFailureKind() == SyncFailureKind.Network

internal fun Throwable.syncFailureKind(): SyncFailureKind {
    val causes = generateSequence(this) { it.cause }.toList()
    causes.filterIsInstance<FirebaseAuthException>().firstOrNull()?.let { error ->
        return syncFailureKindForFirebaseAuthCode(error.errorCode)
    }

    causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()?.let { error ->
        return syncFailureKindForFirestoreCode(error.code.value())
    }

    return when {
        causes.any { it is FirebaseNetworkException || it is IOException } ->
            SyncFailureKind.Network
        causes.any { it is IllegalArgumentException } ->
            SyncFailureKind.Data
        else ->
            SyncFailureKind.Unknown
    }
}

internal fun syncFailureKindForFirebaseAuthCode(code: String): SyncFailureKind = when (code) {
    "ERROR_NETWORK_REQUEST_FAILED" -> SyncFailureKind.Network
    "ERROR_TOO_MANY_REQUESTS" -> SyncFailureKind.Quota
    else -> SyncFailureKind.Authentication
}

/**
 * Maps Firestore's public gRPC-compatible numeric status codes without requiring Android
 * framework classes in local unit tests.
 */
internal fun syncFailureKindForFirestoreCode(code: Int): SyncFailureKind = when (code) {
    16 -> SyncFailureKind.Authentication // UNAUTHENTICATED
    7 -> SyncFailureKind.Permission // PERMISSION_DENIED
    8 -> SyncFailureKind.Quota // RESOURCE_EXHAUSTED
    4, 14 -> SyncFailureKind.Network // DEADLINE_EXCEEDED, UNAVAILABLE
    1, 10, 13 -> SyncFailureKind.Service // CANCELLED, ABORTED, INTERNAL
    3, 5, 6, 9, 11, 12, 15 -> SyncFailureKind.Data
    else -> SyncFailureKind.Unknown
}

private fun Throwable.toSyncFailure(): SyncStatus.Failed {
    val kind = syncFailureKind()
    val message = when (kind) {
        SyncFailureKind.Network -> if (this is InteractiveCloudTimeoutException) {
            AppCopy.Account.timeoutFailure
        } else {
            AppCopy.Account.networkFailure
        }
        SyncFailureKind.Authentication -> AppCopy.Account.authFailure
        SyncFailureKind.Permission -> AppCopy.Account.permissionFailure
        SyncFailureKind.Quota -> AppCopy.Account.quotaFailure
        SyncFailureKind.Service -> AppCopy.Account.serviceFailure
        SyncFailureKind.Data -> AppCopy.Account.dataFailure
        SyncFailureKind.Unknown -> AppCopy.Account.unknownFailure
    }
    return SyncStatus.Failed(message = message, kind = kind)
}
