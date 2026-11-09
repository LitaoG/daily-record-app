package io.github.litaog.dailyrecord.core.common

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import java.time.LocalDate

internal object ZhRecordStrings : RecordStrings {
        override val loading = "正在读取…"
        override val saving = "正在保存…"
        override val saved = "已保存"
        override val save = "保存记录"
        override val saveFailure = "保存失败，请重试"
        override val clear = "清除记录"
        override val clearFailure = "清除失败，请重试"
        override val countOnly = "只记录次数"
        override val countFirst = "先记录次数"
        override val countAndDetails = "记录次数与每次详情"
        override val detailEntry = "记录时间和感受"
        override val detailEntryHintFormat = "为这 %d 次补充详情"
        override val detailSectionTitle = "本次详情"
        override val detailSectionHint = "每增加 1 次，自动新增一条"
        override val detailCollapse = "收起详情"
        override val detailExpand = "展开详情"
        override val detailOccurrenceFormat = "第 %d 次"
        override val detailStartTime = "开始"
        override val detailEndTime = "结束"
        override val detailStartTimeUnset = "开始时间"
        override val detailEndTimeUnset = "结束时间"
        override val detailTimeUnset = "选择时间"
        override val detailTimePickerTitle = "选择时间"
        override val detailTimePickerSubtitle = "上下滑动选择小时和分钟"
        override val detailTimePickerHour = "小时"
        override val detailTimePickerMinute = "分钟"
        override val detailTimePickerHint = "滚动停止后会自动对齐"
        override val detailTimePickerConfirm = "确定"
        override val detailWriteFeeling = "写感受"
        override val detailCollapseFeeling = "收起"
        override val detailFeelingLabel = "感受（可选）"
        override val detailFeelingHint = "写下这一刻的感受"
        override val detailFeelingCounter = "%d / ${MAX_RECORD_DETAIL_FEELING_CHARACTERS}"
        override val detailEndBeforeStart = "结束时间不能早于开始时间"
        override val detailDiscardTitle = "移除这次详情？"
        override val detailDiscardMessage = "这次已填写的时间或感受会一起移除。"
        override val detailConfirmRemove = "移除详情"
        override val detailEntryUnavailable = "详情过多，仅编辑总次数，当前次数内的已有详情会保留"
        override val loadingRecords = "正在读取记录…"
        override val futureUnavailable = "未来日期，不能记录"
        override val notSaved = "尚未填写"
        override val zeroRecorded = "已记录 · 0 次"
        override val clearTitle = "清除这天的记录？"
        override val clearSubtitle = "只影响当前模块"
        override val clearMessage = "清除后会恢复为“未填写”，不会计入统计。"
        override val confirmClear = "确认清除"
        override val clearDetailsFailure = "清除详情失败，请重试"
        override val clearDetailsTitle = "清除本次详情？"
        override val clearDetailsSubtitle = "只清除时间和感受"
        override val clearDetailsMessage = "清除后当天次数保持不变，时间和感受会被移除。"
        override val confirmClearDetails = "确认清除详情"
        override val discardTitle = "放弃未保存的修改？"
        override val unsavedSubtitle = "当前次数或详情尚未保存"
        override val discardMessage = "返回日历后，这次次数或详情修改会丢失。"
        override val continueEditing = "继续编辑"
        override val discard = "放弃修改"
        override val backToCalendar = "返回日历"

        override fun savedStatus(count: Int): String = "待保存 · $count 次"

        override fun recordedStatus(count: Int): String = "已记录 · $count 次"

        override fun explicitZeroHint(text: String): String = "填 0 表示$text，会保留记录。"

        override fun monthSaved(month: Int): String = "${month}月记录"

        override fun monthSummary(count: Long, days: Int): String = "$count 次 · $days 天有记录"

        override fun moduleRecordLabel(moduleLabel: String): String = "${moduleLabel}记录"

        override fun dateLabel(date: LocalDate, weekday: String): String =
            "${date.monthValue}月${date.dayOfMonth}日 · $weekday"

        override fun detailEntryHint(count: Int): String = detailEntryHintFormat.format(count)

        override fun detailOccurrence(index: Int): String = detailOccurrenceFormat.format(index)

        override fun detailFeelingCounter(count: Int): String = detailFeelingCounter.format(count)

        override fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            "${detailOccurrence(occurrence)}，$label，$value"

        override fun detailTimeWheelCurrent(unit: String, value: String): String =
            "$unit，当前 $value"

        override fun detailTimeWheelOption(unit: String, value: String): String =
            "选择$unit $value"

        override fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            "${detailOccurrence(occurrence)}，$action"

        override fun detailFeelingEditorDescription(occurrence: Int): String =
            "${detailOccurrence(occurrence)}，$detailFeelingLabel"
    }

internal object ZhElegantRecordModule : RecordModuleStrings {
        override val handBrewLabel = "个人私密"
        override val handBrewAccessibilityLabel = "个人私密"
        override val handBrewQuestionToday = "今天个人私密了几次？"
        override val handBrewQuestionPast = "当天个人私密了几次？"
        override val handBrewZero = "当天没有个人私密"
        override val sexLabel = "伴侣亲密"
        override val sexAccessibilityLabel = "伴侣亲密"
        override val sexQuestionToday = "今天伴侣亲密了几次？"
        override val sexQuestionPast = "当天伴侣亲密了几次？"
        override val sexZero = "当天没有伴侣亲密"

        override fun recordLabel(label: String): String = "${label}记录"
    }
