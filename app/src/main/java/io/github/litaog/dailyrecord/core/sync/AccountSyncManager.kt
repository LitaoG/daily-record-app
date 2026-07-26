package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AccountSyncManager(
    private val ownerId: String,
    private val coordinator: HandBrewSyncCoordinator,
    private val productionConfigured: Boolean,
    private val networkAvailable: StateFlow<Boolean> = MutableStateFlow(true),
    private val remoteRetryDelayMillis: (Long) -> Long = ::remoteRetryDelayMillis,
) {
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow<SyncStatus>(
        if (productionConfigured) SyncStatus.Syncing else SyncStatus.NotConfigured,
    )
    private val mutableDiagnostics = MutableStateFlow(SyncDiagnostics())
    val status: StateFlow<SyncStatus> = mutableStatus
    val diagnostics: StateFlow<SyncDiagnostics> = mutableDiagnostics

    fun start(scope: CoroutineScope): List<Job> {
        if (!productionConfigured) return emptyList()
        val remoteJob = scope.launch {
            coordinator.observeRemote(ownerId)
                .retryWhen { error, attempt ->
                    val retryable = error.isRetryableRemoteObservation()
                    publishStatus(if (networkAvailable.value) {
                        error.toSyncFailure()
                    } else {
                        SyncStatus.Offline
                    })
                    if (!retryable) return@retryWhen false
                    networkAvailable.first { it }
                    delay(remoteRetryDelayMillis(attempt))
                    true
                }
                .collect { snapshot ->
                    coordinator.applySnapshot(ownerId, snapshot)
                    if (snapshot.rejectedRecordCount > 0) {
                        publishStatus(malformedRemoteRecordsFailure())
                    }
                    if (!snapshot.fromCache && networkAvailable.value) {
                        if (coordinator.pendingCount(ownerId) > 0) {
                            // A fresh server snapshot also proves Firebase is reachable. This
                            // catches VPN/proxy recovery even when Android's network state did
                            // not change and flushes edits that remained safely in Room.
                            syncNow()
                        } else if (snapshot.rejectedRecordCount == 0) {
                            updateIdleStatus()
                        }
                    }
                }
        }
        val pendingJob = scope.launch {
            coordinator.observePendingCount(ownerId).collectLatest { count ->
                updatePendingDiagnostics(count)
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
                if (available) syncNow() else publishStatus(SyncStatus.Offline)
            }
        }
        return listOf(remoteJob, pendingJob, networkJob)
    }

    suspend fun syncNow() {
        if (!productionConfigured) {
            publishStatus(SyncStatus.NotConfigured)
            return
        }
        if (!networkAvailable.value) {
            publishStatus(SyncStatus.Offline)
            return
        }
        mutex.withLock {
            val previousStatus = mutableStatus.value
            publishStatus(SyncStatus.Syncing)
            try {
                val result = coordinator.syncOnce(ownerId)
                updatePendingDiagnostics(result.pending)
                publishStatus(if (result.rejectedRemoteRecords > 0) {
                    malformedRemoteRecordsFailure()
                } else if (result.pending == 0) {
                    SyncStatus.UpToDate
                } else {
                    SyncStatus.Pending(result.pending)
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
    }

    private suspend fun updateIdleStatus() {
        val pending = coordinator.pendingCount(ownerId)
        updatePendingDiagnostics(pending)
        publishStatus(if (pending == 0) SyncStatus.UpToDate else SyncStatus.Pending(pending))
    }

    private fun updatePendingDiagnostics(count: Int) {
        mutableDiagnostics.value = mutableDiagnostics.value.copy(
            hasPendingRecords = count > 0,
        )
    }

    private fun publishStatus(status: SyncStatus) {
        mutableStatus.value = status
        if (status is SyncStatus.Failed) {
            mutableDiagnostics.value = mutableDiagnostics.value.copy(
                latestFailureKind = status.kind,
            )
        }
    }
}

private fun malformedRemoteRecordsFailure() = SyncStatus.Failed(
    message = "部分云端记录格式异常，其余记录已继续同步",
    kind = SyncFailureKind.Data,
)

private fun remoteRetryDelayMillis(attempt: Long): Long {
    val exponent = attempt.coerceAtMost(5).toInt()
    return (1_000L shl exponent).coerceAtMost(30_000L)
}

internal fun Throwable.isRetryableRemoteObservation(): Boolean =
    generateSequence(this) { it.cause }.any { cause ->
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
            )
    }

internal fun Throwable.isNetworkRelatedSyncFailure(): Boolean =
    syncFailureKind() == SyncFailureKind.Network

internal fun Throwable.syncFailureKind(): SyncFailureKind {
    val causes = generateSequence(this) { it.cause }.toList()
    if (causes.any { it is FirebaseAuthException }) return SyncFailureKind.Authentication

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
        SyncFailureKind.Network -> "网络连接异常，记录已保存在本机"
        SyncFailureKind.Authentication -> "登录状态已失效，记录已保存在本机"
        SyncFailureKind.Permission -> "账号暂无云端访问权限，记录已保存在本机"
        SyncFailureKind.Quota -> "云服务额度暂时受限，记录已保存在本机"
        SyncFailureKind.Service -> "云服务暂时不可用，记录已保存在本机"
        SyncFailureKind.Data -> "部分记录暂时无法同步，原始记录已保存在本机"
        SyncFailureKind.Unknown -> "暂时无法同步，记录已保存在本机"
    }
    return SyncStatus.Failed(message = message, kind = kind)
}
