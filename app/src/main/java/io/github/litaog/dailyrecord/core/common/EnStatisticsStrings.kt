package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object EnStatisticsStrings : StatisticsStrings {
        override val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
        override val title = "Stats"
        override val countAndDays = "Times · Days"
        override val countUnit = "times"
        override val dayUnit = "days"
        override val perDayUnit = "/day"
        override val recordedDaysLabel = "Days"
        override val averageLabel = "Avg / day"
        override val dailyDetails = "Daily details"
        override val monthlyDetails = "Monthly details"
        override val yearlyDetails = "Yearly details"
        override val allHistory = "All history"
        override val ended = "Ended"
        override val inProgress = "In progress"
        override val noRecords = "No records"
        override val weekTab = "Wk"
        override val monthTab = "Mo"
        override val yearTab = "Yr"
        override val allTab = "All"
        override val currentWeek = "This week"
        override val currentMonth = "This month"
        override val monthTotalCount = "This month"
        override val currentYear = "This year"
        override val historyPeriod = "History"
        override val selectRange = "Choose range"
        override val emptyTitle = "No %s records yet"
        override val emptyMessage = "Tap a date on the calendar to add the first one."
        override val calendarAction = "Go to calendar"
        override val dailyDistribution = "By day"
        override val times = "Times"
        override val weeklySummaryTitle = "This week"
        override val weeklyRecordedLabel = "Recorded"
        override val weeklyLegendFourPlus = "4+"
        override val weeklyLegendThree = "3"
        override val weeklyLegendTwo = "2"
        override val weeklyLegendOne = "1"
        override val weeklyLegendZero = "0"
        override val weeklyLegendUnrecorded = "Unset"
        override val weeklyLegendFuture = "Future"
        override val dailyCount = "Daily count"
        override val byDate = "By date"
        override val countComposition = "Distribution"
        override val explicitZero = "0"
        override val once = "1 time"
        override val twice = "2 times"
        override val threePlus = "3+"
        override val unfilledDays = "Unset"
        override val futureDays = "Future"
        override val noSavedDays = "No records saved this month"
        override val singleDayExtremes = "Day high & low"
        override val byPositiveCount = "Counted days only"
        override val maximumDay = "High day"
        override val minimumPositiveDay = "Low day"
        override val noPositiveDay = "No day above 0 this month"
        override val future = "Future"
        override val unset = "Unset"
        override val unsetShort = "Unset"
        override val dash = "–"
        override val annualCount = "Yearly total"
        override val quarterShare = "Quarter share"
        override val noPositiveCount = "No counts yet"
        override val byCount = "By count"
        override val quarterShareHint = "Shown after counts are saved."
        override val monthSummary = "Month summary"
        override val fullMonths = "Full months only"
        override val monthExtremesHint = "Not enough full months to compare."
        override val maximumMonth = "High month"
        override val minimumMonth = "Low month"
        override val monthAverageFormat = "%.1f / month"

        private val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        override fun detailCount(count: Long?): String = if (count == null) unset else "$count times"

        override fun detailDays(days: Int?): String = if (days == null) unset else "$days days"

        override fun historyStatus(first: LocalDate?, today: LocalDate): String = first?.let {
            "${monthNames[it.monthValue - 1]} ${it.year} – ${monthNames[today.monthValue - 1]} ${today.year}"
        } ?: noRecords

        override fun yearTitle(year: Int): String = "$year"

        override fun monthTitle(year: Int, month: Int): String = "${monthNames.getOrElse(month - 1) { "$month" }} $year"

        override fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "$month" }

        override val monthListSeparator = ", "

        override fun dateDescription(date: LocalDate, status: String): String =
            "${dateText(date)}, $status"

        override fun weekdayDateLabel(weekday: String, date: LocalDate): String = "$weekday ${date.dayOfMonth}"

        override fun dayLabel(day: Int): String = "$day"

        override fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            if (start.year == end.year) {
                "${dateText(start)}–${monthNames[end.monthValue - 1]} ${end.dayOfMonth}, ${end.year}"
            } else {
                "${dateText(start)}–${dateText(end)}"
            }

        override fun yearStatus(end: LocalDate, today: LocalDate): String =
            if (end < today) ended else "Through ${monthNames[today.monthValue - 1]} ${today.dayOfMonth}"

        override fun periodStatus(end: LocalDate, today: LocalDate): String = if (end < today) ended else inProgress

        override fun periodAction(period: String, previous: Boolean): String = when (period) {
            weekTab -> if (previous) "Prev week" else "Next week"
            monthTab -> if (previous) "Prev month" else "Next month"
            yearTab -> if (previous) "Prev year" else "Next year"
            else -> if (previous) "Prev period" else "Next period"
        }

        override fun datePickerDescription(title: String): String = "$selectRange, current: $title"

        override fun emptyTitle(moduleLabel: String): String = emptyTitle.format(moduleLabel)

        override fun periodCountLabel(period: String, moduleLabel: String): String = "$period · $moduleLabel count"

        override fun statisticsLabel(period: String): String = "$period stats"

        override fun average(value: Double): String = String.format(Locale.US, "%.1f /day", value)

        override fun averageNumber(value: Double): String = String.format(Locale.US, "%.1f", value)

        override fun annualAverage(value: Double): String = String.format(Locale.US, monthAverageFormat, value)

        override fun countText(count: Long): String = "$count times"

        override fun weeklyRecordedDays(recorded: Int, total: Int): String = "$recorded / $total days"

        override fun weeklyCountSuffix(count: Long): String = " ($count)"

        override fun daysText(days: Int): String = "$days days"

        override fun savedDaysSubtitle(days: Int): String = "$days days saved"

        override fun categoryDays(days: Int): String = "$days days"

        override fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String = when {
            future -> "$day, future"
            !recorded -> "$day, unset"
            else -> "$day, ${countText(count ?: 0L)}"
        }

        override fun monthDailyChartAccessibility(days: String): String = "Daily count chart: $days"

        override fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String =
            "This month: ${countText(totalCount)}; ${daysText(recordedDays)}; ${average(average)}"

        override fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String =
            "Distribution: $savedDays days saved; 0: $explicitZeroDays days; 1: $oneCountDays; " +
                "2: $twoCountDays; 3+: $threePlusCountDays; unset: $unfilledDays; future: $futureDays"

        override fun monthExtremeAccessibility(label: String, count: Long): String =
            "$label, ${countText(count)}"

        override fun percentage(value: Double): String = String.format(Locale.US, "%.0f%%", value)

        override fun quarterLabel(quarter: Int): String = "Q$quarter"

        override fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            "${monthNames.getOrElse(month - 1) { "$month" }}: ${chartMonthValue(isFuture, recorded, count)}"

        override fun totalCountAccessibility(total: Long, quarters: String): String =
            "Quarter share: ${countText(total)}; $quarters"

        override fun annualChartAccessibility(months: String): String = "Yearly count chart: $months"

        override fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String = when {
            isFuture -> future
            !recorded -> unset
            else -> countText(count ?: 0L)
        }.toString()

        private fun dateText(date: LocalDate): String =
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }
