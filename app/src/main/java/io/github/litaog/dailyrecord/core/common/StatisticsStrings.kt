package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate

internal interface StatisticsStrings {
        val weekdays: List<String>
        val title: String
        val countAndDays: String
        val countUnit: String
        val dayUnit: String
        val perDayUnit: String
        val recordedDaysLabel: String
        val averageLabel: String
        val dailyDetails: String
        val monthlyDetails: String
        val yearlyDetails: String
        val allHistory: String
        val ended: String
        val inProgress: String
        val noRecords: String
        val weekTab: String
        val monthTab: String
        val yearTab: String
        val allTab: String
        val currentWeek: String
        val currentMonth: String
        val monthTotalCount: String
        val currentYear: String
        val historyPeriod: String
        val selectRange: String
        val emptyTitle: String
        val emptyMessage: String
        val calendarAction: String
        val dailyDistribution: String
        val times: String
        val weeklySummaryTitle: String
        val weeklyRecordedLabel: String
        val weeklyLegendFourPlus: String
        val weeklyLegendThree: String
        val weeklyLegendTwo: String
        val weeklyLegendOne: String
        val weeklyLegendZero: String
        val weeklyLegendUnrecorded: String
        val weeklyLegendFuture: String
        val dailyCount: String
        val byDate: String
        val countComposition: String
        val explicitZero: String
        val once: String
        val twice: String
        val threePlus: String
        val unfilledDays: String
        val futureDays: String
        val noSavedDays: String
        val singleDayExtremes: String
        val byPositiveCount: String
        val maximumDay: String
        val minimumPositiveDay: String
        val noPositiveDay: String
        val future: String
        val unset: String
        val unsetShort: String
        val dash: String
        val annualCount: String
        val quarterShare: String
        val noPositiveCount: String
        val byCount: String
        val quarterShareHint: String
        val monthSummary: String
        val fullMonths: String
        val monthExtremesHint: String
        val maximumMonth: String
        val minimumMonth: String
        val monthAverageFormat: String

        fun detailCount(count: Long?): String

        fun detailDays(days: Int?): String

        fun historyStatus(first: LocalDate?, today: LocalDate): String

        fun yearTitle(year: Int): String

        fun monthTitle(year: Int, month: Int): String

        fun monthLabel(month: Int): String

        /** Joins a list of month labels for display; never hardcode a separator in UI code. */
        val monthListSeparator: String

        fun dateDescription(date: LocalDate, status: String): String

        fun weekdayDateLabel(weekday: String, date: LocalDate): String

        fun dayLabel(day: Int): String

        fun dateRangeTitle(start: LocalDate, end: LocalDate): String

        fun yearStatus(end: LocalDate, today: LocalDate): String

        fun periodStatus(end: LocalDate, today: LocalDate): String

        fun periodAction(period: String, previous: Boolean): String

        fun datePickerDescription(title: String): String

        fun emptyTitle(moduleLabel: String): String

        fun periodCountLabel(period: String, moduleLabel: String): String

        fun statisticsLabel(period: String): String

        fun average(value: Double): String

        fun averageNumber(value: Double): String

        fun annualAverage(value: Double): String

        fun countText(count: Long): String

        fun weeklyRecordedDays(recorded: Int, total: Int): String

        fun weeklyCountSuffix(count: Long): String

        fun daysText(days: Int): String

        fun savedDaysSubtitle(days: Int): String

        fun categoryDays(days: Int): String

        fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String

        fun monthDailyChartAccessibility(days: String): String

        fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String

        fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String

        fun monthExtremeAccessibility(label: String, count: Long): String

        fun percentage(value: Double): String

        fun quarterLabel(quarter: Int): String

        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String

        fun totalCountAccessibility(total: Long, quarters: String): String

        fun annualChartAccessibility(months: String): String

        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String
    }
