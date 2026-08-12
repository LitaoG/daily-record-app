package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.litaog.dailyrecord.BuildConfig
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.shiftMonthAnchor
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.AccountDialog
import io.github.litaog.dailyrecord.ui.account.AccountDeletionDialog
import io.github.litaog.dailyrecord.ui.account.AccountTopBar
import io.github.litaog.dailyrecord.ui.account.LocalAccountTopBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordBottomBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationDialog
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationSelection
import io.github.litaog.dailyrecord.ui.calendar.DailyCountCalendarScreen
import io.github.litaog.dailyrecord.ui.record.DailyCountRecordScreen
import io.github.litaog.dailyrecord.ui.statistics.DailyCountStatisticsScreen
import io.github.litaog.dailyrecord.ui.settings.SettingsScreen
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import java.time.LocalDate
import java.time.YearMonth

internal const val VPN_SYNC_FAILURE_MESSAGE =
    AppCopy.vpnSyncFailure

private val EarliestSupportedMonth: YearMonth = YearMonth.from(EARLIEST_SUPPORTED_DATE)

internal enum class TopDestination {
    Calendar,
    Statistics,
}

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
    onDeleteAccount: suspend (String, LocalDataAfterAccountDeletion) -> Result<Unit> = { _, _ ->
        Result.failure(IllegalStateException("Account deletion is unavailable"))
    },
) {
    val context = LocalContext.current
    val effectiveToday = today ?: rememberCurrentDate()
    val modulePreference = remember(context) { SelectedRecordModulePreference(context) }
    val handBrewController = remember(repository) { HandBrewModuleController(repository) }
    val sexController = remember(sexRepository) {
        sexRepository?.let(::SexModuleController)
    }
    val availableControllers = remember(handBrewController, sexController) {
        listOfNotNull(handBrewController, sexController)
    }
    val availableModuleSpecs = remember(availableControllers) {
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
    val datePickerSelection = DateNavigationSelection.entries.firstOrNull {
        it.name == datePickerSelectionName
    } ?: DateNavigationSelection.Date
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountDeletion by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncStatus) {
        if (
            !showAccountDialog &&
            !showSettings &&
            syncStatus is SyncStatus.Failed &&
            syncStatus.networkRelated
        ) {
            snackbarHostState.showSnackbar(
                message = AppCopy.vpnSyncFailure,
                duration = SnackbarDuration.Short,
            )
        }
    }
    LaunchedEffect(showAccountDialog) {
        if (showAccountDialog) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    val currentMonth = YearMonth.from(effectiveToday)
    val destination = TopDestination.entries.firstOrNull { it.name == destinationName }
        ?: TopDestination.Calendar
    val selectedModule = RecordModule.entries
        .firstOrNull { it.name == selectedModuleName }
        ?.takeIf { selected -> availableControllers.any { it.module == selected } }
        ?: RecordModule.HandBrew
    val selectedController = availableControllers.first { it.module == selectedModule }
    val moduleSpec = selectedModule.uiSpec()
    val selectModule: (RecordModule) -> Unit = { module ->
        if (availableControllers.any { it.module == module }) {
            selectedModuleName = module.name
            modulePreference.setSelectedModule(module)
        }
    }
    val selectedDate = selectedDateText
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeIf { it in EARLIEST_SUPPORTED_DATE..effectiveToday }
    val browseDate = runCatching { LocalDate.parse(browseDateText) }
        .getOrDefault(effectiveToday)
        .takeIf { it in EARLIEST_SUPPORTED_DATE..effectiveToday }
        ?: effectiveToday
    val displayedMonth = YearMonth.from(browseDate)
    val recordsFlow = remember(selectedController, effectiveToday) {
        selectedController.observeRecords(EARLIEST_SUPPORTED_DATE, effectiveToday.plusDays(1))
    }
    val backdropBrush = remember(moduleSpec) { dailyRecordBackdropBrush(moduleSpec.colors) }
    val allRecordsState by recordsFlow.collectAsState<List<DailyCountEntry>, List<DailyCountEntry>?>(
        initial = null,
    )
    if (allRecordsState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropBrush)
                .testTag("records_loading")
                .semantics { contentDescription = AppCopy.readingLocalRecords },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = moduleSpec.colors.primary)
        }
        return
    }
    val allRecords = allRecordsState.orEmpty()

    if (selectedDate != null) {
        // Key the record screen by module so each module keeps its own
        // saveable draft slots: a hand-brew draft must never be restored as a
        // sex draft for the same date (and vice versa).
        key(selectedModule) {
            val monthRecords = remember(allRecords, selectedDate) {
                allRecords.filter { YearMonth.from(it.localDate) == YearMonth.from(selectedDate) }
            }
            DailyCountRecordScreen(
                date = selectedDate,
                today = effectiveToday,
                controller = selectedController,
                moduleSpec = moduleSpec,
                monthRecords = monthRecords,
                onBack = { selectedDateText = null },
                onSaved = { selectedDateText = null },
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropBrush),
    ) {
        if (showSettings) {
            SettingsScreen(
                versionName = BuildConfig.VERSION_NAME,
                accountEmail = accountEmail,
                syncStatus = syncStatus,
                moduleColors = moduleSpec.colors,
                onBack = { showSettings = false },
                onOpenAccount = { showAccountDialog = true },
                onSignIn = onSignIn,
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                snackbarHost = {
                    if (!showAccountDialog) {
                        DailyRecordSnackbarHost(
                            hostState = snackbarHostState,
                            colors = moduleSpec.colors,
                        )
                    }
                },
                topBar = {
                    if (accountEmail != null) {
                        AccountTopBar(
                            status = syncStatus,
                            onClick = { showAccountDialog = true },
                            onSettings = { showSettings = true },
                        )
                    } else {
                        LocalAccountTopBar(
                            onSignIn = onSignIn,
                            onSettings = { showSettings = true },
                        )
                    }
                },
                bottomBar = {
                    DailyRecordBottomBar(
                        selected = destination,
                        colors = moduleSpec.colors,
                        onSelected = { destinationName = it.name },
                    )
                },
            ) { contentPadding ->
                when (destination) {
                    TopDestination.Calendar -> DailyCountCalendarScreen(
                        month = displayedMonth,
                        focusedDate = browseDate,
                        today = effectiveToday,
                        records = allRecords,
                        moduleSpec = moduleSpec,
                        selectedModule = selectedModule,
                        availableModules = availableModuleSpecs,
                        earliestMonth = EarliestSupportedMonth,
                        modifier = Modifier.padding(contentPadding),
                        onPreviousMonth = {
                            val previous = displayedMonth.minusMonths(1)
                            if (!previous.isBefore(EarliestSupportedMonth)) {
                                browseDateText = shiftMonthAnchor(
                                    browseDate,
                                    months = -1,
                                    earliestDate = EARLIEST_SUPPORTED_DATE,
                                    latestDate = effectiveToday,
                                ).toString()
                            }
                        },
                        onModuleSelected = selectModule,
                        onNextMonth = {
                            val next = displayedMonth.plusMonths(1)
                            if (!next.isAfter(currentMonth)) {
                                browseDateText = shiftMonthAnchor(
                                    browseDate,
                                    months = 1,
                                    earliestDate = EARLIEST_SUPPORTED_DATE,
                                    latestDate = effectiveToday,
                                ).toString()
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

    if (showDatePicker) {
        DateNavigationDialog(
            initialDate = browseDate,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            latestDate = effectiveToday,
            colors = moduleSpec.colors,
            selection = datePickerSelection,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                browseDateText = it.toString()
                showDatePicker = false
            },
        )
    }

    if (showAccountDialog && accountEmail != null) {
        AccountDialog(
            email = accountEmail,
            status = syncStatus,
            onSyncNow = onSyncNow,
            onDeleteAccount = {
                showAccountDialog = false
                showAccountDeletion = true
            },
            onSignOut = onSignOut,
            onDismiss = { showAccountDialog = false },
        )
    }
    if (showAccountDeletion && accountEmail != null) {
        AccountDeletionDialog(
            onDeleteAccount = onDeleteAccount,
            onDismiss = { showAccountDeletion = false },
        )
    }
}
