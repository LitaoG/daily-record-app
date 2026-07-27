package io.github.litaog.dailyrecord.core.cloud

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InteractiveCloudOperationTest {
    @Test
    fun timeoutReturnsNetworkClassifiedFailure() = runBlocking {
        val result = runInteractiveCloudOperation(timeoutMillis = 20L) {
            delay(1_000L)
        }

        val error = result.exceptionOrNull()
        assertTrue(error is InteractiveCloudTimeoutException)
        assertTrue(requireNotNull(error).isNetworkReachabilityFailure())
    }

    @Test
    fun wrappedIoFailureIsRecognizedAsNetworkReachabilityFailure() {
        val error = IllegalStateException("wrapper", IOException("offline"))

        assertTrue(error.isNetworkReachabilityFailure())
    }

    @Test
    fun lifecycleCancellationStillPropagates() = runBlocking {
        try {
            runInteractiveCloudOperation {
                throw CancellationException("screen closed")
            }
            fail("CancellationException should propagate")
        } catch (_: CancellationException) {
            // Expected: leaving the screen is not presented as a cloud failure.
        }
    }
}
