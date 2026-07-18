package io.github.litaog.dailyrecord.ui

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentDateTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test
    fun refreshesAtMidnightWhenItIsLessThanOneHourAway() {
        val now = Instant.parse("2026-07-17T15:59:30Z")

        assertEquals(30_000L, currentDateRefreshDelayMillis(now, shanghai))
    }

    @Test
    fun longWaitsAreCappedSoClockAndTimezoneChangesAreDetected() {
        val noon = Instant.parse("2026-07-17T04:00:00Z")

        assertEquals(3_600_000L, currentDateRefreshDelayMillis(noon, shanghai))
    }

    @Test
    fun refreshesAtLocalMidnightAcrossDaylightSavingBoundary() {
        val newYork = ZoneId.of("America/New_York")
        val thirtySecondsBeforeSpringForwardDay = Instant.parse("2026-03-08T04:59:30Z")

        assertEquals(
            30_000L,
            currentDateRefreshDelayMillis(thirtySecondsBeforeSpringForwardDay, newYork),
        )
    }
}
