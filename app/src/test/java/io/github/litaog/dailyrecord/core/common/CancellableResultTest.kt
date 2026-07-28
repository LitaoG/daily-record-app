package io.github.litaog.dailyrecord.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellableResultTest {
    @Test
    fun successfulValueIsReturned() = runBlocking {
        assertEquals(7, runCatchingPreservingCancellation { 7 }.getOrThrow())
    }

    @Test
    fun ordinaryFailureIsCaptured() = runBlocking {
        val failure = IllegalStateException("failed")

        val result = runCatchingPreservingCancellation<Int> { throw failure }

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun cancellationIsPropagated() = runBlocking {
        val cancellation = CancellationException("screen closed")

        try {
            runCatchingPreservingCancellation<Int> { throw cancellation }
            fail("CancellationException should propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
