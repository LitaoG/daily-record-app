package io.github.litaog.dailyrecord.core.statistics

import io.github.litaog.dailyrecord.core.common.AppCopy

/** Statistics period shared by the statistics screen, navigation and app shell. */
enum class StatisticsPeriod {
    Week,
    Month,
    Year,
    All,
}

/** Visible tab label; resolved at call time so language switches apply. */
val StatisticsPeriod.label: String
    get() = when (this) {
        StatisticsPeriod.Week -> AppCopy.Statistics.weekTab
        StatisticsPeriod.Month -> AppCopy.Statistics.monthTab
        StatisticsPeriod.Year -> AppCopy.Statistics.yearTab
        StatisticsPeriod.All -> AppCopy.Statistics.allTab
    }
