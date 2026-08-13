package io.github.litaog.dailyrecord.core.statistics

import io.github.litaog.dailyrecord.core.common.AppCopy

/** Statistics period shared by the statistics screen, navigation and app shell. */
enum class StatisticsPeriod(val label: String) {
    Week(AppCopy.Statistics.weekTab),
    Month(AppCopy.Statistics.monthTab),
    Year(AppCopy.Statistics.yearTab),
    All(AppCopy.Statistics.allTab),
}
