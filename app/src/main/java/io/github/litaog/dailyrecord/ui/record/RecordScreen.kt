package io.github.litaog.dailyrecord.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.HandBrewModuleController
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordModuleController
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.DailyCountControl
import io.github.litaog.dailyrecord.ui.components.BackChevronIcon
import io.github.litaog.dailyrecord.ui.components.DailyRecordConfirmationDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private sealed interface RecordLoadState {
    data object Loading : RecordLoadState
    data class Loaded(val record: DailyCountEntry?) : RecordLoadState
}

@Composable
fun RecordScreen(
    date: LocalDate,
    today: LocalDate,
    repository: HandBrewRecordRepository,
    monthRecords: List<HandBrewRecord>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) = DailyCountRecordScreen(
    date = date,
    today = today,
    controller = HandBrewModuleController(repository),
    moduleSpec = HandBrewModuleSpec,
    monthRecords = monthRecords.map(HandBrewRecord::asDailyCountEntry),
    onBack = onBack,
    onSaved = onSaved,
)

@Composable
internal fun DailyCountRecordScreen(
    date: LocalDate,
    today: LocalDate,
    controller: RecordModuleController,
    moduleSpec: RecordModuleUiSpec,
    monthRecords: List<DailyCountEntry>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val recordFlow = remember(controller, date) {
        controller.observeRecord(date).map<DailyCountEntry?, RecordLoadState> {
            RecordLoadState.Loaded(it)
        }
    }
    val recordState by recordFlow.collectAsState(initial = RecordLoadState.Loading)
    val loadedState = recordState as? RecordLoadState.Loaded
    val record = loadedState?.record
    val dataReady = loadedState != null
    var draft by rememberSaveable(date.toString(), stateSaver = CountDraft.Saver) {
        mutableStateOf(CountDraft())
    }
    var saving by remember(date) { mutableStateOf(false) }
    var showClearDialog by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var errorMessage by rememberSaveable(date.toString()) { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val editable = date <= today

    LaunchedEffect(dataReady, record) {
        if (dataReady) {
            draft = draft.reconcile(record?.count ?: 0)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val storedMonthCount = monthRecords.sumOf { it.count.toLong() }
    val storedMonthDays = monthRecords.count { it.count > 0 }
    val hasUnsavedChanges = dataReady && draft.hasChanges
    val canSave = dataReady && draft.initialized && (record == null || hasUnsavedChanges)
    val launchMutation = { failureMessage: String, operation: suspend () -> Unit ->
        if (!saving) {
            saving = true
            scope.launch {
                val result = runCatchingPreservingCancellation(operation)
                saving = false
                if (result.isSuccess) {
                    onSaved()
                } else {
                    errorMessage = failureMessage
                }
            }
        }
    }
    val requestBack = {
        when {
            saving -> Unit
            hasUnsavedChanges -> showDiscardDialog = true
            else -> onBack()
        }
    }

    BackHandler(onBack = requestBack)

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("record_screen"),
        containerColor = DailyRecordCanvas,
        snackbarHost = {
            DailyRecordSnackbarHost(
                hostState = snackbarHostState,
                colors = moduleSpec.colors,
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = DailyRecordCanvas,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DailyRecordSpacing.ScreenHorizontal,
                            vertical = DailyRecordSpacing.Content,
                        ),
                    verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
                ) {
                    PrimaryActionButton(
                        label = when {
                            !dataReady -> AppCopy.Record.loading
                            saving -> AppCopy.Record.saving
                            !canSave && record != null -> AppCopy.Record.saved
                            else -> AppCopy.Record.save
                        },
                        enabled = editable && canSave && !saving,
                        onClick = {
                            if (!editable || !canSave || saving) return@PrimaryActionButton
                            val currentDraftCount = draft.count
                            launchMutation(AppCopy.Record.saveFailure) {
                                controller.saveRecord(date, currentDraftCount)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_record_button"),
                        accent = moduleSpec.colors.primary,
                    )
                    RecordTextAction(
                        label = AppCopy.Record.clear,
                        enabled = editable && dataReady && record != null && !saving,
                        accent = moduleSpec.colors.primary,
                        onClick = { showClearDialog = true },
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(
                    horizontal = DailyRecordSpacing.ScreenHorizontal,
                    vertical = DailyRecordSpacing.ScreenVertical,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
        ) {
            RecordHeader(date = date, today = today, moduleSpec = moduleSpec, onBack = requestBack)
            Text(
                text = if (date == today) moduleSpec.questionToday else moduleSpec.questionPast,
                color = DailyRecordText,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                AppCopy.Record.countOnly,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            DailyCountControl(
                count = draft.count,
                enabled = editable && dataReady && draft.initialized && !saving,
                onDecrease = { draft = draft.decrease() },
                onIncrease = { draft = draft.increase() },
                colors = moduleSpec.colors,
            )
            Text(
                text = when {
                    !dataReady -> AppCopy.Record.loadingRecords
                    !editable -> AppCopy.Record.futureUnavailable
                    hasUnsavedChanges -> AppCopy.Record.savedStatus(draft.count)
                    record == null -> AppCopy.Record.notSaved
                    record.count == 0 -> AppCopy.Record.zeroRecorded
                    else -> AppCopy.Record.recordedStatus(record.count)
                },
                color = if (editable) moduleSpec.colors.primary else DailyRecordTextMuted,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                AppCopy.Record.explicitZeroHint(moduleSpec.explicitZeroText),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DailyRecordSpacing.Inline)
                    .height(1.dp)
                    .background(DailyRecordDivider),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
            ) {
                Text(
                    text = AppCopy.Record.monthSaved(YearMonth.from(date).monthValue),
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    AppCopy.Record.monthSummary(storedMonthCount, storedMonthDays),
                    color = DailyRecordText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showClearDialog) {
        DailyRecordConfirmationDialog(
            title = AppCopy.Record.clearTitle,
            subtitle = AppCopy.Record.clearSubtitle,
            message = AppCopy.Record.clearMessage,
            cancelLabel = AppCopy.Auth.cancel,
            confirmLabel = AppCopy.Record.confirmClear,
            testTag = "clear_record_dialog",
            confirmEnabled = !saving,
            onDismiss = { if (!saving) showClearDialog = false },
            onConfirm = {
                if (!saving) {
                    showClearDialog = false
                    launchMutation(AppCopy.Record.clearFailure) {
                        controller.clearRecord(date)
                    }
                }
            },
        )
    }

    if (showDiscardDialog) {
        DailyRecordConfirmationDialog(
            title = AppCopy.Record.discardTitle,
            subtitle = AppCopy.Record.unsavedSubtitle,
            message = AppCopy.Record.discardMessage,
            cancelLabel = AppCopy.Record.continueEditing,
            confirmLabel = AppCopy.Record.discard,
            testTag = "discard_record_dialog",
            onDismiss = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
        )
    }
}

@Composable
private fun RecordHeader(
    date: LocalDate,
    today: LocalDate,
    moduleSpec: RecordModuleUiSpec,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(DailyRecordSizes.MinimumTouchTarget)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { role = Role.Button; contentDescription = AppCopy.Record.backToCalendar },
                contentAlignment = Alignment.Center,
            ) {
                BackChevronIcon(color = DailyRecordText)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = AppCopy.Record.dateLabel(date, weekdayName(date)),
                    color = DailyRecordText,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = when {
                        date == today -> AppCopy.today
                        date < today -> AppCopy.historyDate
                        else -> AppCopy.futureDate
                    },
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            moduleSpec.icon(Modifier.size(DailyRecordSizes.ModuleIcon), moduleSpec.colors.primary)
            Text(
                text = AppCopy.Record.moduleRecordLabel(moduleSpec.label),
                color = moduleSpec.colors.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RecordTextAction(
    label: String,
    enabled: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) accent else DailyRecordTextMuted,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun weekdayName(date: LocalDate): String = AppCopy.weekdayName(date.dayOfWeek.value)
