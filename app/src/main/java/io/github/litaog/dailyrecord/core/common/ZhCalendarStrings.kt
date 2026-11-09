package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth

internal object ZhCalendarStrings : CalendarStrings {
        override val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        override val recordHint = "点击日期填写次数"
        override val unset = "未填"
        override val future = "未来"
        override val zero = "0 次"
        override val recorded = "已记录"
        override val previousMonth = "上个月"
        override val nextMonth = "下个月"
        override val selectDate = "选择年份和日期"
        override val backToToday = "回到今天"
        override val legendDescription = "点击日期填写%s次数。状态包括：未填写、未来不可填写、0 次和已记录"
        override val unavailable = "不可用"
        override val oneTime = "1 次"
        override val twoTimes = "2 次"
        override val ninePlusTimes = "9 次以上"
        override val todayShort = "今"
        override val futureDescription = "未来日期，不可记录"
        override val unsetDescription = "未填写"
        override val zeroDescription = "记录为 0 次"
        override val selectedSuffix = "，已选择"
        override val todaySuffix = "，今天"

        override val quickLogTitle = "今日速记"
        override val quickLogMarkZero = "记为 0 次"

        override fun quickLogStatus(count: Int?): String =
            if (count == null) "今天还没记录" else "今天 $count 次"

        override fun quickLogStatusDescription(moduleLabel: String, count: Int?): String =
            if (count == null) "今天${moduleLabel}还没记录" else "今天${moduleLabel}已记录 $count 次"

        override fun quickLogIncreaseDescription(moduleLabel: String): String = "今天${moduleLabel}加 1 次"

        override fun quickLogDecreaseDescription(moduleLabel: String): String = "今天${moduleLabel}减 1 次"

        override fun quickLogMarkZeroDescription(moduleLabel: String): String = "把今天${moduleLabel}记为 0 次"

        override val quickAdjustAction = "快速调整次数"
        override val quickAdjustDone = "完成"

        override fun quickAdjustTitle(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

        override fun quickAdjustStatusDescription(date: LocalDate, moduleLabel: String, count: Int?): String =
            if (count == null) {
                "${quickAdjustTitle(date)}${moduleLabel}还没记录"
            } else {
                "${quickAdjustTitle(date)}${moduleLabel}已记录 $count 次"
            }

        override fun quickAdjustIncreaseDescription(date: LocalDate, moduleLabel: String): String =
            "${quickAdjustTitle(date)}${moduleLabel}加 1 次"

        override fun quickAdjustDecreaseDescription(date: LocalDate, moduleLabel: String): String =
            "${quickAdjustTitle(date)}${moduleLabel}减 1 次"

        override fun monthSummary(count: Long, days: Int): String = "本月 $count 次 · $days 天有记录"

        override fun monthTitle(month: YearMonth): String = "${month.year}年 ${month.monthValue}月"

        override fun monthTitleMultiline(month: YearMonth): String = "${month.year}年\n${month.monthValue}月"

        override fun monthSelectionDescription(month: YearMonth): String =
            "$selectDate，当前${month.year}年${month.monthValue}月"

        override fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$state${if (focused) selectedSuffix else ""}"

        override fun legendDescription(moduleLabel: String): String = legendDescription.format(moduleLabel)

        override fun countDescription(count: Int): String = when (count) {
            0 -> zero
            1 -> oneTime
            2 -> twoTimes
            in 3..8 -> "${count} 次"
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
                count == 0 -> "$moduleLabel，$zeroDescription"
                else -> "$moduleLabel，${countDescription(count)}"
            }
            return if (date == today) "$status$todaySuffix" else status
        }
    }

internal object ZhNavigationStrings : NavigationStrings {
        override val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        override val title = "快速跳转"
        override val subtitle = "直接选择年份和日期"
        override val dateWheelSubtitle = "选择日期"
        override val monthSubtitle = "直接选择年份和月份"
        override val jumpToDate = "跳转到此日"
        override val jumpToMonth = "跳转到此月"
        override val jumpToYear = "跳转到此年"
        override val selected = "已选择"
        override val switchYear = "切换年份"
        override val returnToDatePicker = "返回日期选择"
        override val selectYear = "选择年份"
        override val selectMonth = "选择月份"
        override val dateWheelHint = "上下滑动调整日期"
        override val yearUnit = "年"
        override val monthUnit = "月"
        override val dayUnit = "日"
        private val monthNames = listOf(
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月",
        )

        override fun switchYearDescription(year: Int): String = "$switchYear，当前${year}年"

        override fun nextYearDescription(forward: Boolean): String =
            if (forward) "跳转到下一年" else "跳转到上一年"

        override fun dateText(date: LocalDate): String =
            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))

        override fun dateLabel(date: LocalDate, weekday: String): String =
            "${dateText(date)} · $weekday"

        override fun selectYearDescription(year: Int): String = "选择${year}年"

        override fun yearTitle(year: Int): String = "${year}年"

        override fun monthTitle(month: YearMonth): String = "${month.year}年 ${month.monthValue}月"

        override fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "${month}月" }

        override fun dayLabel(day: Int): String = "${day}日"

        override fun monthDescription(month: YearMonth): String = "选择${month.year}年${month.monthValue}月"
    }

internal object ZhNavigationBarStrings : NavigationBarStrings {
        override val calendar = "日历"
        override val statistics = "统计"
    }
