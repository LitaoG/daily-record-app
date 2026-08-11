package io.github.litaog.dailyrecord.ui

import java.time.LocalDate
import java.time.YearMonth

/** The earliest date the calendar and statistics support. */
internal val EARLIEST_SUPPORTED_DATE: LocalDate = LocalDate.of(1970, 1, 1)

internal val EARLIEST_SUPPORTED_MONTH: YearMonth = YearMonth.from(EARLIEST_SUPPORTED_DATE)

