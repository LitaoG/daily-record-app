package io.github.litaog.dailyrecord.core.cloud

import com.google.firebase.FirebaseNetworkException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

internal const val INTERACTIVE_CLOUD_TIMEOUT_MILLIS = 5_000L

internal class InteractiveCloudTimeoutException(
    cause: Throwable? = null,
) : IOException("Interactive cloud operation timed out", cause)

internal suspend fun <T> runInteractiveCloudOperation(
    timeoutMillis: Long = INTERACTIVE_CLOUD_TIMEOUT_MILLIS,
    operation: suspend () -> T,
): Result<T> = try {
    Result.success(withTimeout(timeoutMillis) { operation() })
} catch (error: TimeoutCancellationException) {
    Result.failure(InteractiveCloudTimeoutException(error))
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}

internal fun Throwable.isNetworkReachabilityFailure(): Boolean =
    generateSequence(this) { it.cause }.any { cause ->
        cause is FirebaseNetworkException || cause is IOException
    }
