package io.github.litaog.dailyrecord.core.common

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * English copy set.
 *
 * Length policy: fixed-width containers (module halves, period capsules,
 * calendar cells, sync chips) use short labels; TalkBack reads full terms.
 */
internal object EnStrings : AppStrings {
    override val privateRecordSubtitle = "Track daily counts"
    override val offlineSubtitle = "Offline, on this device"
    override val vpnSyncFailure = "Check your VPN and retry. Records stay on this device."
    override val readingLocalRecords = "Loading local records"
    override val selected = "selected"
    override val unselected = "not selected"
    override val today = "today"
    override val historyDate = "past date"
    override val futureDate = "future date"
    override val displayLocale: Locale = Locale.US
    override val semanticsSeparator = ", "

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
    override val appLock: AppStrings.AppLockStrings = AppLock
    override val disguise: AppStrings.DisguiseStrings = Disguise
    override val onboarding: AppStrings.OnboardingStrings = Onboarding

    fun withStyle(style: TerminologyStyle): AppStrings = this

    operator fun invoke(style: TerminologyStyle = TerminologyStyle.ELEGANT): AppStrings = this

    private val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun weekdayName(dayOfWeek: Int): String {
        require(dayOfWeek in 1..7) { "dayOfWeek must be in 1..7, was $dayOfWeek" }
        return weekdayNames[dayOfWeek - 1]
    }

    override fun selectedState(label: String, isSelected: Boolean): String =
        "$label, ${if (isSelected) selected else unselected}"

    override fun joinSemantics(vararg parts: String): String =
        parts.joinToString(semanticsSeparator)

    override fun joinSemantics(parts: Iterable<String>): String =
        parts.joinToString(semanticsSeparator)

    private object Auth : AppStrings.AuthStrings {
        override val signIn = "Sign in"
        override val register = "Sign up"
        override val email = "Email"
        override val password = "Password"
        override val confirmPassword = "Confirm password"
        override val show = "Show"
        override val hide = "Hide"
        override val showPassword = "Show password"
        override val hidePassword = "Hide password"
        override val forgotPassword = "Forgot password?"
        override val openPasswordReset = "Reset password"
        override val title = "Private Calendar"
        override val signInSubtitle = "Sign in to restore cloud records"
        override val registerSubtitle = "Sign up to back up records to the cloud"
        override val signInLocalSyncNotice = "Records saved in local mode sync to your account after sign-in"
        override val registerLocalSyncNotice = "Records saved in local mode sync to your account after sign-up"
        override val passwordPolicy = "At least 8 characters"
        override val signInVpnNotice = "A VPN is required to sign in.\nNo VPN? Use local mode to start recording."
        override val registerVpnNotice = "A VPN is required to sign up.\nNo VPN? Use local mode to start recording."
        override val emulatorNotice = "Connected to a local test environment. Cloud sync is unavailable."
        override val wait = "Please wait…"
        override val signInAndRestore = "Sign in & restore"
        override val createAccount = "Create account"
        override val continueOffline = "Skip sign-in, use local mode"
        override val continueOfflineRegister = "Skip sign-up, use local mode"
        override val emailRequired = "Enter your email"
        override val emailInvalid = "Enter a valid email"
        override val passwordTooShort = "Password must be at least 8 characters"
        override val passwordMismatch = "Passwords don't match"
        override val network = "Network or cloud timeout. Check your VPN and retry."
        override val emailAlreadyRegistered = "Email already registered. Sign in instead."
        override val weakPassword = "Password too weak. Use a longer one."
        override val tooManyRequests = "Too many attempts. Try again later."
        override val invalidCredentials = "Wrong email or password. Check and retry."
        override val signInUnavailable = "Sign-in unavailable. Try again later."
        override val registerUnavailable = "Sign-up unavailable. Try again later."
        override val resetSentTitle = "Check your inbox"
        override val resetTitle = "Reset password"
        override val resetSentSubtitle = "If the email is registered, a reset link has been sent. For privacy, we don't reveal whether the account exists."
        override val resetSubtitle = "Enter your registered email and we'll send a reset link."
        override val resetSuccess = "Check your inbox and spam folder, then follow the link."
        override val backToSignIn = "Back to sign-in"
        override val sendResetEmail = "Send reset email"
        override val sendingResetEmail = "Sending…"
        override val cancel = "Cancel"
        override val resetNetwork = "Network unavailable. Reset email not sent. Check your VPN and retry."
        override val resetTooManyRequests = "Too many requests. Try again later."
        override val resetQuotaExceeded = "Daily send limit reached. Try again later."
        override val resetUnavailable = "Reset email unavailable. Try again later."
    }

    private object Account : AppStrings.AccountStrings {
        override val signInSync = "Sign in & sync"
        override val reSignIn = "Sign in again"
        override val signInSyncAccessibility = "Sign in and sync records"
        override val accountAndSync = "Account & sync"
        override val signOutConfirm = "Sign out?"
        override val cloudDataRetained = "Cloud records will be kept"
        override val restoreOnAnotherDevice = "Sign in on another device to restore records"
        override val signOutMessage = "Signing out keeps your cloud records; sign in again to resume syncing."
        override val back = "Back"
        override val confirmSignOut = "Sign out"
        override val syncDescription = "Records save locally first and sync when online; sign in on another device to restore."
        override val syncing = "Syncing"
        override val syncNow = "Sync now"
        override val close = "Close"
        override val signOut = "Sign out"
        override val deleteAccount = "Delete account & cloud data"
        override val notConfigured = "Cloud sync not configured"
        override val offline = "Offline. Records saved on this device."
        override val synced = "Synced to cloud"
        override val shortNotConfigured = "Not linked"
        override val shortOffline = "Offline"
        override val shortSyncing = "Syncing"
        override val shortSynced = "Synced"
        override val shortRetry = "Sync failed"
        override val networkFailureTitle = "Network error"
        override val networkFailureGuidance = "Check your network or VPN, then retry."
        override val authFailureTitle = "Sign-in expired"
        override val authFailureGuidance = "Sign in again, then sync local records."
        override val permissionFailureTitle = "No cloud access"
        override val permissionFailureGuidance = "Sign in again; if it persists, retry later or contact the developer."
        override val quotaFailureTitle = "Cloud is busy"
        override val quotaFailureGuidance = "Local records are safe. Try again later."
        override val serviceFailureTitle = "Cloud unavailable"
        override val serviceFailureGuidance = "Cloud is temporarily down; local records are safe. Try again later."
        override val dataFailureTitle = "Some records can't sync"
        override val dataFailureGuidance = "Unsynced records stay on this device. Try again later."
        override val unknownFailureTitle = "Sync can't finish"
        override val unknownFailureGuidance = "Records stay on this device. Try again later."
        override val syncDialogMessage = "Check your network or VPN, then retry."
        override val dataFormatFailure = "Some cloud records couldn't be read. The rest synced."
        override val networkFailure = "Network error. Records stay on this device."
        override val authFailure = "Sign-in expired. Records stay on this device."
        override val permissionFailure = "No cloud access. Records stay on this device."
        override val quotaFailure = "Cloud is busy. Records stay on this device."
        override val serviceFailure = "Cloud unavailable. Records stay on this device."
        override val dataFailure = "Some records not synced. They stay on this device."
        override val unknownFailure = "Sync not finished. Records stay on this device."

        override fun timeoutFailure(timeoutMillis: Long): String =
            "Cloud timed out after ${(timeoutMillis / 1_000L).coerceAtLeast(1)}s. Records stay on this device."

        override fun pending(count: Int): String = "$count record(s) pending"

        override fun shortPending(count: Int): String = "$count pending"

        override fun syncChipDescription(status: String): String = "Account & sync status: $status"
    }

    private object RecordModule : AppStrings.RecordModuleStrings {
        // ADR-022: English uses neutral journal wording (Solo / Duo); the
        // Chinese labels stay direct until the Play listing decision (D1).
        override val handBrewLabel = "Solo"
        override val handBrewAccessibilityLabel = "Solo journal"
        override val handBrewQuestionToday = "How many solo moments today?"
        override val handBrewQuestionPast = "How many times that day?"
        override val handBrewZero = "No solo moments that day"
        override val sexLabel = "Duo"
        override val sexAccessibilityLabel = "Intimate journal"
        override val sexQuestionToday = "How many intimate moments today?"
        override val sexQuestionPast = "How many times that day?"
        override val sexZero = "No intimate moments that day"

        override fun recordLabel(label: String): String = "$label records"
    }

    private object Deletion : AppStrings.DeletionStrings {
        override val warningTitle = "Delete account & cloud data?"
        override val confirmationTitle = "Permanently delete?"
        override val irreversible = "This can't be undone"
        override val verifyPassword = "Enter your password to verify identity"
        override val warningMessage = "We'll verify your password, then permanently delete your account and cloud records."
        override val localChoice = "Choose what happens to local records"
        override val keepLocalTitle = "Keep local records (recommended)"
        override val keepLocalDescription = "Keep using them offline after deletion"
        override val deleteLocalTitle = "Also delete local records"
        override val deleteLocalDescription = "Remove records from cloud and this device"
        override val continueVerification = "Continue to verification"
        override val currentPassword = "Current password"
        override val deleting = "Deleting…"
        override val deletePermanently = "Delete account permanently"
        override val confirmationKeepLocal = "Cloud records and account will be permanently deleted; local records become offline-only."
        override val confirmationDeleteLocal = "Cloud records, account, and local records will all be permanently deleted."
        override val networkError = "Network interrupted. Deletion not completed. Local records are kept. Check your VPN and retry."
        override val networkAuthError = "Network unavailable. Check your VPN and retry."
        override val authError = "Sign-in expired. Sign in again to delete."
        override val permissionError = "Deletion not authorized. Local records are kept. Sign in again and retry."
        override val serviceError = "Cloud unavailable. Local records are kept. Try again later."
        override val unknownError = "Deletion not completed. Local records are kept. Some cloud records may already be deleted. Retry."
        override val localCleanupPending = "Account and cloud data deleted, but local cleanup is pending. It will finish on next launch."
        override val authDeletionPending = "Deletion result unconfirmed. Local records are kept and sync is paused. Reopen the app with network available."
        override val retryRecovery = "Retry recovery"
        override val recoveryConflict = "Local records exist. The recovery copy was not applied. Handle existing local records first, then retry."
        override val recoveryRetryGuidance = "Recovery not finished. Local data and recovery copy are both kept. Check your network and retry."
        override val replaceLocalAndRestore = "Delete existing local records & restore"
        override val replaceLocalAndRestoreTitle = "Delete existing local records?"
        override val replaceLocalAndRestoreMessage = "This deletes records in the current local space, then restores the deleted account's local copy. This can't be undone."
        override val cancelRecoveryReplacement = "Cancel"
        override val wrongPassword = "Incorrect password. Try again."
        override val tooManyAttempts = "Too many attempts. Try again later."
        override val localRecoveryPending = "Local recovery cleanup pending. Cloud sync paused. Reopen the app with network available."

        override fun selectionDescription(title: String, isSelected: Boolean): String =
            "$title, ${if (isSelected) EnStrings.selected else EnStrings.unselected}"
    }

    private object Calendar : AppStrings.CalendarStrings {
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

    private object Navigation : AppStrings.NavigationStrings {
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

    private object Record : AppStrings.RecordStrings {
        override val loading = "Loading…"
        override val saving = "Saving…"
        override val saved = "Saved"
        override val save = "Save"
        override val saveFailure = "Save failed. Retry."
        override val clear = "Clear"
        override val clearFailure = "Clear failed. Retry."
        override val countOnly = "Count only"
        override val countFirst = "Count first"
        override val countAndDetails = "Count & details"
        override val detailEntry = "Add time & notes"
        override val detailEntryHintFormat = "Add details for %d times"
        override val detailSectionTitle = "Details"
        override val detailSectionHint = "Each +1 adds one entry"
        override val detailCollapse = "Hide details"
        override val detailExpand = "Show details"
        override val detailOccurrenceFormat = "#%d"
        override val detailStartTime = "Start"
        override val detailEndTime = "End"
        override val detailStartTimeUnset = "Start time"
        override val detailEndTimeUnset = "End time"
        override val detailTimeUnset = "Pick time"
        override val detailTimePickerTitle = "Pick time"
        override val detailTimePickerSubtitle = "Swipe to set hour and minute"
        override val detailTimePickerHour = "Hour"
        override val detailTimePickerMinute = "Minute"
        override val detailTimePickerHint = "Auto-aligns when scrolling stops"
        override val detailTimePickerConfirm = "OK"
        override val detailWriteFeeling = "Add note"
        override val detailCollapseFeeling = "Hide"
        override val detailFeelingLabel = "Note (optional)"
        override val detailFeelingHint = "Write how this moment felt"
        override val detailFeelingCounter = "%d / ${MAX_RECORD_DETAIL_FEELING_CHARACTERS}"
        override val detailEndBeforeStart = "End time can't be before start time"
        override val detailDiscardTitle = "Remove this entry?"
        override val detailDiscardMessage = "Its time and note will be removed too."
        override val detailConfirmRemove = "Remove entry"
        override val detailEntryUnavailable = "Too many entries: edit total only. Existing details within range are kept."
        override val loadingRecords = "Loading records…"
        override val futureUnavailable = "Future date, can't record"
        override val notSaved = "Unset"
        override val zeroRecorded = "Recorded · 0"
        override val clearTitle = "Clear this day?"
        override val clearSubtitle = "This module only"
        override val clearMessage = "Clears to unset; not counted in stats."
        override val confirmClear = "Clear"
        override val clearDetailsFailure = "Failed to clear details. Retry."
        override val clearDetailsTitle = "Clear these details?"
        override val clearDetailsSubtitle = "Times and notes only"
        override val clearDetailsMessage = "Keeps today's count; removes times and notes."
        override val confirmClearDetails = "Clear details"
        override val discardTitle = "Discard unsaved changes?"
        override val unsavedSubtitle = "Count or details not saved"
        override val discardMessage = "Going back loses these changes."
        override val continueEditing = "Keep editing"
        override val discard = "Discard"
        override val backToCalendar = "Back to calendar"

        override fun savedStatus(count: Int): String = "Unsaved · $count"

        override fun recordedStatus(count: Int): String = "Recorded · $count"

        override fun explicitZeroHint(text: String): String = "0 means no $text that day; the record is kept."

        override fun monthSaved(month: Int): String = "${monthLabel(month)} records"

        override fun monthSummary(count: Long, days: Int): String = "$count times · $days days"

        override fun moduleRecordLabel(moduleLabel: String): String = "$moduleLabel records"

        override fun dateLabel(date: LocalDate, weekday: String): String =
            "${monthLabel(date.monthValue)} ${date.dayOfMonth} · $weekday"

        override fun detailEntryHint(count: Int): String = detailEntryHintFormat.format(count)

        override fun detailOccurrence(index: Int): String = detailOccurrenceFormat.format(index)

        override fun detailFeelingCounter(count: Int): String = detailFeelingCounter.format(count)

        override fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            "${detailOccurrence(occurrence)}, $label, $value"

        override fun detailTimeWheelCurrent(unit: String, value: String): String =
            "$unit, current $value"

        override fun detailTimeWheelOption(unit: String, value: String): String =
            "Choose $unit $value"

        override fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            "${detailOccurrence(occurrence)}, $action"

        override fun detailFeelingEditorDescription(occurrence: Int): String =
            "${detailOccurrence(occurrence)}, $detailFeelingLabel"

        private val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        private fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "$month" }
    }

    private object Statistics : AppStrings.StatisticsStrings {
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

    private object Settings : AppStrings.SettingsStrings {
        override val title = "Settings"
        override val open = "Open settings"
        override val back = "Back to home"
        override val accountSection = "Account & sync"
        override val localAccountTitle = "Local records"
        override val localAccountSummary = "Stored on this device only; sign in to sync and restore."
        override val signedInAccountSummary = "Check sync status, sync manually, or manage your account"
        override val generalSection = "General"
        override val terminologyTitle = "Terminology Style"
        override val terminologyDialogTitle = "Choose terminology style"
        override val terminologyElegant = "Elegant"
        override val terminologyDirect = "Direct"
        override val terminologyElegantOption = "Elegant (Solo / Partner Intimacy)"
        override val terminologyDirectOption = "Direct (Direct Terms)"
        override val languageTitle = "Language"
        override val languageZh = "中文"
        override val languageEn = "English"
        override val languageDialogTitle = "Choose language"
        override val themeTitle = "Color theme"
        override val themeDialogTitle = "Choose color theme"
        override val themeAurora = "Aurora"
        override val themeGlacialCoral = "Glacier Coral"
        override val themeTwilightLilac = "Twilight Lilac"
        override val themeEmeraldBerry = "Emerald Berry"
        override val themeTitaniumChampagne = "Titanium Gold"
        override val themeCreamLavender = "Cream Lavender"
        override val themeSpringSky = "Spring Sky"
        override val themeBuffGlacier = "Buff Glacier"
        override val themeDreamBlush = "Blush Dream"
        override val dataSection = "Data & privacy"
        override val localFirstTitle = "Local first"
        override val localFirstSummary = "All records save locally first; nothing uploads while signed out."
        override val privacyTitle = "Privacy"
        override val privacySummary = "No ads, analytics, or crash-reporting SDKs"
        override val aboutSection = "About"
        override val version = "Version"
        override val license = "License"
        override val licenseValue = "Apache 2.0"
        override val privacyPolicy = "Privacy policy"
        override val privacyPolicyAction = "Opens in browser"

        override fun accountDescription(title: String, status: String): String = "$title, $status"
    }

    private object NavigationBar : AppStrings.NavigationBarStrings {
        override val calendar = "Calendar"
        override val statistics = "Stats"
    }

    private object AppLock : AppStrings.AppLockStrings {
        override val lockedTitle = "Locked"
        override val lockedBody = "Verify it's you to continue"
        override val unlock = "Unlock"
        override val notCompleted = "Verification wasn't completed. Try again."
        override val promptTitle = "Verify it's you"
        override val promptSubtitle = "Use your fingerprint, face or screen lock"
        override val settingTitle = "App lock"
        override val settingSummary = "Ask for verification when the app opens"
        override val settingOn = "On"
        override val settingOff = "Off"
        override val recentsNote = "When on, recent apps don't show the content"
        override val recentsAndScreenshotNote = "When on, recent apps don't show the content and screenshots are blocked"
        override val delayTitle = "Lock again"
        override val delayDialogTitle = "When to lock again"
        override val delayImmediately = "Immediately"
        override val delayOneMinute = "After 1 minute away"
        override val delayFiveMinutes = "After 5 minutes away"
        override val needsScreenLock = "Set a screen lock in system settings first"
        override val turnedOffNoScreenLock = "This device has no screen lock, so app lock was turned off"
    }

    private object Disguise : AppStrings.DisguiseStrings {
        override val settingTitle = "Home screen disguise"
        override val settingSummary = "When on, the home screen shows the app as \"Journal\""
        override val settingNote = "The home screen takes a few seconds to update. Add the icon to your home screen again."
    }

    private object Components : AppStrings.ComponentStrings {
        override val decrease = "Decrease by 1"
        override val increase = "Increase by 1"
    }

    private object Onboarding : AppStrings.OnboardingStrings {
        override val title = "Terminology Guide"
        override val soloLabel = "Solo Intimacy"
        override val soloDescription = "Track personal rhythm and solo moments"
        override val duoLabel = "Partner Intimacy"
        override val duoDescription = "Track couple intimacy and shared moments"
        override val settingsHint = "You can switch between Elegant and Direct styles anytime in Settings."
        override val dismissAction = "Got it"
    }
}
