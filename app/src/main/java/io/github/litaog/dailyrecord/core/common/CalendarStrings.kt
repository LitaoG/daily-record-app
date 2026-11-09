package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth

internal interface CalendarStrings {
        val weekdays: List<String>
        val recordHint: String
        val unset: String
        val future: String
        val zero: String
        val recorded: String
        val previousMonth: String
        val nextMonth: String
        val selectDate: String
        val backToToday: String
        val legendDescription: String
        val unavailable: String
        val oneTime: String
        val twoTimes: String
        val ninePlusTimes: String
        val todayShort: String
        val futureDescription: String
        val unsetDescription: String
        val zeroDescription: String
        val selectedSuffix: String
        val todaySuffix: String

        // Today quick-log island (ADR-022).
        val quickLogTitle: String
        val quickLogMarkZero: String

        /** Visible status line; [count] null means today is not filled yet. */
        fun quickLogStatus(count: Int?): String

        fun quickLogStatusDescription(moduleLabel: String, count: Int?): String

        fun quickLogIncreaseDescription(moduleLabel: String): String

        fun quickLogDecreaseDescription(moduleLabel: String): String

        fun quickLogMarkZeroDescription(moduleLabel: String): String

        // Long-press quick adjust on a calendar date (ADR-022).
        /** Accessibility label of the long-press action on a date cell. */
        val quickAdjustAction: String
        val quickAdjustDone: String

        fun quickAdjustTitle(date: LocalDate): String

        fun quickAdjustStatusDescription(date: LocalDate, moduleLabel: String, count: Int?): String

        fun quickAdjustIncreaseDescription(date: LocalDate, moduleLabel: String): String

        fun quickAdjustDecreaseDescription(date: LocalDate, moduleLabel: String): String

        fun monthSummary(count: Long, days: Int): String

        fun monthTitle(month: YearMonth): String

        fun monthTitleMultiline(month: YearMonth): String

        fun monthSelectionDescription(month: YearMonth): String

        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String

        fun legendDescription(moduleLabel: String): String

        fun countDescription(count: Int): String

        fun statusDescription(
            date: LocalDate,
            today: LocalDate,
            unsupported: Boolean,
            future: Boolean,
            count: Int?,
            moduleLabel: String,
        ): String
    }

internal interface NavigationStrings {
        val weekdays: List<String>
        val title: String
        val subtitle: String
        val dateWheelSubtitle: String
        val monthSubtitle: String
        val jumpToDate: String
        val jumpToMonth: String
        val jumpToYear: String
        val selected: String
        val switchYear: String
        val returnToDatePicker: String
        val selectYear: String
        val selectMonth: String
        val dateWheelHint: String
        val yearUnit: String
        val monthUnit: String
        val dayUnit: String

        fun switchYearDescription(year: Int): String

        fun nextYearDescription(forward: Boolean): String

        fun dateText(date: LocalDate): String

        fun dateLabel(date: LocalDate, weekday: String): String

        fun selectYearDescription(year: Int): String

        fun yearTitle(year: Int): String

        fun monthTitle(month: YearMonth): String

        fun monthLabel(month: Int): String

        fun dayLabel(day: Int): String

        fun monthDescription(month: YearMonth): String
    }

internal interface NavigationBarStrings {
        val calendar: String
        val statistics: String
    }
