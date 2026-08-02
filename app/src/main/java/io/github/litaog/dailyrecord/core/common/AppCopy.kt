package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth

/**
 * Single source for user-facing copy used by both the UI and sync layer.
 *
 * Keeping copy out of individual Composables makes wording changes auditable and
 * prevents the two record modules from drifting. Dynamic copy stays here as
 * small functions so callers only provide data, never assemble user-facing
 * fragments themselves.
 */
internal object AppCopy {
    const val appName = "私密日历"
    const val privateRecordSubtitle = "记录每天的私密次数"
    const val offlineSubtitle = "本机记录无需 VPN（梯子），可离线使用"
    const val vpnSyncFailure = "云同步需要打开 VPN（梯子）。记录已保存在本机，请开启后重试。"
    const val diagnosticUnavailable = "诊断信息暂不可用"
    const val readingLocalRecords = "正在读取本机记录"

    const val selected = "已选择"
    const val unselected = "未选择"
    const val today = "今天"
    const val historyDate = "历史日期"
    const val futureDate = "未来日期"
    private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun weekdayName(dayOfWeek: Int): String = weekdayNames[(dayOfWeek - 1).coerceIn(0, 6)]

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
        const val subtitle = "登录后，本机记录会合并到你的账号，换手机可自动恢复。"
        const val passwordPolicy = "密码至少 8 位；不使用短信或验证码。请妥善保存密码。"
        const val vpnNotice = "登录和注册需要打开 VPN（梯子）。选择本机使用则无需开启，但不会同步到云端。"
        const val emulatorNotice = "云端开发项目尚未完成配置，当前构建只能连接本地测试环境。"
        const val wait = "请稍候…"
        const val signInAndRestore = "登录并恢复记录"
        const val createAccount = "创建账号"
        const val continueOffline = "先在本机使用"
        const val offlineMergeNotice = "以后登录时，本机记录会合并到你下一次登录的账号。"
        const val cloudDataNotice = "云端只保存两个模块中已记录的日期、次数和同步所需数据。"
        const val emailRequired = "请输入邮箱"
        const val emailInvalid = "请输入有效邮箱"
        const val passwordTooShort = "密码至少需要 8 位"
        const val passwordMismatch = "两次输入的密码不一致"
        const val network = "连接云服务超时或网络不可用，请打开 VPN（梯子）后重试"
        const val emailAlreadyRegistered = "此邮箱已注册，请直接登录"
        const val weakPassword = "密码强度不足，请使用更长的密码"
        const val tooManyRequests = "尝试次数过多，请稍后再试"
        const val invalidCredentials = "邮箱或密码不正确，请检查后重试"
        const val signInUnavailable = "暂时无法登录，请稍后重试"
        const val registerUnavailable = "暂时无法创建账号，请稍后重试"
        const val resetSentTitle = "请查收邮件"
        const val resetTitle = "重置密码"
        const val resetSentSubtitle = "为了保护账号隐私，我们不会显示该邮箱是否已注册。"
        const val resetSubtitle = "输入注册邮箱，我们会发送安全的重置链接。"
        const val resetSuccess = "如果该邮箱已注册，重置邮件将在几分钟内送达。请检查收件箱和垃圾邮件。"
        const val backToSignIn = "返回登录"
        const val sendResetEmail = "发送重置邮件"
        const val sendingResetEmail = "正在发送…"
        const val cancel = "取消"
        const val resetNetwork = "网络不可用，邮件尚未发送。请打开 VPN（梯子）后重试。"
        const val resetTooManyRequests = "请求过于频繁，请稍后再试。"
        const val resetQuotaExceeded = "今日发送额度已用完，请稍后再试。"
        const val resetUnavailable = "暂时无法发送重置邮件，请稍后重试。"
    }

    object Account {
        const val diagnostics = "诊断"
        const val diagnosticsAccessibility = "查看本机诊断信息"
        const val signInSync = "登录同步"
        const val reSignIn = "重新登录"
        const val signInSyncAccessibility = "登录账号并开启云同步"
        const val accountAndSync = "账号与云同步"
        const val signOutConfirm = "确认退出登录？"
        const val cloudDataRetained = "云端数据会保留"
        const val restoreOnAnotherDevice = "换手机后仍可恢复全部记录"
        const val signOutMessage = "退出后不会删除云端记录；本机缓存仍按账号隔离，下次登录会继续同步。"
        const val back = "返回"
        const val confirmSignOut = "确认退出"
        const val syncDescription = "记录会先保存在本机，断网时照常使用；联网后自动上传，并可在其他手机登录恢复。"
        const val syncing = "正在同步"
        const val syncNow = "立即同步"
        const val close = "关闭"
        const val viewDiagnostics = "查看诊断信息"
        const val signOut = "退出登录"
        const val deleteAccount = "删除账号与云端数据"
        const val notConfigured = "云端尚未配置"
        const val offline = "当前离线，记录已保存在本机"
        const val synced = "云端已同步"
        const val shortNotConfigured = "未配置"
        const val shortOffline = "离线"
        const val shortSyncing = "同步中"
        const val shortSynced = "已同步"
        const val shortRetry = "需重试"
        const val networkFailureTitle = "网络连接异常"
        const val networkFailureGuidance = "请检查网络或 VPN（梯子），然后点击“立即同步”。"
        const val authFailureTitle = "登录状态已失效"
        const val authFailureGuidance = "请重新登录账号，然后再次同步本机记录。"
        const val permissionFailureTitle = "账号没有云端访问权限"
        const val permissionFailureGuidance = "请重新登录；如果仍然失败，请稍后重试或联系开发者。"
        const val quotaFailureTitle = "云服务额度暂时受限"
        const val quotaFailureGuidance = "本机记录不会丢失，请稍后再点击“立即同步”。"
        const val serviceFailureTitle = "云服务暂时不可用"
        const val serviceFailureGuidance = "可能是 Firebase 临时故障，本机记录不会丢失，请稍后重试。"
        const val dataFailureTitle = "部分记录无法同步"
        const val dataFailureGuidance = "原始记录已保存在本机，请不要清除应用数据，并在稍后重试。"
        const val unknownFailureTitle = "暂时无法完成同步"
        const val unknownFailureGuidance = "未能确定失败原因，本机记录不会丢失，请稍后重试。"
        const val syncDialogMessage = "请检查网络或 VPN（梯子），然后点击“立即同步”。"
        const val dataFormatFailure = "部分云端记录格式异常，其余记录已继续同步"
        const val timeoutFailure = "连接云服务超过 5 秒，记录已保存在本机"
        const val networkFailure = "网络连接异常，记录已保存在本机"
        const val authFailure = "登录状态已失效，记录已保存在本机"
        const val permissionFailure = "账号暂无云端访问权限，记录已保存在本机"
        const val quotaFailure = "云服务额度暂时受限，记录已保存在本机"
        const val serviceFailure = "云服务暂时不可用，记录已保存在本机"
        const val dataFailure = "部分记录暂时无法同步，原始记录已保存在本机"
        const val unknownFailure = "暂时无法同步，记录已保存在本机"

        fun pending(count: Int): String = "有 $count 条记录等待同步"
        fun shortPending(count: Int): String = "待同步 $count"
        fun syncChipDescription(status: String): String = "账号与云同步，$status"
    }

    object RecordModule {
        const val handBrewLabel = "手冲"
        const val handBrewQuestionToday = "今天手冲了几次？"
        const val handBrewQuestionPast = "这天手冲了几次？"
        const val handBrewZero = "明确没冲"
        const val sexLabel = "做爱"
        const val sexQuestionToday = "今天做爱了几次？"
        const val sexQuestionPast = "这天做爱了几次？"
        const val sexZero = "明确没有"
        fun recordLabel(label: String): String = "${label}记录"
    }

    object Deletion {
        const val warningTitle = "删除账号与云端数据？"
        const val confirmationTitle = "再次确认永久删除"
        const val irreversible = "此操作无法撤销"
        const val verifyPassword = "输入当前密码验证身份"
        const val warningMessage = "继续后会先验证密码，再删除该账号的全部云端手冲、做爱记录和登录账号。"
        const val localChoice = "选择本机记录的处理方式"
        const val keepLocalTitle = "保留在本机（推荐）"
        const val keepLocalDescription = "删除账号后继续离线使用这些记录"
        const val deleteLocalTitle = "同时删除本机记录"
        const val deleteLocalDescription = "云端和这台手机都不再保留"
        const val continueVerification = "继续验证身份"
        const val currentPassword = "当前密码"
        const val deleting = "正在永久删除…"
        const val deletePermanently = "永久删除账号"
        const val confirmationKeepLocal = "云端记录和账号会永久删除；本机记录将转为离线记录。"
        const val confirmationDeleteLocal = "云端记录、账号和这台手机里的账号记录都会永久删除。"
        const val networkError = "网络中断，删除未完成；本机记录仍保留，请开启 VPN（梯子）后重试"
        const val networkAuthError = "网络不可用，请确认 VPN（梯子）已开启后重试"
        const val authError = "登录状态已变化，请退出后重新登录再删除"
        const val permissionError = "账号暂无删除权限；本机记录仍保留，请重新登录后重试"
        const val serviceError = "云服务暂时不可用；本机记录仍保留，请稍后重试"
        const val unknownError = "删除未完成，本机记录仍保留；部分云端记录可能已先删除，请直接重试"
        const val wrongPassword = "密码不正确，请重新输入"
        const val tooManyAttempts = "尝试次数过多，请稍后再试"

        fun selectionDescription(title: String, isSelected: Boolean): String =
            "$title，${if (isSelected) AppCopy.selected else AppCopy.unselected}"
    }

    object Calendar {
        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
        const val recordHint = "点击日期记录"
        const val unset = "未填"
        const val future = "未来"
        const val zero = "0 次"
        const val recorded = "1+ 次"
        const val previousMonth = "上个月"
        const val nextMonth = "下个月"
        const val selectDate = "选择年份和日期"
        const val backToToday = "回到今天"
        const val legendDescription = "点击日期记录%s次数。图例：未填写、未来不可记录、明确记录零次、已记录"
        const val unavailable = "不可用"
        const val oneTime = "1次"
        const val twoTimes = "2次"
        const val ninePlusTimes = "9+次"
        const val todayShort = "今"
        const val futureDescription = "未来日期，不可记录"
        const val unsetDescription = "未填写"
        const val zeroDescription = "明确记录 0 次"
        const val recordedDescription = "%s %d 次"
        const val selectedSuffix = "，已选择"
        const val todaySuffix = "，今天"

        fun monthSummary(count: Long, days: Int): String = "本月 $count 次 · $days 天"
        fun monthTitle(month: YearMonth): String = "${month.year}年 ${month.monthValue}月"
        fun monthTitleMultiline(month: YearMonth): String = "${month.year}年\n${month.monthValue}月"
        fun monthSelectionDescription(month: YearMonth): String =
            "$selectDate，当前${month.year}年${month.monthValue}月"
        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$state${if (focused) selectedSuffix else ""}"
        fun legendDescription(moduleLabel: String): String = legendDescription.format(moduleLabel)
        fun countDescription(count: Int): String = when (count) {
            1 -> oneTime
            2 -> twoTimes
            in 3..8 -> "${count}次"
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
                count == 0 -> "$zeroDescription$moduleLabel"
                else -> recordedDescription.format(moduleLabel, count)
            }
            return if (date == today) "$status$todaySuffix" else status
        }
    }

    object Navigation {
        val weekdays = Calendar.weekdays
        const val title = "快速跳转"
        const val subtitle = "直接选择年份和日期，不必逐月翻找"
        const val jump = "跳转到此日"
        const val selected = "已选择"
        const val switchYear = "切换年份"
        const val returnToDatePicker = "返回日期选择"
        const val selectYear = "选择年份"
        fun switchYearDescription(year: Int): String = "$switchYear，当前${year}年"
        fun nextMonthDescription(forward: Boolean): String = if (forward) "快速跳转下个月" else "快速跳转上个月"
        fun dateDescription(date: LocalDate, weekday: String): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$weekday"
        fun dateText(date: LocalDate): String =
            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))
        fun dateLabel(date: LocalDate, weekday: String): String =
            "${dateText(date)} · $weekday"
        fun selectYearDescription(year: Int): String = "选择${year}年"
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
        const val loadingRecords = "正在读取记录…"
        const val futureUnavailable = "未来日期 · 不可记录"
        const val notSaved = "尚未填写"
        const val zeroRecorded = "已记录 · 0 次"
        const val saveHint = "保存后更新日历与统计"
        const val clearTitle = "清除这天的记录？"
        const val clearSubtitle = "记录会恢复为“未填写”"
        const val clearMessage = "这次操作不能在应用内撤销，也不会计入统计。"
        const val confirmClear = "确认清除"
        const val discardTitle = "放弃未保存的修改？"
        const val unsavedSubtitle = "当前次数还没有保存"
        const val discardMessage = "返回日历后，本次调整会丢失。"
        const val continueEditing = "继续编辑"
        const val discard = "放弃修改"
        const val backToCalendar = "返回日历"
        fun savedStatus(count: Int): String = "待保存 · $count 次"
        fun recordedStatus(count: Int): String = "已记录 · $count 次"
        fun explicitZeroHint(text: String): String = "0 次＝$text，会保留记录。"
        fun monthSaved(month: Int): String = "${month}月已保存"
        fun monthSummary(count: Long, days: Int): String = "$count 次 · $days 天"
        fun moduleRecordLabel(moduleLabel: String): String = "${moduleLabel}记录"
        fun dateLabel(date: LocalDate, weekday: String): String =
            "${date.monthValue}月${date.dayOfMonth}日 · $weekday"
    }

    object Statistics {
        val weekdays = Calendar.weekdays
        const val title = "统计"
        const val countAndDays = "次数 · 天数"
        const val countUnit = "次"
        const val recordedDaysLabel = "发生天数"
        const val averageLabel = "记录日均"
        const val historyFacts = "历史事实"
        const val noFutureTrend = "只展示已发生数据，不预测未来趋势"
        const val dailyDetails = "每日明细"
        const val weeklyDetails = "周明细"
        const val monthlyDetails = "月份明细"
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
        const val currentYear = "本年"
        const val historyPeriod = "历史"
        const val previousPeriod = "上一个%s"
        const val nextPeriod = "下一个%s"
        const val selectDate = "选择统计日期"
        const val emptyTitle = "还没有可统计的%s记录"
        const val emptyMessage = "回到日历选择日期，保存第一条记录。"
        const val calendarAction = "去日历记录"
        const val dailyDistribution = "每日分布"
        const val times = "次数"
        const val dailyRecords = "每日记录"
        const val realDates = "真实日期"
        const val futureDay = "未来日期"
        const val future = "未来"
        const val unset = "未填写"
        const val unsetShort = "未填"
        const val dash = "—"
        const val explicitZero = "明确记录 0 次"
        const val zeroAccessibility = "明确 0 次"
        const val annualCount = "年度次数"
        const val blankBarHint = "空白表示未填写或未来；0 次不绘制柱高"
        const val quarterShare = "季度占比"
        const val noPositiveCount = "暂无正次数"
        const val byCount = "按次数"
        const val quarterShareHint = "至少有一次正次数记录后显示季度占比。"
        const val monthSummary = "月份摘要"
        const val fullMonths = "完整月份"
        const val monthExtremesHint = "完成至少一个有记录的月份后显示最高和最低月份。"
        const val maximumMonth = "最高月份"
        const val minimumMonth = "最低月份"
        const val monthAverageFormat = "12 个月 · 月均 %.1f 次"
        const val firstRecordPrefix = "首次记录："

        fun detailCount(count: Long?): String = if (count == null) "未填写" else "$count 次"
        fun detailDays(days: Int?): String = if (days == null) "未填写" else "$days 天"
        fun firstRecord(date: LocalDate?): String = firstRecordPrefix + (date?.toString() ?: noRecords)
        fun historyStatus(first: LocalDate?, today: LocalDate): String = first?.let {
            "${it.year}.${it.monthValue.toString().padStart(2, '0')}–" +
                "${today.year}.${today.monthValue.toString().padStart(2, '0')}"
        } ?: noRecords
        fun yearTitle(year: Int): String = "${year}年"
        fun monthTitle(year: Int, month: Int): String = "${year}年 ${month}月"
        fun monthLabel(month: Int): String = "${month}月"
        fun dateDescription(date: LocalDate, status: String): String =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$status"
        fun weekdayDateLabel(weekday: String, date: LocalDate): String = "$weekday ${date.dayOfMonth}日"
        fun monthWeekLabel(index: Int, start: LocalDate, end: LocalDate): String =
            "第${index}周 ${start.dayOfMonth}–${end.dayOfMonth}日"
        fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            if (start.year == end.year) {
                "${start.year}年 ${start.monthValue}月${start.dayOfMonth}日–" +
                    "${end.monthValue}月${end.dayOfMonth}日"
            } else {
                "${start.year}年${start.monthValue}月${start.dayOfMonth}日–${end.year}年${end.monthValue}月${end.dayOfMonth}日"
            }
        fun yearStatus(end: LocalDate, today: LocalDate): String =
            if (end < today) ended else "截至 ${today.monthValue}月${today.dayOfMonth}日"
        fun periodStatus(end: LocalDate, today: LocalDate): String = if (end < today) ended else inProgress
        fun periodAction(period: String, previous: Boolean): String =
            (if (previous) previousPeriod else nextPeriod).format(period)
        fun datePickerDescription(title: String): String = "$selectDate，当前$title"
        fun emptyTitle(moduleLabel: String): String = Statistics.emptyTitle.format(moduleLabel)
        fun periodCountLabel(period: String, moduleLabel: String): String = "$period · ${moduleLabel}次数"
        fun statisticsLabel(period: String): String = "${period}统计"
        fun average(value: Double): String = String.format(java.util.Locale.US, "%.1f 次/天", value)
        fun annualAverage(value: Double): String = String.format(java.util.Locale.US, monthAverageFormat, value)
        fun countText(count: Long): String = "$count 次"
        fun daysText(days: Int): String = "$days 天"
        fun percentage(value: Double): String = String.format(java.util.Locale.US, "%.0f%%", value)
        fun quarterLabel(quarter: Int): String = "Q$quarter"
        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            "${month}月 ${chartMonthValue(isFuture, recorded, count)}"
        fun totalCountAccessibility(total: Long, quarters: String): String = "季度占比，总次数 $total 次；$quarters"
        fun annualChartAccessibility(months: String): String = "年度次数柱状图；$months"
        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String = when {
            isFuture -> future
            !recorded -> unset
            else -> countText(count ?: 0L)
        }.toString()
    }

    object Diagnostics {
        const val title = "本机诊断信息"
        const val subtitle = "不包含邮箱、私密记录日期、次数或密码"
        const val copy = "复制诊断信息"
        const val copied = "诊断信息已复制"
        const val copyFailed = "复制失败，请手动选择文字"
        const val share = "分享诊断信息"
        const val noShareTarget = "没有找到可用的分享应用"
        const val back = "返回"
        const val shareHint = "发送前仍可长按检查或选择其中的文字。"
        const val clipboardLabel = "私密日历诊断信息"
    }

    object NavigationBar {
        const val calendar = "日历"
        const val statistics = "统计"
    }

    object Components {
        const val decrease = "减少一次"
        const val increase = "增加一次"
    }
}
