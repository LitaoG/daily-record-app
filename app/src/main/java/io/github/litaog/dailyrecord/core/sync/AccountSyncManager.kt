package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                    mutableStatus.value = if (networkAvailable.value) {
                        SyncStatus.Failed(error.userMessage())
                    } else {
                        SyncStatus.Offline
                    }
                    networkAvailable.first { it }
                    delay(remoteRetryDelayMillis(attempt))
                    true
                }
                .collectLatest { snapshot ->
                    coordinator.applySnapshot(ownerId, snapshot)
                    if (!snapshot.fromCache && networkAvailable.value) updateIdleStatus()
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
            mutableStatus.value = SyncStatus.Syncing
            runCatching { coordinator.syncOnce(ownerId) }
                .onSuccess { result ->
                    mutableStatus.value = if (result.pending == 0) {
                        SyncStatus.UpToDate
                    } else {
                        SyncStatus.Pending(result.pending)
                    }
                }
                .onFailure {
                    mutableStatus.value = if (networkAvailable.value) {
                        SyncStatus.Failed(it.userMessage())
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

private fun Throwable.userMessage(): String = when {
    generateSequence(this) { it.cause }.any { it is com.google.firebase.FirebaseNetworkException } ->
        "网络连接不稳定，记录已保存在本机"
    generateSequence(this) { it.cause }.any { it is com.google.firebase.auth.FirebaseAuthException } ->
        "账号权限已失效，请退出后重新登录"
    else -> "暂时无法同步，记录已保存在本机"
}
