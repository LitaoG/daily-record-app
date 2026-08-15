package io.github.litaog.dailyrecord.core.common

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Single source for user-facing copy used by both the UI and sync layer.
 *
 * Keeping copy out of individual Composables makes wording changes auditable and
 * prevents the two record modules from drifting. Dynamic copy stays here as
 * small functions so callers only provide data, never assemble user-facing
 * fragments themselves.
 */
internal object AppCopy {
    const val privateRecordSubtitle = "记录每天的次数"
    const val offlineSubtitle = "本机记录可离线使用"
    const val vpnSyncFailure = "请打开 VPN（梯子）后重试；记录仍在本机。"
    const val readingLocalRecords = "正在读取本机记录"

    const val selected = "已选择"
    const val unselected = "未选择"
    const val today = "今天"
    const val historyDate = "历史日期"
    const val futureDate = "未来日期"
    private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun weekdayName(dayOfWeek: Int): String {
        require(dayOfWeek in 1..7) { "dayOfWeek must be in 1..7, was $dayOfWeek" }
        return weekdayNames[dayOfWeek - 1]
    }

    fun selectedState(label: String, isSelected: Boolean): String =
        "$label，${if (isSelected) selected else unselected}"

    object Auth {
        const val signIn = "登录"
        const val register = "注册"
        const val email = "邮箱"
        const val password = "密码"
        const val confirmPassword = "再次输入密码"
        const val show = "显示"
        const val hide = "隐藏"
        const val showPassword = "显示密码"
        const val hidePassword = "隐藏密码"
        const val forgotPassword = "忘记密码？"
        const val openPasswordReset = "打开重置密码"
        const val title = "私密日历"
        const val signInSubtitle = "登录后可恢复云端记录"
        const val registerSubtitle = "注册后可保存数据到云端长久记录"
        const val signInLocalSyncNotice = "若先行选择“本机记录”，记录的数据会在登录后自动同步到账号"
        const val registerLocalSyncNotice = "若先行选择“本机记录”，记录的数据会在注册后自动同步到账号"
        const val passwordPolicy = "密码至少 8 位"
        const val signInVpnNotice = "需使用 VPN 进行登录\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动"
        const val registerVpnNotice = "需使用 VPN 进行注册\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动"
        const val emulatorNotice = "当前仅连接本地测试环境，云同步不可用。"
        const val wait = "请稍候…"
        const val signInAndRestore = "登录并恢复记录"
        const val createAccount = "创建账号"
        const val continueOffline = "暂不登录，先使用“本机记录”"
        const val continueOfflineRegister = "暂不注册，先使用“本机记录”"
        const val emailRequired = "请输入邮箱"
        const val emailInvalid = "请输入有效邮箱"
        const val passwordTooShort = "密码至少需要 8 位"
        const val passwordMismatch = "两次输入的密码不一致"
        const val network = "网络不可用或连接云服务超时，请检查 VPN（梯子）后重试。"
        const val emailAlreadyRegistered = "此邮箱已注册，请直接登录"
        const val weakPassword = "密码强度不足，请使用更长的密码。"
        const val tooManyRequests = "尝试次数过多，请稍后再试"
        const val invalidCredentials = "邮箱或密码错误，请检查后重试。"
        const val signInUnavailable = "暂时无法登录，请稍后重试"
        const val registerUnavailable = "暂时无法创建账号，请稍后重试"
        const val resetSentTitle = "请查收邮件"
        const val resetTitle = "重置密码"
        const val resetSentSubtitle = "如果邮箱已注册，重置邮件已发送。为保护隐私，页面不会显示账号是否存在。"
        const val resetSubtitle = "输入注册邮箱，我们会发送重置邮件。"
        const val resetSuccess = "请检查收件箱和垃圾邮件，并按邮件提示修改密码。"
        const val backToSignIn = "返回登录"
        const val sendResetEmail = "发送重置邮件"
        const val sendingResetEmail = "正在发送…"
        const val cancel = "取消"
        const val resetNetwork = "网络不可用，重置邮件未发送。请打开 VPN（梯子）后重试。"
        const val resetTooManyRequests = "请求过于频繁，请稍后再试。"
        const val resetQuotaExceeded = "今日发送额度已用完，请稍后再试。"
        const val resetUnavailable = "暂时无法发送重置邮件，请稍后重试。"
    }

    object Account {
        const val signInSync = "登录并同步"
        const val reSignIn = "重新登录"
        const val signInSyncAccessibility = "登录账号并同步记录"
        const val accountAndSync = "账号与云同步"
        const val signOutConfirm = "确认退出登录？"
        const val cloudDataRetained = "云端记录仍会保留"
        const val restoreOnAnotherDevice = "换手机登录后即可恢复记录"
        const val signOutMessage = "退出登录不会删除云端记录；下次登录后仍可继续同步。"
        const val back = "返回"
        const val confirmSignOut = "确认退出"
        const val syncDescription = "记录会先保存在本机，联网后自动同步；换手机登录即可恢复。"
        const val syncing = "正在同步"
        const val syncNow = "立即同步"
        const val close = "关闭"
        const val signOut = "退出登录"
        const val deleteAccount = "删除账号与云端数据"
        const val notConfigured = "云同步未配置"
        const val offline = "当前离线，记录已保存在本机"
        const val synced = "云端已同步"
        const val shortNotConfigured = "未连接"
        const val shortOffline = "离线"
        const val shortSyncing = "同步中"
        const val shortSynced = "已同步"
        const val shortRetry = "同步失败"
        const val networkFailureTitle = "网络连接异常"
        const val networkFailureGuidance = "请检查网络或 VPN（梯子），然后重试。"
        const val authFailureTitle = "登录状态已失效"
        const val authFailureGuidance = "请重新登录账号，然后再次同步本机记录。"
        const val permissionFailureTitle = "账号无权访问云端数据"
        const val permissionFailureGuidance = "请重新登录；如果仍然失败，请稍后重试或联系开发者。"
        const val quotaFailureTitle = "云服务暂时无法处理请求"
        const val quotaFailureGuidance = "本机记录不会丢失，请稍后再试。"
        const val serviceFailureTitle = "云服务暂时不可用"
        const val serviceFailureGuidance = "云服务暂时异常；本机记录不会丢失，请稍后再试。"
        const val dataFailureTitle = "部分记录无法同步"
        const val dataFailureGuidance = "未同步的记录仍在本机，请稍后再试。"
        const val unknownFailureTitle = "暂时无法完成同步"
        const val unknownFailureGuidance = "记录仍在本机，请稍后再试。"
        const val syncDialogMessage = "请检查网络或 VPN（梯子），然后重试。"
        const val dataFormatFailure = "部分云端记录无法读取，其余记录已同步。"
        fun timeoutFailure(timeoutMillis: Long): String =
            "等待云服务超过 ${(timeoutMillis / 1_000L).coerceAtLeast(1)} 秒，已停止同步；记录仍在本机。"
        const val networkFailure = "网络连接异常，记录仍在本机。"
        const val authFailure = "登录状态已失效，记录仍在本机。"
        const val permissionFailure = "账号暂时无法访问云端，记录仍在本机。"
        const val quotaFailure = "云服务暂时无法处理同步，记录仍在本机。"
        const val serviceFailure = "云服务暂时异常，记录仍在本机。"
        const val dataFailure = "部分记录暂未同步，记录仍在本机。"
        const val unknownFailure = "同步未完成，记录仍在本机。"

        fun pending(count: Int): String = "有 $count 条记录待同步"
        fun shortPending(count: Int): String = "待同步 $count 条"
        fun syncChipDescription(status: String): String = "账号与云同步状态：$status"
    }

    object RecordModule {
        const val handBrewLabel = "自慰"
        const val handBrewQuestionToday = "今天自慰了几次？"
        const val handBrewQuestionPast = "当天自慰了几次？"
        const val handBrewZero = "当天没有自慰"
        const val sexLabel = "做爱"
        const val sexQuestionToday = "今天做爱了几次？"
        const val sexQuestionPast = "当天做爱了几次？"
        const val sexZero = "当天没有做爱"
        fun recordLabel(label: String): String = "${label}记录"
    }

    object Deletion {
        const val warningTitle = "删除账号与云端数据？"
        const val confirmationTitle = "确认永久删除？"
        const val irreversible = "此操作无法撤销"
        const val verifyPassword = "输入当前密码验证身份"
        const val warningMessage = "继续后会先验证密码，再永久删除账号和全部云端记录。"
        const val localChoice = "选择是否保留本机记录"
        const val keepLocalTitle = "保留本机记录（推荐）"
        const val keepLocalDescription = "删除账号后继续离线使用这些记录"
        const val deleteLocalTitle = "同时删除本机记录"
        const val deleteLocalDescription = "云端和这台手机都不再保留这些记录"
        const val continueVerification = "继续验证身份"
        const val currentPassword = "当前密码"
        const val deleting = "正在删除…"
        const val deletePermanently = "永久删除账号"
        const val confirmationKeepLocal = "云端记录和账号会永久删除；本机记录将转为离线记录。"
        const val confirmationDeleteLocal = "云端记录、账号和本机记录都会永久删除。"
        const val networkError = "网络中断，删除未完成。本机记录仍保留，请打开 VPN（梯子）后重试。"
        const val networkAuthError = "网络不可用，请打开 VPN（梯子）后重试。"
        const val authError = "登录状态已失效，请重新登录后再删除。"
        const val permissionError = "账号暂时无权删除；本机记录仍保留，请重新登录后重试。"
        const val serviceError = "云服务暂时不可用；本机记录仍保留，请稍后重试。"
        const val unknownError = "删除未完成，本机记录仍保留。部分云端记录可能已删除，请重试。"
        const val localCleanupPending = "账号和云端数据已删除，但本机记录清理未完成，将在下次启动时自动完成。"
        const val authDeletionPending = "删除请求的最终结果暂时无法确认；本机记录已保留，云同步已暂停。请保持网络可用后重新打开应用。"
        const val retryRecovery = "重试恢复"
        const val recoveryConflict = "本机已有记录，恢复副本未覆盖；请先处理现有本机记录后再重试恢复。"
        const val recoveryRetryGuidance = "恢复操作暂时未完成；本机数据和恢复副本均已保留。请检查网络后重试。"
        const val replaceLocalAndRestore = "删除现有本机记录并恢复"
        const val replaceLocalAndRestoreTitle = "删除现有本机记录？"
        const val replaceLocalAndRestoreMessage = "这会删除当前本机空间中的记录，再恢复已删除账号的本机副本。此操作不可撤销。"
        const val cancelRecoveryReplacement = "取消"
        const val wrongPassword = "密码不正确，请重新输入"
        const val tooManyAttempts = "尝试次数过多，请稍后再试"

        const val localRecoveryPending = "本机恢复副本清理未完成，云服务同步已暂停。请保持网络可用后重新打开应用。"

        fun selectionDescription(title: String, isSelected: Boolean): String =
            "$title，${if (isSelected) AppCopy.selected else AppCopy.unselected}"
    }

    object Calendar {
        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        const val recordHint = "点击日期填写次数"
        const val unset = "未填"
        const val future = "未来"
        const val zero = "0 次"
        const val recorded = "已记录"
        const val previousMonth = "上个月"
        const val nextMonth = "下个月"
        const val selectDate = "选择年份和日期"
        const val backToToday = "回到今天"
        const val legendDescription = "点击日期填写%s次数。状态包括：未填写、未来不可填写、0 次和已记录"
        const val unavailable = "不可用"
        const val oneTime = "1 次"
        const val twoTimes = "2 次"
        const val ninePlusTimes = "9 次以上"
        const val todayShort = "今"
        const val futureDescription = "未来日期，不可记录"
        const val unsetDescription = "未填写"
        const val zeroDescription = "记录为 0 次"
        const val selectedSuffix = "，已选择"
        const val todaySuffix = "，今天"

        fun monthSummary(count: Long, days: Int): String = "本月 $count 次 · $days 天有记录"
        fun monthTitle(month: YearMonth): String = "${month.year}年 ${month.monthValue}月"
        fun monthTitleMultiline(month: YearMonth): String = "${month.year}年\n${month.monthValue}月"
        fun monthSelectionDescription(month: YearMonth): String =
            "$selectDate，当前${month.year}年${month.monthValue}月"
        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$state${if (focused) selectedSuffix else ""}"
        fun legendDescription(moduleLabel: String): String = legendDescription.format(moduleLabel)
        fun countDescription(count: Int): String = when (count) {
            0 -> zero
            1 -> oneTime
            2 -> twoTimes
            in 3..8 -> "${count} 次"
            else -> ninePlusTimes
        }
        fun statusDescription(
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
                // The same count bucketing as the visual cell keeps TalkBack
                // and screen text consistent (9+ is never read as exact 9).
                else -> "$moduleLabel，${countDescription(count)}"
            }
            return if (date == today) "$status$todaySuffix" else status
        }
    }

    object Navigation {
        val weekdays = Calendar.weekdays
        const val title = "快速跳转"
        const val subtitle = "直接选择年份和日期"
        const val dateWheelSubtitle = "选择日期"
        const val monthSubtitle = "直接选择年份和月份"
        const val jumpToDate = "跳转到此日"
        const val jumpToMonth = "跳转到此月"
        const val jumpToYear = "跳转到此年"
        const val selected = "已选择"
        const val switchYear = "切换年份"
        const val returnToDatePicker = "返回日期选择"
        const val selectYear = "选择年份"
        const val selectMonth = "选择月份"
        const val dateWheelHint = "上下滑动调整日期"
        const val yearUnit = "年"
        const val monthUnit = "月"
        const val dayUnit = "日"
        private val monthNames = listOf(
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月",
        )
        fun switchYearDescription(year: Int): String = "$switchYear，当前${year}年"
        fun nextYearDescription(forward: Boolean): String = if (forward) "跳转到下一年" else "跳转到上一年"
        fun dateText(date: LocalDate): String =
            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))
        fun dateLabel(date: LocalDate, weekday: String): String =
            "${dateText(date)} · $weekday"
        fun selectYearDescription(year: Int): String = "选择${year}年"
        fun yearTitle(year: Int): String = "${year}年"
        fun monthTitle(month: YearMonth): String = "${month.year}年 ${month.monthValue}月"
        fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "${month}月" }
        fun dayLabel(day: Int): String = "${day}日"
        fun monthDescription(month: YearMonth): String = "选择${month.year}年${month.monthValue}月"
    }

    object Record {
        const val loading = "正在读取…"
        const val saving = "正在保存…"
        const val saved = "已保存"
        const val save = "保存记录"
        const val saveFailure = "保存失败，请重试"
        const val clear = "清除记录"
        const val clearFailure = "清除失败，请重试"
        const val countOnly = "只记录次数"
        const val countFirst = "先记录次数"
        const val countAndDetails = "记录次数与每次详情"
        const val detailEntry = "记录时间和感受"
        const val detailEntryHintFormat = "为这 %d 次补充详情"
        const val detailSectionTitle = "本次详情"
        const val detailSectionHint = "每增加 1 次，自动新增一条"
        const val detailCollapse = "收起详情"
        const val detailExpand = "展开详情"
        const val detailOccurrenceFormat = "第 %d 次"
        const val detailStartTime = "开始"
        const val detailEndTime = "结束"
        const val detailStartTimeUnset = "开始时间"
        const val detailEndTimeUnset = "结束时间"
        const val detailTimeUnset = "选择时间"
        const val detailTimePickerTitle = "选择时间"
        const val detailTimePickerSubtitle = "上下滑动选择小时和分钟"
        const val detailTimePickerHour = "小时"
        const val detailTimePickerMinute = "分钟"
        const val detailTimePickerHint = "滚动停止后会自动对齐"
        const val detailTimePickerConfirm = "确定"
        const val detailWriteFeeling = "写感受"
        const val detailCollapseFeeling = "收起"
        const val detailFeelingLabel = "感受（可选）"
        const val detailFeelingHint = "写下这一刻的感受"
        // The counter denominator must track the validation limit so the
        // displayed "N / 100" never drifts from the accepted length.
        val detailFeelingCounter = "%d / ${MAX_RECORD_DETAIL_FEELING_CHARACTERS}"
        const val detailEndBeforeStart = "结束时间不能早于开始时间"
        const val detailDiscardTitle = "移除这次详情？"
        const val detailDiscardMessage = "这次已填写的时间或感受会一起移除。"
        const val detailConfirmRemove = "移除详情"

        const val detailEntryUnavailable = "详情过多，仅编辑总次数，当前次数内的已有详情会保留"

        fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            "${detailOccurrence(occurrence)}，$label，$value"

        fun detailTimeWheelCurrent(unit: String, value: String): String =
            "$unit，当前 $value"

        fun detailTimeWheelOption(unit: String, value: String): String =
            "选择$unit $value"

        fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            "${detailOccurrence(occurrence)}，$action"

        fun detailFeelingEditorDescription(occurrence: Int): String =
            "${detailOccurrence(occurrence)}，$detailFeelingLabel"
        const val loadingRecords = "正在读取记录…"
        const val futureUnavailable = "未来日期，不能记录"
        const val notSaved = "尚未填写"
        const val zeroRecorded = "已记录 · 0 次"
        const val clearTitle = "清除这天的记录？"
        const val clearSubtitle = "只影响当前模块"
        const val clearMessage = "清除后会恢复为“未填写”，不会计入统计。"
        const val confirmClear = "确认清除"
        const val clearDetailsFailure = "清除详情失败，请重试"
        const val clearDetailsTitle = "清除本次详情？"
        const val clearDetailsSubtitle = "只清除时间和感受"
        const val clearDetailsMessage = "清除后当天次数保持不变，时间和感受会被移除。"
        const val confirmClearDetails = "确认清除详情"
        const val discardTitle = "放弃未保存的修改？"
        const val unsavedSubtitle = "当前次数或详情尚未保存"
        const val discardMessage = "返回日历后，这次次数或详情修改会丢失。"
        const val continueEditing = "继续编辑"
        const val discard = "放弃修改"
        const val backToCalendar = "返回日历"
        fun savedStatus(count: Int): String = "待保存 · $count 次"
        fun recordedStatus(count: Int): String = "已记录 · $count 次"
        fun explicitZeroHint(text: String): String = "填 0 表示$text，会保留记录。"
        fun monthSaved(month: Int): String = "${month}月记录"
        fun monthSummary(count: Long, days: Int): String = "$count 次 · $days 天有记录"
        fun moduleRecordLabel(moduleLabel: String): String = "${moduleLabel}记录"
        fun dateLabel(date: LocalDate, weekday: String): String =
            "${date.monthValue}月${date.dayOfMonth}日 · $weekday"
        fun detailEntryHint(count: Int): String = detailEntryHintFormat.format(count)
        fun detailOccurrence(index: Int): String = detailOccurrenceFormat.format(index)
        fun detailFeelingCounter(count: Int): String = detailFeelingCounter.format(count)
    }

    object Statistics {
        val weekdays = Calendar.weekdays
        const val title = "统计"
        const val countAndDays = "次数 · 天数"
        const val countUnit = "次"
        const val dayUnit = "天"
        const val perDayUnit = "次/天"
        const val recordedDaysLabel = "发生天数"
        const val averageLabel = "日均次数"
        const val dailyDetails = "每日明细"
        const val monthlyDetails = "每月明细"
        const val yearlyDetails = "年度明细"
        const val allHistory = "全部历史"
        const val ended = "已结束"
        const val inProgress = "进行中"
        const val noRecords = "暂无记录"
        const val weekTab = "周"
        const val monthTab = "月"
        const val yearTab = "年"
        const val allTab = "全部"
        const val currentWeek = "本周"
        const val currentMonth = "本月"
        const val monthTotalCount = "本月总次数"
        const val currentYear = "本年"
        const val historyPeriod = "历史"
        const val selectRange = "选择统计范围"
        const val emptyTitle = "还没有可统计的%s记录"
        const val emptyMessage = "去日历填写第一条记录。"
        const val calendarAction = "去日历填写"
        const val dailyDistribution = "每日分布"
        const val times = "次数"
        const val weeklySummaryTitle = "本周"
        const val weeklyRecordedLabel = "有记录"
        const val weeklyLegendFourPlus = "4次及以上"
        const val weeklyLegendThree = "3次"
        const val weeklyLegendTwo = "2次"
        const val weeklyLegendOne = "1次"
        const val weeklyLegendZero = "0次"
        const val weeklyLegendUnrecorded = "未填写"
        const val weeklyLegendFuture = "未到"
        const val dailyCount = "每日次数"
        const val byDate = "按日期"
        const val countComposition = "次数分布"
        const val explicitZero = "0 次"
        const val once = "1 次"
        const val twice = "2 次"
        const val threePlus = "3 次以上"
        const val unfilledDays = "未填写"
        const val futureDays = "未来日期"
        const val noSavedDays = "本月还没有填写记录"
        const val singleDayExtremes = "单日最高与最低"
        const val byPositiveCount = "仅统计有次数的日期"
        const val maximumDay = "最高单日"
        const val minimumPositiveDay = "最低单日"
        const val noPositiveDay = "本月没有大于 0 次的记录"
        const val future = "未来"
        const val unset = "未填写"
        const val unsetShort = "未填"
        const val dash = "—"
        const val annualCount = "年度次数"
        const val quarterShare = "季度占比"
        const val noPositiveCount = "暂无次数"
        const val byCount = "按次数"
        const val quarterShareHint = "填写次数后显示占比。"
        const val monthSummary = "月份摘要"
        const val fullMonths = "只比较已结束月份"
        const val monthExtremesHint = "已结束月份不足，暂时无法比较。"
        const val maximumMonth = "最高月份"
        const val minimumMonth = "最低月份"
        const val monthAverageFormat = "月均 %.1f 次"
        fun detailCount(count: Long?): String = if (count == null) "未填写" else "$count 次"
        fun detailDays(days: Int?): String = if (days == null) "未填写" else "$days 天"
        fun historyStatus(first: LocalDate?, today: LocalDate): String = first?.let {
            "${it.year}年${it.monthValue}月—${today.year}年${today.monthValue}月"
        } ?: noRecords
        fun yearTitle(year: Int): String = "${year}年"
        fun monthTitle(year: Int, month: Int): String = "${year}年 ${month}月"
        fun monthLabel(month: Int): String = "${month}月"
        fun dateDescription(date: LocalDate, status: String): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$status"
        fun weekdayDateLabel(weekday: String, date: LocalDate): String = "$weekday ${date.dayOfMonth}日"
        fun dayLabel(day: Int): String = "${day}日"
        fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            if (start.year == end.year) {
                "${start.year}年 ${start.monthValue}月${start.dayOfMonth}日–" +
                    "${end.monthValue}月${end.dayOfMonth}日"
            } else {
                "${start.year}年${start.monthValue}月${start.dayOfMonth}日–${end.year}年${end.monthValue}月${end.dayOfMonth}日"
            }
        fun yearStatus(end: LocalDate, today: LocalDate): String =
            if (end < today) ended else "截至${today.monthValue}月${today.dayOfMonth}日"
        fun periodStatus(end: LocalDate, today: LocalDate): String = if (end < today) ended else inProgress
        fun periodAction(period: String, previous: Boolean): String = when (period) {
            weekTab -> if (previous) "上一周" else "下一周"
            monthTab -> if (previous) "上个月" else "下个月"
            yearTab -> if (previous) "上一年" else "下一年"
            else -> if (previous) "上一段历史" else "下一段历史"
        }
        fun datePickerDescription(title: String): String = "$selectRange，当前$title"
        fun emptyTitle(moduleLabel: String): String = Statistics.emptyTitle.format(moduleLabel)
        fun periodCountLabel(period: String, moduleLabel: String): String = "$period · ${moduleLabel}次数"
        fun statisticsLabel(period: String): String = "${period}统计"
        fun average(value: Double): String = String.format(java.util.Locale.US, "%.1f 次/天", value)
        fun averageNumber(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
        fun annualAverage(value: Double): String = String.format(java.util.Locale.US, monthAverageFormat, value)
        fun countText(count: Long): String = "$count 次"
        fun weeklyRecordedDays(recorded: Int, total: Int): String = "$recorded / $total 天"
        fun weeklyCountSuffix(count: Long): String = "（${count}次）"
        fun daysText(days: Int): String = "$days 天"
        fun savedDaysSubtitle(days: Int): String = "已填写 ${days} 天"
        fun categoryDays(days: Int): String = "$days 天"
        fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String = when {
            future -> "${dayLabel(day)}，未来日期"
            !recorded -> "${dayLabel(day)}，未填写"
            else -> "${dayLabel(day)}，${countText(count ?: 0L)}"
        }
        fun monthDailyChartAccessibility(days: String): String = "每日次数图：$days"
        fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String =
            "本月总次数${countText(totalCount)}；发生天数${daysText(recordedDays)}；日均次数${average(average)}"
        fun monthCompositionAccessibility(
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
        fun monthExtremeAccessibility(label: String, count: Long): String =
            "$label，${countText(count)}"
        fun percentage(value: Double): String = String.format(java.util.Locale.US, "%.0f%%", value)
        fun quarterLabel(quarter: Int): String = "Q$quarter"
        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            "${month}月 ${chartMonthValue(isFuture, recorded, count)}"
        fun totalCountAccessibility(total: Long, quarters: String): String = "季度占比：总次数 $total 次；$quarters"
        fun annualChartAccessibility(months: String): String = "年度次数折线图：$months"
        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String = when {
            isFuture -> future
            !recorded -> unset
            else -> countText(count ?: 0L)
        }.toString()
    }

    object Settings {
        const val title = "设置"
        const val open = "打开设置"
        const val back = "返回主页"
        const val accountSection = "账号与同步"
        const val localAccountTitle = "本机记录"
        const val localAccountSummary = "记录只保存在这台设备；登录后可同步并在换机时恢复。"
        const val signedInAccountSummary = "查看同步状态、手动同步或管理账号"
        const val dataSection = "数据与隐私"
        const val localFirstTitle = "本机优先"
        const val localFirstSummary = "所有记录先保存在本机；未登录时不会上传。"
        const val privacyTitle = "隐私保护"
        const val privacySummary = "不含广告、分析或崩溃上报 SDK"
        const val aboutSection = "关于"
        const val version = "版本"
        const val license = "开源许可"
        const val licenseValue = "Apache 2.0"

        fun accountDescription(title: String, status: String): String = "$title，$status"
    }

    object NavigationBar {
        const val calendar = "日历"
        const val statistics = "统计"
    }

    object Components {
        const val decrease = "减少一次"
        const val increase = "增加一次"

        /** Joins TalkBack-visible parts with the app's fixed semantics separator. */
        fun joinSemantics(vararg parts: String): String = parts.joinToString(SEMANTICS_SEPARATOR)

        fun joinSemantics(parts: Iterable<String>): String =
            parts.joinToString(SEMANTICS_SEPARATOR)
    }

    /** Locale used for user-visible date/weekday formatting. */
    val DISPLAY_LOCALE: Locale = Locale.SIMPLIFIED_CHINESE

    /** TalkBack-visible list separator used across app semantic descriptions. */
    const val SEMANTICS_SEPARATOR = "，"
}
