package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object EnCalendarStrings : CalendarStrings {
        override val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
        override val recordHint = "Tap a date to record"
        override val unset = "Unset"
        override val future = "Future"
        override val zero = "0"
        override val recorded = "Recorded"
        override val previousMonth = "Previous month"
        override val nextMonth = "Next month"
        override val selectDate = "Choose date"
        override val backToToday = "Back to today"
        override val legendDescription = "Tap a date to record %s. States: unset, future (disabled), 0, recorded"
        override val unavailable = "Unavailable"
        override val oneTime = "1"
        override val twoTimes = "2"
        override val ninePlusTimes = "9+"
        // A dot marker keeps the square cell geometry intact at 200% font;
        // the today border and TalkBack "today" carry the full meaning.
        override val todayShort = "•"
        override val futureDescription = "Future date, not recordable"
        override val unsetDescription = "Unset"
        override val zeroDescription = "Recorded as 0"
        override val selectedSuffix = ", selected"
        override val todaySuffix = ", today"

        override val quickLogTitle = "Today"
        override val quickLogMarkZero = "Log 0"

        override fun quickLogStatus(count: Int?): String = when (count) {
            null -> "Not logged yet"
            1 -> "1 time today"
            else -> "$count times today"
        }

        override fun quickLogStatusDescription(moduleLabel: String, count: Int?): String = when (count) {
            null -> "$moduleLabel today: not logged yet"
            1 -> "$moduleLabel today: 1 time"
            else -> "$moduleLabel today: $count times"
        }

        override fun quickLogIncreaseDescription(moduleLabel: String): String = "Add 1 to $moduleLabel today"

        override fun quickLogDecreaseDescription(moduleLabel: String): String = "Remove 1 from $moduleLabel today"

        override fun quickLogMarkZeroDescription(moduleLabel: String): String = "Log $moduleLabel today as 0"

        override val quickAdjustAction = "Quick adjust"
        override val quickAdjustDone = "Done"

        override fun quickAdjustTitle(date: LocalDate): String =
            date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

        override fun quickAdjustStatusDescription(date: LocalDate, moduleLabel: String, count: Int?): String =
            when (count) {
                null -> "$moduleLabel on ${quickAdjustTitle(date)}: not logged yet"
                1 -> "$moduleLabel on ${quickAdjustTitle(date)}: 1 time"
                else -> "$moduleLabel on ${quickAdjustTitle(date)}: $count times"
            }

        override fun quickAdjustIncreaseDescription(date: LocalDate, moduleLabel: String): String =
            "Add 1 to $moduleLabel on ${quickAdjustTitle(date)}"

        override fun quickAdjustDecreaseDescription(date: LocalDate, moduleLabel: String): String =
            "Remove 1 from $moduleLabel on ${quickAdjustTitle(date)}"

        override fun monthSummary(count: Long, days: Int): String = "This month: $count times · $days days"

        override fun monthTitle(month: YearMonth): String =
            "${month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)} ${month.year}"

        override fun monthTitleMultiline(month: YearMonth): String =
            "${month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)}\n${month.year}"

        override fun monthSelectionDescription(month: YearMonth): String =
            "$selectDate, current: ${monthTitle(month)}"

        override fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            "${dateText(date)}, $state${if (focused) selectedSuffix else ""}"

        override fun legendDescription(moduleLabel: String): String = legendDescription.format(moduleLabel)

        override fun countDescription(count: Int): String = when (count) {
            0 -> zero
            1 -> oneTime
            2 -> twoTimes
            in 3..8 -> "$count"
            else -> ninePlusTimes
        }

        override fun statusDescription(
            date: LocalDate,
            today: LocalDate,
            unsupported: Boolean,
            future: Boolean,
            count: Int?,
            moduleLabel: String,
        ): String {
            val status = when {
                unsupported -> unavailable
                future -> futureDescription
                count == null -> unsetDescription
                count == 0 -> "$moduleLabel, $zeroDescription"
                else -> "$moduleLabel, ${countDescription(count)}"
            }
            return if (date == today) "$status$todaySuffix" else status
        }

        private fun dateText(date: LocalDate): String =
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }

internal object EnNavigationStrings : NavigationStrings {
        override val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
        override val title = "Quick jump"
        override val subtitle = "Choose a year and date"
        override val dateWheelSubtitle = "Choose date"
        override val monthSubtitle = "Choose year and month"
        override val jumpToDate = "Jump to date"
        override val jumpToMonth = "Jump to month"
        override val jumpToYear = "Jump to year"
        override val selected = "Selected"
        override val switchYear = "Switch year"
        override val returnToDatePicker = "Back to date picker"
        override val selectYear = "Choose year"
        override val selectMonth = "Choose month"
        override val dateWheelHint = "Swipe to adjust the date"
        override val yearUnit = "Yr"
        override val monthUnit = "Mo"
        override val dayUnit = "D"
        private val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        override fun switchYearDescription(year: Int): String = "$switchYear, current: $year"

        override fun nextYearDescription(forward: Boolean): String =
            if (forward) "Next year" else "Previous year"

        override fun dateText(date: LocalDate): String =
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

        override fun dateLabel(date: LocalDate, weekday: String): String =
            "${dateText(date)} · $weekday"

        override fun selectYearDescription(year: Int): String = "Choose $year"

        override fun yearTitle(year: Int): String = "$year"

        override fun monthTitle(month: YearMonth): String =
            "${month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)} ${month.year}"

        override fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "$month" }

        override fun dayLabel(day: Int): String = "$day"

        override fun monthDescription(month: YearMonth): String = "Choose ${monthTitle(month)}"
    }

internal object EnNavigationBarStrings : NavigationBarStrings {
        override val calendar = "Calendar"
        override val statistics = "Stats"
    }
