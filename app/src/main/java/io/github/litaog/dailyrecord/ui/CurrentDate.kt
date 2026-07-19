package io.github.litaog.dailyrecord.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

private const val MAX_DATE_REFRESH_DELAY_MILLIS = 60L * 60L * 1_000L

/** Keeps calendar boundaries current across midnight without requiring an app restart. */
@Composable
internal fun rememberCurrentDate(clock: Clock = Clock.systemDefaultZone()): LocalDate {
    val currentDate by produceState(initialValue = LocalDate.now(clock), clock) {
        while (true) {
            delay(currentDateRefreshDelayMillis(Instant.now(clock), clock.zone))
            value = LocalDate.now(clock)
        }
    }
    return currentDate
}

internal fun currentDateRefreshDelayMillis(now: Instant, zoneId: ZoneId): Long {
    val nextMidnight = now.atZone(zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
    return Duration.between(now, nextMidnight)
        .toMillis()
        .coerceIn(1_000L, MAX_DATE_REFRESH_DELAY_MILLIS)
}
