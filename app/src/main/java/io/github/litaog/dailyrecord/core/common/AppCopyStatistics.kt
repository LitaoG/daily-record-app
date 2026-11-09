package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate

internal interface AppCopyStatistics {
        val weekdays: List<String> get() = AppLanguageState.current.statistics.weekdays
        val title: String get() = AppLanguageState.current.statistics.title
        val countAndDays: String get() = AppLanguageState.current.statistics.countAndDays
        val countUnit: String get() = AppLanguageState.current.statistics.countUnit
        val dayUnit: String get() = AppLanguageState.current.statistics.dayUnit
        val perDayUnit: String get() = AppLanguageState.current.statistics.perDayUnit
        val recordedDaysLabel: String get() = AppLanguageState.current.statistics.recordedDaysLabel
        val averageLabel: String get() = AppLanguageState.current.statistics.averageLabel
        val dailyDetails: String get() = AppLanguageState.current.statistics.dailyDetails
        val monthlyDetails: String get() = AppLanguageState.current.statistics.monthlyDetails
        val yearlyDetails: String get() = AppLanguageState.current.statistics.yearlyDetails
        val allHistory: String get() = AppLanguageState.current.statistics.allHistory
        val ended: String get() = AppLanguageState.current.statistics.ended
        val inProgress: String get() = AppLanguageState.current.statistics.inProgress
        val noRecords: String get() = AppLanguageState.current.statistics.noRecords
        val weekTab: String get() = AppLanguageState.current.statistics.weekTab
        val monthTab: String get() = AppLanguageState.current.statistics.monthTab
        val yearTab: String get() = AppLanguageState.current.statistics.yearTab
        val allTab: String get() = AppLanguageState.current.statistics.allTab
        val currentWeek: String get() = AppLanguageState.current.statistics.currentWeek
        val currentMonth: String get() = AppLanguageState.current.statistics.currentMonth
        val monthTotalCount: String get() = AppLanguageState.current.statistics.monthTotalCount
        val currentYear: String get() = AppLanguageState.current.statistics.currentYear
        val historyPeriod: String get() = AppLanguageState.current.statistics.historyPeriod
        val selectRange: String get() = AppLanguageState.current.statistics.selectRange
        val emptyTitle: String get() = AppLanguageState.current.statistics.emptyTitle
        val emptyMessage: String get() = AppLanguageState.current.statistics.emptyMessage
        val calendarAction: String get() = AppLanguageState.current.statistics.calendarAction
        val dailyDistribution: String get() = AppLanguageState.current.statistics.dailyDistribution
        val times: String get() = AppLanguageState.current.statistics.times
        val weeklySummaryTitle: String get() = AppLanguageState.current.statistics.weeklySummaryTitle
        val weeklyRecordedLabel: String get() = AppLanguageState.current.statistics.weeklyRecordedLabel
        val weeklyLegendFourPlus: String get() = AppLanguageState.current.statistics.weeklyLegendFourPlus
        val weeklyLegendThree: String get() = AppLanguageState.current.statistics.weeklyLegendThree
        val weeklyLegendTwo: String get() = AppLanguageState.current.statistics.weeklyLegendTwo
        val weeklyLegendOne: String get() = AppLanguageState.current.statistics.weeklyLegendOne
        val weeklyLegendZero: String get() = AppLanguageState.current.statistics.weeklyLegendZero
        val weeklyLegendUnrecorded: String get() = AppLanguageState.current.statistics.weeklyLegendUnrecorded
        val weeklyLegendFuture: String get() = AppLanguageState.current.statistics.weeklyLegendFuture
        val dailyCount: String get() = AppLanguageState.current.statistics.dailyCount
        val byDate: String get() = AppLanguageState.current.statistics.byDate
        val countComposition: String get() = AppLanguageState.current.statistics.countComposition
        val explicitZero: String get() = AppLanguageState.current.statistics.explicitZero
        val once: String get() = AppLanguageState.current.statistics.once
        val twice: String get() = AppLanguageState.current.statistics.twice
        val threePlus: String get() = AppLanguageState.current.statistics.threePlus
        val unfilledDays: String get() = AppLanguageState.current.statistics.unfilledDays
        val futureDays: String get() = AppLanguageState.current.statistics.futureDays
        val noSavedDays: String get() = AppLanguageState.current.statistics.noSavedDays
        val singleDayExtremes: String get() = AppLanguageState.current.statistics.singleDayExtremes
        val byPositiveCount: String get() = AppLanguageState.current.statistics.byPositiveCount
        val maximumDay: String get() = AppLanguageState.current.statistics.maximumDay
        val minimumPositiveDay: String get() = AppLanguageState.current.statistics.minimumPositiveDay
        val noPositiveDay: String get() = AppLanguageState.current.statistics.noPositiveDay
        val future: String get() = AppLanguageState.current.statistics.future
        val unset: String get() = AppLanguageState.current.statistics.unset
        val unsetShort: String get() = AppLanguageState.current.statistics.unsetShort
        val dash: String get() = AppLanguageState.current.statistics.dash
        val annualCount: String get() = AppLanguageState.current.statistics.annualCount
        val quarterShare: String get() = AppLanguageState.current.statistics.quarterShare
        val noPositiveCount: String get() = AppLanguageState.current.statistics.noPositiveCount
        val byCount: String get() = AppLanguageState.current.statistics.byCount
        val quarterShareHint: String get() = AppLanguageState.current.statistics.quarterShareHint
        val monthSummary: String get() = AppLanguageState.current.statistics.monthSummary
        val fullMonths: String get() = AppLanguageState.current.statistics.fullMonths
        val monthExtremesHint: String get() = AppLanguageState.current.statistics.monthExtremesHint
        val maximumMonth: String get() = AppLanguageState.current.statistics.maximumMonth
        val minimumMonth: String get() = AppLanguageState.current.statistics.minimumMonth
        val monthAverageFormat: String get() = AppLanguageState.current.statistics.monthAverageFormat

        fun detailCount(count: Long?): String =
            AppLanguageState.current.statistics.detailCount(count)

        fun detailDays(days: Int?): String =
            AppLanguageState.current.statistics.detailDays(days)

        fun historyStatus(first: LocalDate?, today: LocalDate): String =
            AppLanguageState.current.statistics.historyStatus(first, today)

        fun yearTitle(year: Int): String = AppLanguageState.current.statistics.yearTitle(year)

        fun monthTitle(year: Int, month: Int): String =
            AppLanguageState.current.statistics.monthTitle(year, month)

        fun monthLabel(month: Int): String = AppLanguageState.current.statistics.monthLabel(month)

        val monthListSeparator: String get() = AppLanguageState.current.statistics.monthListSeparator

        fun dateDescription(date: LocalDate, status: String): String =
            AppLanguageState.current.statistics.dateDescription(date, status)

        fun weekdayDateLabel(weekday: String, date: LocalDate): String =
            AppLanguageState.current.statistics.weekdayDateLabel(weekday, date)

        fun dayLabel(day: Int): String = AppLanguageState.current.statistics.dayLabel(day)

        fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            AppLanguageState.current.statistics.dateRangeTitle(start, end)

        fun yearStatus(end: LocalDate, today: LocalDate): String =
            AppLanguageState.current.statistics.yearStatus(end, today)

        fun periodStatus(end: LocalDate, today: LocalDate): String =
            AppLanguageState.current.statistics.periodStatus(end, today)

        fun periodAction(period: String, previous: Boolean): String =
            AppLanguageState.current.statistics.periodAction(period, previous)

        fun datePickerDescription(title: String): String =
            AppLanguageState.current.statistics.datePickerDescription(title)

        fun emptyTitle(moduleLabel: String): String =
            AppLanguageState.current.statistics.emptyTitle(moduleLabel)

        fun periodCountLabel(period: String, moduleLabel: String): String =
            AppLanguageState.current.statistics.periodCountLabel(period, moduleLabel)

        fun statisticsLabel(period: String): String =
            AppLanguageState.current.statistics.statisticsLabel(period)

        fun average(value: Double): String = AppLanguageState.current.statistics.average(value)

        fun averageNumber(value: Double): String =
            AppLanguageState.current.statistics.averageNumber(value)

        fun annualAverage(value: Double): String =
            AppLanguageState.current.statistics.annualAverage(value)

        fun countText(count: Long): String = AppLanguageState.current.statistics.countText(count)

        fun weeklyRecordedDays(recorded: Int, total: Int): String =
            AppLanguageState.current.statistics.weeklyRecordedDays(recorded, total)

        fun weeklyCountSuffix(count: Long): String =
            AppLanguageState.current.statistics.weeklyCountSuffix(count)

        fun daysText(days: Int): String = AppLanguageState.current.statistics.daysText(days)

        fun savedDaysSubtitle(days: Int): String =
            AppLanguageState.current.statistics.savedDaysSubtitle(days)

        fun categoryDays(days: Int): String = AppLanguageState.current.statistics.categoryDays(days)

        fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String =
            AppLanguageState.current.statistics.dayChartValue(day, count, future, recorded)

        fun monthDailyChartAccessibility(days: String): String =
            AppLanguageState.current.statistics.monthDailyChartAccessibility(days)

        fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String =
            AppLanguageState.current.statistics.monthSummaryAccessibility(totalCount, recordedDays, average)

        fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String = AppLanguageState.current.statistics.monthCompositionAccessibility(
            savedDays, explicitZeroDays, oneCountDays, twoCountDays,
            threePlusCountDays, unfilledDays, futureDays,
        )

        fun monthExtremeAccessibility(label: String, count: Long): String =
            AppLanguageState.current.statistics.monthExtremeAccessibility(label, count)

        fun percentage(value: Double): String = AppLanguageState.current.statistics.percentage(value)

        fun quarterLabel(quarter: Int): String =
            AppLanguageState.current.statistics.quarterLabel(quarter)

        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            AppLanguageState.current.statistics.monthChartLabel(month, isFuture, recorded, count)

        fun totalCountAccessibility(total: Long, quarters: String): String =
            AppLanguageState.current.statistics.totalCountAccessibility(total, quarters)

        fun annualChartAccessibility(months: String): String =
            AppLanguageState.current.statistics.annualChartAccessibility(months)

        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String =
            AppLanguageState.current.statistics.chartMonthValue(isFuture, recorded, count)
    }
