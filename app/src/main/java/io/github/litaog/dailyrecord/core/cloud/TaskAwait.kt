package io.github.litaog.dailyrecord.core.cloud

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    // Play Services Tasks 18.x has no removeOnCompleteListener API, so the
    // listener cannot be unregistered on cancellation. Instead the listener
    // holds a nullable reference to the continuation and clears it in
    // invokeOnCancellation: after cancellation the Task may still invoke the
    // listener, but it no longer retains the coroutine continuation (and its
    // captured context), so repeated cancelled cloud operations do not leak.
    val holder = TaskContinuationHolder<T>(continuation)
    val listener = OnCompleteListener<T> { task ->
        val active = holder.continuation
        if (active == null || !active.isActive) return@OnCompleteListener
        val error = task.exception
        if (task.isSuccessful) {
            active.resume(task.result)
        } else {
            active.resumeWithException(error ?: IllegalStateException("Firebase task failed"))
        }
    }
    continuation.invokeOnCancellation { holder.clear() }
    addOnCompleteListener(listener)
}

private class TaskContinuationHolder<T>(
    continuation: kotlinx.coroutines.CancellableContinuation<T>,
) {
    @Volatile
    var continuation: kotlinx.coroutines.CancellableContinuation<T>? = continuation

    fun clear() {
        continuation = null
    }
}
