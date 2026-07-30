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
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.AccountDialog
import io.github.litaog.dailyrecord.ui.account.AccountDeletionDialog
import io.github.litaog.dailyrecord.ui.account.AccountTopBar
import io.github.litaog.dailyrecord.ui.account.LocalAccountTopBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordBottomBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.diagnostics.DiagnosticDialog
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationDialog
import io.github.litaog.dailyrecord.ui.navigation.shiftMonthAnchor
import io.github.litaog.dailyrecord.ui.calendar.DailyCountCalendarScreen
import io.github.litaog.dailyrecord.ui.record.DailyCountRecordScreen
import io.github.litaog.dailyrecord.ui.statistics.DailyCountStatisticsScreen
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import java.time.LocalDate
import java.time.YearMonth

private val EarliestSupportedDate: LocalDate = LocalDate.of(1970, 1, 1)
private val EarliestSupportedMonth: YearMonth = YearMonth.from(EarliestSupportedDate)

internal const val VPN_SYNC_FAILURE_MESSAGE =
    "云同步需要打开 VPN（梯子）。记录已保存在本机，请开启后重试。"

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
    diagnosticReport: String = "诊断信息暂不可用",
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
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    var showAccountDeletion by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncStatus) {
        if (
            !showAccountDialog &&
            syncStatus is SyncStatus.Failed &&
            syncStatus.networkRelated
        ) {
            snackbarHostState.showSnackbar(
                message = VPN_SYNC_FAILURE_MESSAGE,
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
        ?.takeIf { it in EarliestSupportedDate..effectiveToday }
    val browseDate = runCatching { LocalDate.parse(browseDateText) }
        .getOrDefault(effectiveToday)
        .takeIf { it in EarliestSupportedDate..effectiveToday }
        ?: effectiveToday
    val displayedMonth = YearMonth.from(browseDate)
    val recordsFlow = remember(selectedController, effectiveToday) {
        selectedController.observeRecords(EarliestSupportedDate, effectiveToday.plusDays(1))
    }
    val allRecordsState by recordsFlow.collectAsState<List<DailyCountEntry>, List<DailyCountEntry>?>(
        initial = null,
    )
    if (allRecordsState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DailyRecordCanvas)
                .testTag("records_loading")
                .semantics { contentDescription = "正在读取本机记录" },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = moduleSpec.colors.primary)
        }
        return
    }
    val allRecords = allRecordsState.orEmpty()

    if (selectedDate != null) {
        DailyCountRecordScreen(
            date = selectedDate,
            today = effectiveToday,
            controller = selectedController,
            moduleSpec = moduleSpec,
            monthRecords = allRecords.filter { YearMonth.from(it.localDate) == YearMonth.from(selectedDate) },
            onBack = { selectedDateText = null },
            onSaved = { selectedDateText = null },
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DailyRecordCanvas,
        snackbarHost = {
            if (!showAccountDialog) {
                DailyRecordSnackbarHost(snackbarHostState)
            }
        },
        topBar = {
            if (accountEmail != null) {
                AccountTopBar(status = syncStatus, onClick = { showAccountDialog = true })
            } else if (onSignIn != null) {
                LocalAccountTopBar(
                    onClick = onSignIn,
                    onDiagnostics = { showDiagnostics = true },
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
                            earliestDate = EarliestSupportedDate,
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
                            earliestDate = EarliestSupportedDate,
                            latestDate = effectiveToday,
                        ).toString()
                    }
                },
                onToday = { browseDateText = effectiveToday.toString() },
                onOpenDatePicker = { showDatePicker = true },
                onDateSelected = {
                    browseDateText = it.toString()
                    selectedDateText = it.toString()
                },
            )

            TopDestination.Statistics -> DailyCountStatisticsScreen(
                today = effectiveToday,
                anchorDate = browseDate,
                earliestDate = EarliestSupportedDate,
                records = allRecords,
                moduleSpec = moduleSpec,
                selectedModule = selectedModule,
                availableModules = availableModuleSpecs,
                onModuleSelected = selectModule,
                modifier = Modifier.padding(contentPadding),
                onAnchorDateChanged = { browseDateText = it.toString() },
                onOpenDatePicker = { showDatePicker = true },
                onOpenCalendar = { destinationName = TopDestination.Calendar.name },
            )
        }
    }

    if (showDatePicker) {
        DateNavigationDialog(
            initialDate = browseDate,
            earliestDate = EarliestSupportedDate,
            latestDate = effectiveToday,
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
            onOpenDiagnostics = {
                showAccountDialog = false
                showDiagnostics = true
            },
            onDeleteAccount = {
                showAccountDialog = false
                showAccountDeletion = true
            },
            onSignOut = onSignOut,
            onDismiss = { showAccountDialog = false },
        )
    }
    if (showDiagnostics) {
        DiagnosticDialog(
            report = diagnosticReport,
            onDismiss = { showDiagnostics = false },
        )
    }
    if (showAccountDeletion && accountEmail != null) {
        AccountDeletionDialog(
            onDeleteAccount = onDeleteAccount,
            onDismiss = { showAccountDeletion = false },
        )
    }
}
