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
    val status: StateFlow<SyncStatus> = mutableStatus

    fun start(scope: CoroutineScope): List<Job> {
        if (!productionConfigured) return emptyList()
        val remoteJob = scope.launch {
            coordinator.observeRemote(ownerId)
                .retryWhen { error, attempt ->
                    val retryable = error.isRetryableRemoteObservation()
                    mutableStatus.value = if (networkAvailable.value) {
                        SyncStatus.Failed(error.userMessage())
                    } else {
                        SyncStatus.Offline
                    }
                    if (!retryable) return@retryWhen false
                    networkAvailable.first { it }
                    delay(remoteRetryDelayMillis(attempt))
                    true
                }
                .collect { snapshot ->
                    coordinator.applySnapshot(ownerId, snapshot)
                    if (!snapshot.fromCache && networkAvailable.value) {
                        if (coordinator.pendingCount(ownerId) > 0) {
                            // A fresh server snapshot also proves Firebase is reachable. This
                            // catches VPN/proxy recovery even when Android's network state did
                            // not change and flushes edits that remained safely in Room.
                            syncNow()
                        } else {
                            updateIdleStatus()
                        }
                    }
                }
        }
        val pendingJob = scope.launch {
            coordinator.observePendingCount(ownerId).collectLatest { count ->
                if (networkAvailable.value && mutableStatus.value !is SyncStatus.Syncing) {
                    mutableStatus.value = if (count == 0) SyncStatus.UpToDate else SyncStatus.Pending(count)
                }
            }
        }
        val networkJob = scope.launch {
            networkAvailable.collectLatest { available ->
                if (available) syncNow() else mutableStatus.value = SyncStatus.Offline
            }
        }
        return listOf(remoteJob, pendingJob, networkJob)
    }

    suspend fun syncNow() {
        if (!productionConfigured) {
            mutableStatus.value = SyncStatus.NotConfigured
            return
        }
        if (!networkAvailable.value) {
            mutableStatus.value = SyncStatus.Offline
            return
        }
        mutex.withLock {
            val previousStatus = mutableStatus.value
            mutableStatus.value = SyncStatus.Syncing
            try {
                val result = coordinator.syncOnce(ownerId)
                mutableStatus.value = if (result.pending == 0) {
                    SyncStatus.UpToDate
                } else {
                    SyncStatus.Pending(result.pending)
                }
            } catch (error: CancellationException) {
                mutableStatus.value = previousStatus
                throw error
            } catch (error: Exception) {
                mutableStatus.value = if (networkAvailable.value) {
                    SyncStatus.Failed(error.userMessage())
                } else {
                    SyncStatus.Offline
                }
            }
        }
    }

    private suspend fun updateIdleStatus() {
        val pending = coordinator.pendingCount(ownerId)
        mutableStatus.value = if (pending == 0) SyncStatus.UpToDate else SyncStatus.Pending(pending)
    }
}

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

private fun Throwable.userMessage(): String = when {
    generateSequence(this) { it.cause }.any { it is FirebaseNetworkException } ->
        "网络连接不稳定，记录已保存在本机"
    generateSequence(this) { it.cause }.any {
        it is FirebaseAuthException ||
            it is FirebaseFirestoreException && it.code in setOf(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
            )
    } ->
        "账号权限已失效，请退出后重新登录"
    else -> "暂时无法同步，记录已保存在本机"
}
