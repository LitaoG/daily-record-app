package io.github.litaog.dailyrecord.core.common

import kotlinx.coroutines.CancellationException

/**
 * Converts ordinary operation failures to [Result] without swallowing coroutine cancellation.
 */
internal suspend fun <T> runCatchingPreservingCancellation(
    operation: suspend () -> T,
): Result<T> = try {
    Result.success(operation())
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
