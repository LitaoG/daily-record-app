package io.github.litaog.dailyrecord.core.common

import java.time.LocalTime

/** Minute-of-day helpers shared by the record editor and its details. */

fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

fun formatMinutesOfDay(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)
