package io.github.litaog.dailyrecord.core.cloud

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskAwaitTest {
    @Test
    fun cancelledAwaitDoesNotResumeWhenTaskCompletesLater() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val task = PendingTask<String>()
            val suspended = CompletableDeferred<Unit>()
            var cancelledObserved = false
            val job = scope.launch {
                suspended.complete(Unit)
                try {
                    task.awaitResult()
                    fail("awaitResult should be cancelled")
                } catch (_: CancellationException) {
                    cancelledObserved = true
                }
            }
            // Wait until the coroutine is suspended inside awaitResult before
            // cancelling, otherwise the launch block may be skipped entirely.
            withTimeout(5_000L) { suspended.await() }
            job.cancel()
            job.join()
            assertTrue(cancelledObserved)

            // Task completes after cancellation: the listener observes the
            // cleared continuation and does nothing (no resume, no crash).
            task.complete("late-result")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun successfulTaskResumesWithResult() = runBlocking {
        val task = PendingTask<String>()
        // Complete synchronously before awaiting: awaitResult must register its
        // listener and still receive the already-complete task result.
        task.complete("ok")
        val result = withTimeout(5_000L) { task.awaitResult() }
        assertTrue(result == "ok")
    }

    @Test
    fun failedTaskResumesWithException() = runBlocking {
        val task = PendingTask<String>()
        task.fail(IllegalStateException("boom"))
        val thrown = try {
            withTimeout(5_000L) { task.awaitResult() }
            null
        } catch (error: IllegalStateException) {
            error
        }
        assertTrue(thrown is IllegalStateException)
    }

    private class PendingTask<T> : Task<T>() {
        private val listeners = mutableListOf<OnCompleteListener<T>>()
        private var completed: T? = null
        private var error: Exception? = null

        override fun isComplete() = error != null || completed != null

        override fun isSuccessful() = error == null && completed != null

        override fun isCanceled() = false

        override fun getResult(): T = completed ?: throw error ?: IllegalStateException("not complete")

        override fun <X : Throwable> getResult(exClass: Class<X>): T =
            completed ?: throw error ?: IllegalStateException("not complete")

        override fun getException(): Exception? = error

        override fun addOnCompleteListener(listener: OnCompleteListener<T>): Task<T> {
            if (isComplete()) {
                listener.onComplete(this)
            } else {
                listeners += listener
            }
            return this
        }

        override fun addOnCompleteListener(executor: Executor, listener: OnCompleteListener<T>): Task<T> {
            if (isComplete()) {
                listener.onComplete(this)
            } else {
                listeners += listener
            }
            return this
        }

        override fun addOnCompleteListener(activity: Activity, listener: OnCompleteListener<T>): Task<T> {
            if (isComplete()) {
                listener.onComplete(this)
            } else {
                listeners += listener
            }
            return this
        }

        override fun addOnSuccessListener(listener: OnSuccessListener<in T>): Task<T> = this

        override fun addOnSuccessListener(executor: Executor, listener: OnSuccessListener<in T>): Task<T> = this

        override fun addOnSuccessListener(activity: Activity, listener: OnSuccessListener<in T>): Task<T> = this

        override fun addOnFailureListener(listener: OnFailureListener): Task<T> = this

        override fun addOnFailureListener(executor: Executor, listener: OnFailureListener): Task<T> = this

        override fun addOnFailureListener(activity: Activity, listener: OnFailureListener): Task<T> = this

        override fun addOnCanceledListener(listener: OnCanceledListener): Task<T> = this

        override fun addOnCanceledListener(executor: Executor, listener: OnCanceledListener): Task<T> = this

        override fun addOnCanceledListener(activity: Activity, listener: OnCanceledListener): Task<T> = this

        override fun <TContinuationResult> continueWith(continuation: com.google.android.gms.tasks.Continuation<T, TContinuationResult>): Task<TContinuationResult> =
            error("not used")

        override fun <TContinuationResult> continueWith(executor: Executor, continuation: com.google.android.gms.tasks.Continuation<T, TContinuationResult>): Task<TContinuationResult> =
            error("not used")

        override fun <TContinuationResult> continueWithTask(continuation: com.google.android.gms.tasks.Continuation<T, Task<TContinuationResult>>): Task<TContinuationResult> =
            error("not used")

        override fun <TContinuationResult> continueWithTask(executor: Executor, continuation: com.google.android.gms.tasks.Continuation<T, Task<TContinuationResult>>): Task<TContinuationResult> =
            error("not used")

        override fun <TContinuationResult> onSuccessTask(continuation: SuccessContinuation<T, TContinuationResult>): Task<TContinuationResult> =
            error("not used")

        override fun <TContinuationResult> onSuccessTask(executor: Executor, continuation: SuccessContinuation<T, TContinuationResult>): Task<TContinuationResult> =
            error("not used")

        fun complete(value: T) {
            completed = value
            listeners.toList().forEach { it.onComplete(this) }
        }

        fun fail(exception: Exception) {
            error = exception
            listeners.toList().forEach { it.onComplete(this) }
        }
    }
}
