package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.util.Locale

internal object ZhStatisticsStrings : StatisticsStrings {
        override val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        override val title = "统计"
        override val countAndDays = "次数 · 天数"
        override val countUnit = "次"
        override val dayUnit = "天"
        override val perDayUnit = "次/天"
        override val recordedDaysLabel = "发生天数"
        override val averageLabel = "日均次数"
        override val dailyDetails = "每日明细"
        override val monthlyDetails = "每月明细"
        override val yearlyDetails = "年度明细"
        override val allHistory = "全部历史"
        override val ended = "已结束"
        override val inProgress = "进行中"
        override val noRecords = "暂无记录"
        override val weekTab = "周"
        override val monthTab = "月"
        override val yearTab = "年"
        override val allTab = "全部"
        override val currentWeek = "本周"
        override val currentMonth = "本月"
        override val monthTotalCount = "本月总次数"
        override val currentYear = "本年"
        override val historyPeriod = "历史"
        override val selectRange = "选择统计范围"
        override val emptyTitle = "还没有可统计的%s记录"
        override val emptyMessage = "去日历填写第一条记录。"
        override val calendarAction = "去日历填写"
        override val dailyDistribution = "每日分布"
        override val times = "次数"
        override val weeklySummaryTitle = "本周"
        override val weeklyRecordedLabel = "有记录"
        override val weeklyLegendFourPlus = "4次及以上"
        override val weeklyLegendThree = "3次"
        override val weeklyLegendTwo = "2次"
        override val weeklyLegendOne = "1次"
        override val weeklyLegendZero = "0次"
        override val weeklyLegendUnrecorded = "未填写"
        override val weeklyLegendFuture = "未来"
        override val dailyCount = "每日次数"
        override val byDate = "按日期"
        override val countComposition = "次数分布"
        override val explicitZero = "0 次"
        override val once = "1 次"
        override val twice = "2 次"
        override val threePlus = "3 次以上"
        override val unfilledDays = "未填写"
        override val futureDays = "未来日期"
        override val noSavedDays = "本月还没有填写记录"
        override val singleDayExtremes = "单日最高与最低"
        override val byPositiveCount = "仅统计有次数的日期"
        override val maximumDay = "最高单日"
        override val minimumPositiveDay = "最低单日"
        override val noPositiveDay = "本月没有大于 0 次的记录"
        override val future = "未来"
        override val unset = "未填写"
        override val unsetShort = "未填"
        override val dash = "—"
        override val annualCount = "年度次数"
        override val quarterShare = "季度占比"
        override val noPositiveCount = "暂无次数"
        override val byCount = "按次数"
        override val quarterShareHint = "填写次数后显示占比。"
        override val monthSummary = "月份摘要"
        override val fullMonths = "只比较已结束月份"
        override val monthExtremesHint = "已结束月份不足，暂时无法比较。"
        override val maximumMonth = "最高月份"
        override val minimumMonth = "最低月份"
        override val monthAverageFormat = "月均 %.1f 次"

        override fun detailCount(count: Long?): String = if (count == null) "未填写" else "$count 次"

        override fun detailDays(days: Int?): String = if (days == null) "未填写" else "$days 天"

        override fun historyStatus(first: LocalDate?, today: LocalDate): String = first?.let {
            "${it.year}年${it.monthValue}月—${today.year}年${today.monthValue}月"
        } ?: noRecords

        override fun yearTitle(year: Int): String = "${year}年"

        override fun monthTitle(year: Int, month: Int): String = "${year}年 ${month}月"

        override fun monthLabel(month: Int): String = "${month}月"

        override val monthListSeparator = "、"

        override fun dateDescription(date: LocalDate, status: String): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$status"

        override fun weekdayDateLabel(weekday: String, date: LocalDate): String = "$weekday ${date.dayOfMonth}日"

        override fun dayLabel(day: Int): String = "${day}日"

        override fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            if (start.year == end.year) {
                "${start.year}年 ${start.monthValue}月${start.dayOfMonth}日–" +
                    "${end.monthValue}月${end.dayOfMonth}日"
            } else {
                "${start.year}年${start.monthValue}月${start.dayOfMonth}日–${end.year}年${end.monthValue}月${end.dayOfMonth}日"
            }

        override fun yearStatus(end: LocalDate, today: LocalDate): String =
            if (end < today) ended else "截至${today.monthValue}月${today.dayOfMonth}日"

        override fun periodStatus(end: LocalDate, today: LocalDate): String = if (end < today) ended else inProgress

        override fun periodAction(period: String, previous: Boolean): String = when (period) {
            weekTab -> if (previous) "上一周" else "下一周"
            monthTab -> if (previous) "上个月" else "下个月"
            yearTab -> if (previous) "上一年" else "下一年"
            else -> if (previous) "上一段历史" else "下一段历史"
        }

        override fun datePickerDescription(title: String): String = "$selectRange，当前$title"

        override fun emptyTitle(moduleLabel: String): String = emptyTitle.format(moduleLabel)

        override fun periodCountLabel(period: String, moduleLabel: String): String = "$period · ${moduleLabel}次数"

        override fun statisticsLabel(period: String): String = "${period}统计"

        override fun average(value: Double): String = String.format(Locale.US, "%.1f 次/天", value)

        override fun averageNumber(value: Double): String = String.format(Locale.US, "%.1f", value)

        override fun annualAverage(value: Double): String = String.format(Locale.US, monthAverageFormat, value)

        override fun countText(count: Long): String = "$count 次"

        override fun weeklyRecordedDays(recorded: Int, total: Int): String = "$recorded / $total 天"

        override fun weeklyCountSuffix(count: Long): String = "（${count}次）"

        override fun daysText(days: Int): String = "$days 天"

        override fun savedDaysSubtitle(days: Int): String = "已填写 ${days} 天"

        override fun categoryDays(days: Int): String = "$days 天"

        override fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String = when {
            future -> "${dayLabel(day)}，未来日期"
            !recorded -> "${dayLabel(day)}，未填写"
            else -> "${dayLabel(day)}，${countText(count ?: 0L)}"
        }

        override fun monthDailyChartAccessibility(days: String): String = "每日次数图：$days"

        override fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String =
            "本月总次数${countText(totalCount)}；发生天数${daysText(recordedDays)}；日均次数${average(average)}"

        override fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String =
            "次数分布：已填写 $savedDays 天；0 次 $explicitZeroDays 天；1 次 $oneCountDays 天；" +
                "2 次 $twoCountDays 天；3 次以上 $threePlusCountDays 天；" +
                "未填写 $unfilledDays 天；未来日期 $futureDays 天"

        override fun monthExtremeAccessibility(label: String, count: Long): String =
            "$label，${countText(count)}"

        override fun percentage(value: Double): String = String.format(Locale.US, "%.0f%%", value)

        override fun quarterLabel(quarter: Int): String = "Q$quarter"

        override fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            "${month}月 ${chartMonthValue(isFuture, recorded, count)}"

        override fun totalCountAccessibility(total: Long, quarters: String): String =
            "季度占比：总次数 $total 次；$quarters"

        override fun annualChartAccessibility(months: String): String = "年度次数折线图：$months"

        override fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String = when {
            isFuture -> future
            !recorded -> unset
            else -> countText(count ?: 0L)
        }.toString()
    }
