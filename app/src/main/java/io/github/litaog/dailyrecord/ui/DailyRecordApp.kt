package io.github.litaog.dailyrecord.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.litaog.dailyrecord.BuildConfig
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationSelection
import io.github.litaog.dailyrecord.ui.calendar.DailyCountCalendarScreen
import io.github.litaog.dailyrecord.ui.record.DailyCountRecordScreen
import io.github.litaog.dailyrecord.ui.statistics.DailyCountStatisticsScreen
import io.github.litaog.dailyrecord.ui.settings.SettingsScreen
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.sync.Mutex
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.settings.LauncherDisguiseSettings

@Composable
fun DailyRecordApp(
    repository: HandBrewRecordRepository,
    sexRepository: SexRecordRepository? = null,
    today: LocalDate? = null,
    accountEmail: String? = null,
    syncStatus: SyncStatus = SyncStatus.NotConfigured,
    onSyncNow: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSignIn: (() -> Unit)? = null,
    onLanguageChanged: (AppLanguage) -> Unit = {},
    onColorThemeChanged: (DailyRecordColorTheme) -> Unit = {},
    appLockSettings: AppLockSettings? = null,
    launcherDisguiseSettings: LauncherDisguiseSettings? = null,
    onDeleteAccount: suspend (String, LocalDataAfterAccountDeletion) -> Result<Unit> = { _, _ ->
        Result.failure(IllegalStateException("Account deletion is unavailable"))
    },
) {
    val context = LocalContext.current
    val effectiveToday = today ?: rememberCurrentDate()
    val modulePreference = remember(context) { SelectedRecordModulePreference(context) }
    val languagePreference = remember(context) { LanguagePreference(context) }
    val language by languagePreference.language.collectAsStateWithLifecycle(
        initialValue = languagePreference.current,
    )
    val terminologyStylePreference = remember(context) { TerminologyStylePreference(context) }
    val terminologyStyle by terminologyStylePreference.style.collectAsStateWithLifecycle(
        initialValue = terminologyStylePreference.current,
    )
    val onTerminologyStyleSelected: (TerminologyStyle) -> Unit = { selected ->
        if (selected != terminologyStyle) {
            terminologyStylePreference.setStyle(selected)
            AppLanguageState.current = language.strings(selected)
        }
    }
    val intimacyOnboardingPreference = remember(context) { IntimacyOnboardingPreference(context) }
    val intimacyOnboardingDismissed by intimacyOnboardingPreference.dismissed.collectAsStateWithLifecycle(
        initialValue = intimacyOnboardingPreference.isDismissed,
    )
    val onLanguageSelected: (AppLanguage) -> Unit = { selected ->
        // Tapping the active language must not trigger a full recreation.
        if (selected != language) {
            languagePreference.setLanguage(selected)
            AppLanguageState.current = selected.strings(terminologyStyle)
            onLanguageChanged(selected)
        }
    }
    val colorThemePreference = remember(context) { ColorThemePreference(context) }
    val colorTheme by colorThemePreference.theme.collectAsStateWithLifecycle(
        initialValue = colorThemePreference.current,
    )
    val onColorThemeSelected: (DailyRecordColorTheme) -> Unit = { selected ->
        // Re-selecting the active theme must not trigger a full recreation.
        if (selected != colorTheme) {
            colorThemePreference.setTheme(selected)
            onColorThemeChanged(selected)
        }
    }
    val handBrewController = remember(repository) { HandBrewModuleController(repository) }
    val sexController = remember(sexRepository) {
        sexRepository?.let(::SexModuleController)
    }
    val availableControllers = remember(handBrewController, sexController) {
        listOfNotNull(handBrewController, sexController)
    }
    val availableModuleSpecs = remember(availableControllers, language, terminologyStyle) {
        availableControllers.map { it.module.uiSpec() }
    }
    var selectedModuleName by rememberSaveable {
        mutableStateOf(modulePreference.selectedModule.name)
    }
    var destinationName by rememberSaveable { mutableStateOf(TopDestination.Calendar.name) }
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    var browseDateText by rememberSaveable { mutableStateOf(effectiveToday.toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var datePickerSelectionName by rememberSaveable {
        mutableStateOf(DateNavigationSelection.Date.name)
    }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountDeletion by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    AppRootFeedbackEffects(
        syncStatus = syncStatus,
        showAccountDialog = showAccountDialog,
        showSettings = showSettings,
        snackbarHostState = snackbarHostState,
    )

    val currentMonth = YearMonth.from(effectiveToday)
    val destination = TopDestination.entries.firstOrNull { it.name == destinationName }
        ?: TopDestination.Calendar
    val selectedModule = RecordModule.entries
        .firstOrNull { it.name == selectedModuleName }
        ?.takeIf { selected -> availableControllers.any { it.module == selected } }
        ?: RecordModule.HandBrew
    val selectedController = availableControllers.first { it.module == selectedModule }
    val moduleSpec = remember(selectedModule, language) { selectedModule.uiSpec() }
    val selectModule: (RecordModule) -> Unit = { module ->
        if (availableControllers.any { it.module == module }) {
            selectedModuleName = module.name
            modulePreference.setSelectedModule(module)
        }
    }
    val selectedDate = remember(selectedDateText, effectiveToday) {
        selectedDateText
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.takeIf { it in EARLIEST_SUPPORTED_DATE..effectiveToday }
    }
    val browseDate = remember(browseDateText, effectiveToday) {
        runCatching { LocalDate.parse(browseDateText) }
            .getOrDefault(effectiveToday)
            .takeIf { it in EARLIEST_SUPPORTED_DATE..effectiveToday }
            ?: effectiveToday
    }
    val displayedMonth = YearMonth.from(browseDate)
    // The full-history flow drives the statistics screen and the initial
    // loading gate; the calendar and record screen consume a month-scoped
    // flow so saves outside the consumed month do not rebuild their models.
    // The scope follows the record screen's selected date when open, so the
    // month summary can never desync from the edited date.
    val recordsMonth = selectedDate?.let(YearMonth::from) ?: displayedMonth
    val recordsFlow = remember(selectedController, effectiveToday) {
        selectedController.observeRecords(EARLIEST_SUPPORTED_DATE, effectiveToday.plusDays(1))
            .distinctUntilChanged()
    }
    val monthRecordsFlow = remember(selectedController, recordsMonth) {
        selectedController.observeRecords(
            recordsMonth.atDay(1),
            recordsMonth.plusMonths(1).atDay(1),
        ).distinctUntilChanged()
    }
    val backdropBrush = remember(moduleSpec) { dailyRecordBackdropBrush(moduleSpec.colors) }
    val allRecordsState by recordsFlow.collectAsStateWithLifecycle(
        initialValue = null as List<DailyCountEntry>?,
    )
    val scopedMonthRecordsState by monthRecordsFlow.collectAsStateWithLifecycle(
        initialValue = null as List<DailyCountEntry>?,
    )
    if (allRecordsState == null) {
        RecordsLoadingGate(colors = moduleSpec.colors, backdropBrush = backdropBrush)
        return
    }
    val allRecords = allRecordsState.orEmpty()
    // Until the month-scoped flow emits (or after a month switch), derive the
    // consumed month from the already-loaded full list so no frame ever shows
    // an under-filled calendar.
    val fallbackMonthRecords = remember(allRecords, recordsMonth) {
        allRecords.filter { YearMonth.from(it.localDate) == recordsMonth }
    }
    val monthRecords = scopedMonthRecordsState ?: fallbackMonthRecords

    // Today quick-log island (ADR-022). Each tap re-reads today's stored count
    // inside one lock before saving, so fast taps apply in order and a stale
    // value never overwrites a newer one. Saving reuses the module's
    // count-only path, which already trims details above the new count.
    val todayCount = allRecords.asReversed()
        .firstOrNull { it.localDate == effectiveToday }
        ?.count
    val quickLogScope = rememberCoroutineScope()
    val quickLogMutex = remember { Mutex() }
    val onQuickLog = todayQuickLogHandler(
        scope = quickLogScope,
        mutex = quickLogMutex,
        selectedController = selectedController,
        effectiveToday = effectiveToday,
        snackbarHostState = snackbarHostState,
    )

    val currentScreen: AppScreen = when {
        selectedDate != null -> AppScreen.Record(selectedDate)
        showSettings -> AppScreen.Settings
        else -> AppScreen.Main
    }

    AppScreenShell(
        currentScreen = currentScreen,
        colors = moduleSpec.colors,
    ) { screen ->
        when (screen) {
            is AppScreen.Record -> {
                // Key the record screen by module so each module keeps its own
                // saveable draft slots: a hand-brew draft must never be restored as a
                // sex draft for the same date (and vice versa).
                key(selectedModule) {
                    DailyCountRecordScreen(
                        date = screen.date,
                        today = effectiveToday,
                        controller = selectedController,
                        moduleSpec = moduleSpec,
                        monthRecords = monthRecords,
                        onBack = { selectedDateText = null },
                        // Saving is an in-place action. Keep the record page mounted so
                        // the user can see the saved feeling and continue editing this
                        // date; leaving the page remains an explicit back action.
                        onSaved = {},
                    )
                }
            }
            is AppScreen.Settings -> {
                SettingsScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    accountEmail = accountEmail,
                    syncStatus = syncStatus,
                    moduleColors = moduleSpec.colors,
                    language = language,
                    onLanguageSelected = onLanguageSelected,
                    colorTheme = colorTheme,
                    onColorThemeSelected = onColorThemeSelected,
                    terminologyStyle = terminologyStyle,
                    onTerminologyStyleSelected = onTerminologyStyleSelected,
                    appLockSettings = appLockSettings,
                    launcherDisguiseSettings = launcherDisguiseSettings,
                    onBack = { showSettings = false },
                    onOpenAccount = { showAccountDialog = true },
                    onSignIn = onSignIn,
                )
            }
            is AppScreen.Main -> {
                MainAppScaffold(
                    destination = destination,
                    moduleSpec = moduleSpec,
                    snackbarHostState = snackbarHostState,
                    showAccountDialog = showAccountDialog,
                    accountEmail = accountEmail,
                    syncStatus = syncStatus,
                    onSignIn = onSignIn,
                    onOpenAccount = { showAccountDialog = true },
                    onOpenSettings = { showSettings = true },
                    onDestinationSelected = { destinationName = it.name },
                ) { contentPadding ->
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = {
                            if (targetState == TopDestination.Statistics) {
                                (slideInHorizontally(DailyRecordMotion.spatial()) { it / 6 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutHorizontally(DailyRecordMotion.spatial()) { -it / 6 } + fadeOut(tween(140)))
                            } else {
                                (slideInHorizontally(DailyRecordMotion.spatial()) { -it / 6 } + fadeIn(tween(180)))
                                    .togetherWith(slideOutHorizontally(DailyRecordMotion.spatial()) { it / 6 } + fadeOut(tween(140)))
                            }
                        },
                        label = "topDestinationTransition",
                    ) { dest ->
                        when (dest) {
                            TopDestination.Calendar -> DailyCountCalendarScreen(
                                month = displayedMonth,
                                focusedDate = browseDate,
                                today = effectiveToday,
                                records = monthRecords,
                                moduleSpec = moduleSpec,
                                selectedModule = selectedModule,
                                availableModules = availableModuleSpecs,
                                earliestMonth = earliestSupportedMonth,
                                todayCount = todayCount,
                                onQuickLog = onQuickLog,
                                showOnboardingCard = !intimacyOnboardingDismissed,
                                onDismissOnboarding = { intimacyOnboardingPreference.dismiss() },
                                modifier = Modifier.padding(contentPadding),
                        onPreviousMonth = {
                            val previous = displayedMonth.minusMonths(1)
                            if (!previous.isBefore(earliestSupportedMonth)) {
                                browseDateText =
                                    shiftBrowseMonth(browseDate, months = -1L, effectiveToday).toString()
                            }
                        },
                        onModuleSelected = selectModule,
                        onNextMonth = {
                            val next = displayedMonth.plusMonths(1)
                            if (!next.isAfter(currentMonth)) {
                                browseDateText =
                                    shiftBrowseMonth(browseDate, months = 1L, effectiveToday).toString()
                            }
                        },
                                onToday = { browseDateText = effectiveToday.toString() },
                                onOpenDatePicker = {
                                    datePickerSelectionName = DateNavigationSelection.Date.name
                                    showDatePicker = true
                                },
                                onDateSelected = {
                                    browseDateText = it.toString()
                                    selectedDateText = it.toString()
                                },
                            )

                            TopDestination.Statistics -> DailyCountStatisticsScreen(
                                today = effectiveToday,
                                anchorDate = browseDate,
                                earliestDate = EARLIEST_SUPPORTED_DATE,
                                records = allRecords,
                                moduleSpec = moduleSpec,
                                selectedModule = selectedModule,
                                availableModules = availableModuleSpecs,
                                onModuleSelected = selectModule,
                                modifier = Modifier.padding(contentPadding),
                                onAnchorDateChanged = { browseDateText = it.toString() },
                                onOpenDatePicker = {
                                    datePickerSelectionName = DateNavigationSelection.Date.name
                                    showDatePicker = true
                                },
                                onOpenPeriodPicker = { period ->
                                    datePickerSelectionName = when (period) {
                                        StatisticsPeriod.Week -> DateNavigationSelection.Date.name
                                        StatisticsPeriod.Month -> DateNavigationSelection.Month.name
                                        StatisticsPeriod.Year -> DateNavigationSelection.Year.name
                                        StatisticsPeriod.All -> DateNavigationSelection.Date.name
                                    }
                                    showDatePicker = true
                                },
                                onOpenCalendar = { destinationName = TopDestination.Calendar.name },
                            )
                        }
                    }
                }
            }
        }
    }

    AppRootDialogs(
        showDatePicker = showDatePicker,
        browseDate = browseDate,
        effectiveToday = effectiveToday,
        colors = moduleSpec.colors,
        datePickerSelectionName = datePickerSelectionName,
        showAccountDialog = showAccountDialog,
        accountEmail = accountEmail,
        syncStatus = syncStatus,
        onSyncNow = onSyncNow,
        onSignOut = onSignOut,
        onDeleteAccount = onDeleteAccount,
        onDismissAccountDialog = { showAccountDialog = false },
        onOpenAccountDeletion = {
            showAccountDialog = false
            showAccountDeletion = true
        },
        showAccountDeletion = showAccountDeletion,
        onDismissAccountDeletion = { showAccountDeletion = false },
        onDatePickerDismiss = { showDatePicker = false },
        onDatePickerDateSelected = {
            browseDateText = it.toString()
            showDatePicker = false
        },
    )
}
