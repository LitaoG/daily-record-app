package io.github.litaog.dailyrecord.core.common

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Chinese copy set. Wording is identical to the historical AppCopy source. */
internal object ZhStrings : AppStrings {
    override val privateRecordSubtitle = "记录每天的次数"
    override val offlineSubtitle = "本机记录可离线使用"
    override val vpnSyncFailure = "请打开 VPN（梯子）后重试；记录仍在本机。"
    override val readingLocalRecords = "正在读取本机记录"
    override val selected = "已选择"
    override val unselected = "未选择"
    override val today = "今天"
    override val historyDate = "历史日期"
    override val futureDate = "未来日期"
    override val displayLocale: Locale = Locale.SIMPLIFIED_CHINESE
    override val semanticsSeparator = "，"

    override val auth: AppStrings.AuthStrings = Auth
    override val account: AppStrings.AccountStrings = Account
    override val recordModule: AppStrings.RecordModuleStrings = RecordModule
    override val deletion: AppStrings.DeletionStrings = Deletion
    override val calendar: AppStrings.CalendarStrings = Calendar
    override val navigation: AppStrings.NavigationStrings = Navigation
    override val record: AppStrings.RecordStrings = Record
    override val statistics: AppStrings.StatisticsStrings = Statistics
    override val settings: AppStrings.SettingsStrings = Settings
    override val navigationBar: AppStrings.NavigationBarStrings = NavigationBar
    override val components: AppStrings.ComponentStrings = Components

    private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    override fun weekdayName(dayOfWeek: Int): String {
        require(dayOfWeek in 1..7) { "dayOfWeek must be in 1..7, was $dayOfWeek" }
        return weekdayNames[dayOfWeek - 1]
    }

    override fun selectedState(label: String, isSelected: Boolean): String =
        "$label，${if (isSelected) selected else unselected}"

    override fun joinSemantics(vararg parts: String): String =
        parts.joinToString(semanticsSeparator)

    override fun joinSemantics(parts: Iterable<String>): String =
        parts.joinToString(semanticsSeparator)

    private object Auth : AppStrings.AuthStrings {
        override val signIn = "登录"
        override val register = "注册"
        override val email = "邮箱"
        override val password = "密码"
        override val confirmPassword = "再次输入密码"
        override val show = "显示"
        override val hide = "隐藏"
        override val showPassword = "显示密码"
        override val hidePassword = "隐藏密码"
        override val forgotPassword = "忘记密码？"
        override val openPasswordReset = "打开重置密码"
        override val title = "私密日历"
        override val signInSubtitle = "登录后可恢复云端记录"
        override val registerSubtitle = "注册后可保存数据到云端长久记录"
        override val signInLocalSyncNotice = "若先行选择“本机记录”，记录的数据会在登录后自动同步到账号"
        override val registerLocalSyncNotice = "若先行选择“本机记录”，记录的数据会在注册后自动同步到账号"
        override val passwordPolicy = "密码至少 8 位"
        override val signInVpnNotice = "需使用 VPN 进行登录\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动"
        override val registerVpnNotice = "需使用 VPN 进行注册\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动"
        override val emulatorNotice = "当前仅连接本地测试环境，云同步不可用。"
        override val wait = "请稍候…"
        override val signInAndRestore = "登录并恢复记录"
        override val createAccount = "创建账号"
        override val continueOffline = "暂不登录，先使用“本机记录”"
        override val continueOfflineRegister = "暂不注册，先使用“本机记录”"
        override val emailRequired = "请输入邮箱"
        override val emailInvalid = "请输入有效邮箱"
        override val passwordTooShort = "密码至少需要 8 位"
        override val passwordMismatch = "两次输入的密码不一致"
        override val network = "网络不可用或连接云服务超时，请检查 VPN（梯子）后重试。"
        override val emailAlreadyRegistered = "此邮箱已注册，请直接登录"
        override val weakPassword = "密码强度不足，请使用更长的密码。"
        override val tooManyRequests = "尝试次数过多，请稍后再试"
        override val invalidCredentials = "邮箱或密码错误，请检查后重试。"
        override val signInUnavailable = "暂时无法登录，请稍后重试"
        override val registerUnavailable = "暂时无法创建账号，请稍后重试"
        override val resetSentTitle = "请查收邮件"
        override val resetTitle = "重置密码"
        override val resetSentSubtitle = "如果邮箱已注册，重置邮件已发送。为保护隐私，页面不会显示账号是否存在。"
        override val resetSubtitle = "输入注册邮箱，我们会发送重置邮件。"
        override val resetSuccess = "请检查收件箱和垃圾邮件，并按邮件提示修改密码。"
        override val backToSignIn = "返回登录"
        override val sendResetEmail = "发送重置邮件"
        override val sendingResetEmail = "正在发送…"
        override val cancel = "取消"
        override val resetNetwork = "网络不可用，重置邮件未发送。请打开 VPN（梯子）后重试。"
        override val resetTooManyRequests = "请求过于频繁，请稍后再试。"
        override val resetQuotaExceeded = "今日发送额度已用完，请稍后再试。"
        override val resetUnavailable = "暂时无法发送重置邮件，请稍后重试。"
    }

    private object Account : AppStrings.AccountStrings {
        override val signInSync = "登录并同步"
        override val reSignIn = "重新登录"
        override val signInSyncAccessibility = "登录账号并同步记录"
        override val accountAndSync = "账号与云同步"
        override val signOutConfirm = "确认退出登录？"
        override val cloudDataRetained = "云端记录仍会保留"
        override val restoreOnAnotherDevice = "换手机登录后即可恢复记录"
        override val signOutMessage = "退出登录不会删除云端记录；下次登录后仍可继续同步。"
        override val back = "返回"
        override val confirmSignOut = "确认退出"
        override val syncDescription = "记录会先保存在本机，联网后自动同步；换手机登录即可恢复。"
        override val syncing = "正在同步"
        override val syncNow = "立即同步"
        override val close = "关闭"
        override val signOut = "退出登录"
        override val deleteAccount = "删除账号与云端数据"
        override val notConfigured = "云同步未配置"
        override val offline = "当前离线，记录已保存在本机"
        override val synced = "云端已同步"
        override val shortNotConfigured = "未连接"
        override val shortOffline = "离线"
        override val shortSyncing = "同步中"
        override val shortSynced = "已同步"
        override val shortRetry = "同步失败"
        override val networkFailureTitle = "网络连接异常"
        override val networkFailureGuidance = "请检查网络或 VPN（梯子），然后重试。"
        override val authFailureTitle = "登录状态已失效"
        override val authFailureGuidance = "请重新登录账号，然后再次同步本机记录。"
        override val permissionFailureTitle = "账号无权访问云端数据"
        override val permissionFailureGuidance = "请重新登录；如果仍然失败，请稍后重试或联系开发者。"
        override val quotaFailureTitle = "云服务暂时无法处理请求"
        override val quotaFailureGuidance = "本机记录不会丢失，请稍后再试。"
        override val serviceFailureTitle = "云服务暂时不可用"
        override val serviceFailureGuidance = "云服务暂时异常；本机记录不会丢失，请稍后再试。"
        override val dataFailureTitle = "部分记录无法同步"
        override val dataFailureGuidance = "未同步的记录仍在本机，请稍后再试。"
        override val unknownFailureTitle = "暂时无法完成同步"
        override val unknownFailureGuidance = "记录仍在本机，请稍后再试。"
        override val syncDialogMessage = "请检查网络或 VPN（梯子），然后重试。"
        override val dataFormatFailure = "部分云端记录无法读取，其余记录已同步。"
        override val networkFailure = "网络连接异常，记录仍在本机。"
        override val authFailure = "登录状态已失效，记录仍在本机。"
        override val permissionFailure = "账号暂时无法访问云端，记录仍在本机。"
        override val quotaFailure = "云服务暂时无法处理同步，记录仍在本机。"
        override val serviceFailure = "云服务暂时异常，记录仍在本机。"
        override val dataFailure = "部分记录暂未同步，记录仍在本机。"
        override val unknownFailure = "同步未完成，记录仍在本机。"

        override fun timeoutFailure(timeoutMillis: Long): String =
            "等待云服务超过 ${(timeoutMillis / 1_000L).coerceAtLeast(1)} 秒，已停止同步；记录仍在本机。"

        override fun pending(count: Int): String = "有 $count 条记录待同步"

        override fun shortPending(count: Int): String = "待同步 $count 条"

        override fun syncChipDescription(status: String): String = "账号与云同步状态：$status"
    }

    private object RecordModule : AppStrings.RecordModuleStrings {
        override val handBrewLabel = "自慰"
        override val handBrewAccessibilityLabel = "自慰"
        override val handBrewQuestionToday = "今天自慰了几次？"
        override val handBrewQuestionPast = "当天自慰了几次？"
        override val handBrewZero = "当天没有自慰"
        override val sexLabel = "做爱"
        override val sexAccessibilityLabel = "做爱"
        override val sexQuestionToday = "今天做爱了几次？"
        override val sexQuestionPast = "当天做爱了几次？"
        override val sexZero = "当天没有做爱"

        override fun recordLabel(label: String): String = "${label}记录"
    }

    private object Deletion : AppStrings.DeletionStrings {
        override val warningTitle = "删除账号与云端数据？"
        override val confirmationTitle = "确认永久删除？"
        override val irreversible = "此操作无法撤销"
        override val verifyPassword = "输入当前密码验证身份"
        override val warningMessage = "继续后会先验证密码，再永久删除账号和全部云端记录。"
        override val localChoice = "选择是否保留本机记录"
        override val keepLocalTitle = "保留本机记录（推荐）"
        override val keepLocalDescription = "删除账号后继续离线使用这些记录"
        override val deleteLocalTitle = "同时删除本机记录"
        override val deleteLocalDescription = "云端和这台手机都不再保留这些记录"
        override val continueVerification = "继续验证身份"
        override val currentPassword = "当前密码"
        override val deleting = "正在删除…"
        override val deletePermanently = "永久删除账号"
        override val confirmationKeepLocal = "云端记录和账号会永久删除；本机记录将转为离线记录。"
        override val confirmationDeleteLocal = "云端记录、账号和本机记录都会永久删除。"
        override val networkError = "网络中断，删除未完成。本机记录仍保留，请打开 VPN（梯子）后重试。"
        override val networkAuthError = "网络不可用，请打开 VPN（梯子）后重试。"
        override val authError = "登录状态已失效，请重新登录后再删除。"
        override val permissionError = "账号暂时无权删除；本机记录仍保留，请重新登录后重试。"
        override val serviceError = "云服务暂时不可用；本机记录仍保留，请稍后重试。"
        override val unknownError = "删除未完成，本机记录仍保留。部分云端记录可能已删除，请重试。"
        override val localCleanupPending = "账号和云端数据已删除，但本机记录清理未完成，将在下次启动时自动完成。"
        override val authDeletionPending = "删除请求的最终结果暂时无法确认；本机记录已保留，云同步已暂停。请保持网络可用后重新打开应用。"
        override val retryRecovery = "重试恢复"
        override val recoveryConflict = "本机已有记录，恢复副本未覆盖；请先处理现有本机记录后再重试恢复。"
        override val recoveryRetryGuidance = "恢复操作暂时未完成；本机数据和恢复副本均已保留。请检查网络后重试。"
        override val replaceLocalAndRestore = "删除现有本机记录并恢复"
        override val replaceLocalAndRestoreTitle = "删除现有本机记录？"
        override val replaceLocalAndRestoreMessage = "这会删除当前本机空间中的记录，再恢复已删除账号的本机副本。此操作不可撤销。"
        override val cancelRecoveryReplacement = "取消"
        override val wrongPassword = "密码不正确，请重新输入"
        override val tooManyAttempts = "尝试次数过多，请稍后再试"
        override val localRecoveryPending = "本机恢复副本清理未完成，云服务同步已暂停。请保持网络可用后重新打开应用。"

        override fun selectionDescription(title: String, isSelected: Boolean): String =
            "$title，${if (isSelected) ZhStrings.selected else ZhStrings.unselected}"
    }

    private object Calendar : AppStrings.CalendarStrings {
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

    private object Navigation : AppStrings.NavigationStrings {
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

    private object Record : AppStrings.RecordStrings {
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

    private object Statistics : AppStrings.StatisticsStrings {
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
        override val weeklyLegendFuture = "未到"
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

    private object Settings : AppStrings.SettingsStrings {
        override val title = "设置"
        override val open = "打开设置"
        override val back = "返回主页"
        override val accountSection = "账号与同步"
        override val localAccountTitle = "本机记录"
        override val localAccountSummary = "记录只保存在这台设备；登录后可同步并在换机时恢复。"
        override val signedInAccountSummary = "查看同步状态、手动同步或管理账号"
        override val generalSection = "通用"
        override val languageTitle = "语言"
        override val languageZh = "中文"
        override val languageEn = "English"
        override val languageDialogTitle = "选择语言"
        override val dataSection = "数据与隐私"
        override val localFirstTitle = "本机优先"
        override val localFirstSummary = "所有记录先保存在本机；未登录时不会上传。"
        override val privacyTitle = "隐私保护"
        override val privacySummary = "不含广告、分析或崩溃上报 SDK"
        override val aboutSection = "关于"
        override val version = "版本"
        override val license = "开源许可"
        override val licenseValue = "Apache 2.0"

        override fun accountDescription(title: String, status: String): String = "$title，$status"
    }

    private object NavigationBar : AppStrings.NavigationBarStrings {
        override val calendar = "日历"
        override val statistics = "统计"
    }

    private object Components : AppStrings.ComponentStrings {
        override val decrease = "减少一次"
        override val increase = "增加一次"
    }
}
