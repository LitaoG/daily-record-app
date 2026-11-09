package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth

internal interface AppCopyCalendar {
        val weekdays: List<String> get() = AppLanguageState.current.calendar.weekdays
        val recordHint: String get() = AppLanguageState.current.calendar.recordHint
        val unset: String get() = AppLanguageState.current.calendar.unset
        val future: String get() = AppLanguageState.current.calendar.future
        val zero: String get() = AppLanguageState.current.calendar.zero
        val recorded: String get() = AppLanguageState.current.calendar.recorded
        val previousMonth: String get() = AppLanguageState.current.calendar.previousMonth
        val nextMonth: String get() = AppLanguageState.current.calendar.nextMonth
        val selectDate: String get() = AppLanguageState.current.calendar.selectDate
        val backToToday: String get() = AppLanguageState.current.calendar.backToToday
        val legendDescription: String get() = AppLanguageState.current.calendar.legendDescription
        val unavailable: String get() = AppLanguageState.current.calendar.unavailable
        val oneTime: String get() = AppLanguageState.current.calendar.oneTime
        val twoTimes: String get() = AppLanguageState.current.calendar.twoTimes
        val ninePlusTimes: String get() = AppLanguageState.current.calendar.ninePlusTimes
        val todayShort: String get() = AppLanguageState.current.calendar.todayShort
        val futureDescription: String get() = AppLanguageState.current.calendar.futureDescription
        val unsetDescription: String get() = AppLanguageState.current.calendar.unsetDescription
        val zeroDescription: String get() = AppLanguageState.current.calendar.zeroDescription
        val selectedSuffix: String get() = AppLanguageState.current.calendar.selectedSuffix
        val todaySuffix: String get() = AppLanguageState.current.calendar.todaySuffix
        val quickLogTitle: String get() = AppLanguageState.current.calendar.quickLogTitle
        val quickLogMarkZero: String get() = AppLanguageState.current.calendar.quickLogMarkZero

        fun quickLogStatus(count: Int?): String =
            AppLanguageState.current.calendar.quickLogStatus(count)

        fun quickLogStatusDescription(moduleLabel: String, count: Int?): String =
            AppLanguageState.current.calendar.quickLogStatusDescription(moduleLabel, count)

        fun quickLogIncreaseDescription(moduleLabel: String): String =
            AppLanguageState.current.calendar.quickLogIncreaseDescription(moduleLabel)

        fun quickLogDecreaseDescription(moduleLabel: String): String =
            AppLanguageState.current.calendar.quickLogDecreaseDescription(moduleLabel)

        fun quickLogMarkZeroDescription(moduleLabel: String): String =
            AppLanguageState.current.calendar.quickLogMarkZeroDescription(moduleLabel)

        val quickAdjustAction: String get() = AppLanguageState.current.calendar.quickAdjustAction
        val quickAdjustDone: String get() = AppLanguageState.current.calendar.quickAdjustDone

        fun quickAdjustTitle(date: LocalDate): String =
            AppLanguageState.current.calendar.quickAdjustTitle(date)

        fun quickAdjustStatusDescription(date: LocalDate, moduleLabel: String, count: Int?): String =
            AppLanguageState.current.calendar.quickAdjustStatusDescription(date, moduleLabel, count)

        fun quickAdjustIncreaseDescription(date: LocalDate, moduleLabel: String): String =
            AppLanguageState.current.calendar.quickAdjustIncreaseDescription(date, moduleLabel)

        fun quickAdjustDecreaseDescription(date: LocalDate, moduleLabel: String): String =
            AppLanguageState.current.calendar.quickAdjustDecreaseDescription(date, moduleLabel)

        fun monthSummary(count: Long, days: Int): String =
            AppLanguageState.current.calendar.monthSummary(count, days)

        fun monthTitle(month: YearMonth): String =
            AppLanguageState.current.calendar.monthTitle(month)

        fun monthTitleMultiline(month: YearMonth): String =
            AppLanguageState.current.calendar.monthTitleMultiline(month)

        fun monthSelectionDescription(month: YearMonth): String =
            AppLanguageState.current.calendar.monthSelectionDescription(month)

        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            AppLanguageState.current.calendar.monthDateDescription(date, state, focused)

        fun legendDescription(moduleLabel: String): String =
            AppLanguageState.current.calendar.legendDescription(moduleLabel)

        fun countDescription(count: Int): String =
            AppLanguageState.current.calendar.countDescription(count)

        fun statusDescription(
            date: LocalDate,
            today: LocalDate,
            unsupported: Boolean,
            future: Boolean,
            count: Int?,
            moduleLabel: String,
        ): String = AppLanguageState.current.calendar.statusDescription(
            date, today, unsupported, future, count, moduleLabel,
        )
    }

internal interface AppCopyNavigation {
        val weekdays: List<String> get() = AppLanguageState.current.navigation.weekdays
        val title: String get() = AppLanguageState.current.navigation.title
        val subtitle: String get() = AppLanguageState.current.navigation.subtitle
        val dateWheelSubtitle: String get() = AppLanguageState.current.navigation.dateWheelSubtitle
        val monthSubtitle: String get() = AppLanguageState.current.navigation.monthSubtitle
        val jumpToDate: String get() = AppLanguageState.current.navigation.jumpToDate
        val jumpToMonth: String get() = AppLanguageState.current.navigation.jumpToMonth
        val jumpToYear: String get() = AppLanguageState.current.navigation.jumpToYear
        val selected: String get() = AppLanguageState.current.navigation.selected
        val switchYear: String get() = AppLanguageState.current.navigation.switchYear
        val returnToDatePicker: String get() = AppLanguageState.current.navigation.returnToDatePicker
        val selectYear: String get() = AppLanguageState.current.navigation.selectYear
        val selectMonth: String get() = AppLanguageState.current.navigation.selectMonth
        val dateWheelHint: String get() = AppLanguageState.current.navigation.dateWheelHint
        val yearUnit: String get() = AppLanguageState.current.navigation.yearUnit
        val monthUnit: String get() = AppLanguageState.current.navigation.monthUnit
        val dayUnit: String get() = AppLanguageState.current.navigation.dayUnit

        fun switchYearDescription(year: Int): String =
            AppLanguageState.current.navigation.switchYearDescription(year)

        fun nextYearDescription(forward: Boolean): String =
            AppLanguageState.current.navigation.nextYearDescription(forward)

        fun dateText(date: LocalDate): String =
            AppLanguageState.current.navigation.dateText(date)

        fun dateLabel(date: LocalDate, weekday: String): String =
            AppLanguageState.current.navigation.dateLabel(date, weekday)

        fun selectYearDescription(year: Int): String =
            AppLanguageState.current.navigation.selectYearDescription(year)

        fun yearTitle(year: Int): String = AppLanguageState.current.navigation.yearTitle(year)

        fun monthTitle(month: YearMonth): String =
            AppLanguageState.current.navigation.monthTitle(month)

        fun monthLabel(month: Int): String = AppLanguageState.current.navigation.monthLabel(month)

        fun dayLabel(day: Int): String = AppLanguageState.current.navigation.dayLabel(day)

        fun monthDescription(month: YearMonth): String =
            AppLanguageState.current.navigation.monthDescription(month)
    }

internal interface AppCopyNavigationBar {
        val calendar: String get() = AppLanguageState.current.navigationBar.calendar
        val statistics: String get() = AppLanguageState.current.navigationBar.statistics
    }
