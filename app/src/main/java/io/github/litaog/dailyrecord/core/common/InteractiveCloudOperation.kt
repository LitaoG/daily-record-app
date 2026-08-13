package io.github.litaog.dailyrecord.core.common

import com.google.firebase.FirebaseNetworkException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

internal const val INTERACTIVE_CLOUD_TIMEOUT_MILLIS = 5_000L

/** Background reconciliation may wait longer; it is not a user-facing action. */
internal const val BACKGROUND_CLOUD_TIMEOUT_MILLIS = 30_000L

internal class InteractiveCloudTimeoutException(
    val timeoutMillis: Long = INTERACTIVE_CLOUD_TIMEOUT_MILLIS,
    cause: Throwable? = null,
) : IOException("Cloud operation timed out after $timeoutMillis ms", cause) {
    constructor(cause: Throwable?) : this(INTERACTIVE_CLOUD_TIMEOUT_MILLIS, cause)
}

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
