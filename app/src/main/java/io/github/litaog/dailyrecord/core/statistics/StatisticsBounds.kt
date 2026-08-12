package io.github.litaog.dailyrecord.core.statistics

import java.time.LocalDate

/**
 * Earliest date the app supports. The first supported week is the partial week
 * starting at this date; older dates can never be recorded or navigated to.
 */
val EARLIEST_SUPPORTED_DATE: LocalDate = LocalDate.of(1970, 1, 1)
