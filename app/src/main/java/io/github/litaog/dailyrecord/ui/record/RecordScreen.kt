package io.github.litaog.dailyrecord.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.formatMinutesOfDay
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.common.toMinutesOfDay
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.visibleCharacterCount
import io.github.litaog.dailyrecord.ui.HandBrewModuleController
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.RecordModuleController
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.BackChevronIcon
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.DailyCountControl
import io.github.litaog.dailyrecord.ui.components.DailyRecordConfirmationDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private sealed interface RecordLoadState {
    data object Loading : RecordLoadState
    data class Loaded(
        val record: DailyCountEntry?,
        val details: List<RecordDetailEntry>,
    ) : RecordLoadState
}

private enum class DetailTimeTarget {
    Start,
    End,
}

private data class TimePickerRequest(
    val index: Int,
    val target: DetailTimeTarget,
    // Snapshotted when the request is created (the picker must not move while
    // open, and reopening the same target must always start from the current
    // entry value, not from a stale first-open value).
    val initialMinutes: Int,
)

@Composable
internal fun RecordScreen(
    date: LocalDate,
    today: LocalDate,
    repository: HandBrewRecordRepository,
    monthRecords: List<HandBrewRecord>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) = DailyCountRecordScreen(
    date = date,
    today = today,
    controller = remember(repository) { HandBrewModuleController(repository) },
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
        combine(
            controller.observeRecord(date),
            controller.observeDetails(date),
        ) { record, details -> RecordLoadState.Loaded(record, details) }
    }
    val recordState by recordFlow.collectAsState(initial = RecordLoadState.Loading)
    val loadedState = recordState as? RecordLoadState.Loaded
    val record = loadedState?.record
    val storedDetails = loadedState?.details.orEmpty()
    val dataReady = loadedState != null
    var countDraft by rememberSaveable(date.toString(), stateSaver = CountDraft.Saver) {
        mutableStateOf(CountDraft())
    }
    var detailsDraft by rememberSaveable(
        date.toString(),
        stateSaver = RecordDetailsDraft.Saver,
    ) {
        mutableStateOf(RecordDetailsDraft())
    }
    var saving by remember(date) { mutableStateOf(false) }
    var showClearDialog by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var showRemoveDetailDialog by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var errorMessage by remember(date) { mutableStateOf<String?>(null) }
    var timePickerRequest by remember(date) { mutableStateOf<TimePickerRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val editable = date <= today

    LaunchedEffect(dataReady, record, storedDetails) {
        if (dataReady) {
            val latestCount = record?.count ?: 0
            val reconciledCountDraft = countDraft.reconcile(latestCount)
            countDraft = reconciledCountDraft
            // Normalize the detail rows to the reconciled draft count: a dirty
            // count keeps its size (no remote truncation of in-progress edits)
            // and a clean count stays in sync with the server.
            detailsDraft = detailsDraft.reconcile(storedDetails, reconciledCountDraft.count)
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
    val hasUnsavedChanges = dataReady && (countDraft.hasChanges || detailsDraft.hasChanges)
    val canSave = dataReady && countDraft.initialized && (record == null || hasUnsavedChanges)
    val applyCount = { nextCount: Int ->
        val previousCount = countDraft.count
        countDraft = countDraft.copy(count = nextCount)
        detailsDraft = if (
            nextCount <= MAX_RECORD_DETAIL_EDITOR_ROWS &&
            previousCount > MAX_RECORD_DETAIL_EDITOR_ROWS &&
            !detailsDraft.hasChanges
        ) {
            RecordDetailsDraft().reconcile(storedDetails, nextCount)
        } else {
            detailsDraft.resize(nextCount)
        }.let {
            if (nextCount == 0) it.copy(expanded = false) else it
        }
    }
    val launchMutation = {
        failureMessage: String,
        onSuccess: () -> Unit,
        operation: suspend () -> Unit,
    ->
        if (!saving) {
            saving = true
            scope.launch {
                val result = runCatchingPreservingCancellation(operation)
                saving = false
                if (result.isSuccess) {
                    // Close the feeling editor after a successful mutation. The
                    // caller decides whether to keep this page or leave it.
                    detailsDraft = detailsDraft.copy(
                        entries = detailsDraft.entries.map { it.copy(feelingExpanded = false) },
                    )
                    onSuccess()
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

    val backdropBrush = remember(moduleSpec) { dailyRecordBackdropBrush(moduleSpec.colors) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropBrush)
            .testTag("record_screen"),
        containerColor = Color.Transparent,
        snackbarHost = {
            DailyRecordSnackbarHost(
                hostState = snackbarHostState,
                colors = moduleSpec.colors,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(contentPadding),
        ) {
            val recordScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(recordScrollState)
                    .testTag("record_scroll_content")
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
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = when {
                        detailsDraft.expanded && countDraft.count > 0 -> AppCopy.Record.countAndDetails
                        countDraft.count > 0 -> AppCopy.Record.countFirst
                        else -> AppCopy.Record.countOnly
                    },
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                DailyCountControl(
                    count = countDraft.count,
                    enabled = editable && dataReady && countDraft.initialized && !saving,
                    onDecrease = {
                        val last = detailsDraft.entries.lastOrNull()
                            .takeIf { countDraft.count <= MAX_RECORD_DETAIL_EDITOR_ROWS }
                        if (countDraft.count > 0 && last?.hasContent == true) {
                            showRemoveDetailDialog = true
                        } else {
                            applyCount((countDraft.count - 1).coerceAtLeast(0))
                        }
                    },
                    onIncrease = {
                        if (countDraft.count < Int.MAX_VALUE) {
                            applyCount(countDraft.count + 1)
                        }
                    },
                    colors = moduleSpec.colors,
                    compact = detailsDraft.expanded,
                )
                Text(
                    text = when {
                        !dataReady -> AppCopy.Record.loadingRecords
                        !editable -> AppCopy.Record.futureUnavailable
                        hasUnsavedChanges -> AppCopy.Record.savedStatus(countDraft.count)
                        record == null -> AppCopy.Record.notSaved
                        record.count == 0 -> AppCopy.Record.zeroRecorded
                        else -> AppCopy.Record.recordedStatus(record.count)
                    },
                    color = if (editable) moduleSpec.colors.primary else DailyRecordTextMuted,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!detailsDraft.expanded) {
                    Text(
                        AppCopy.Record.explicitZeroHint(moduleSpec.explicitZeroText),
                        color = DailyRecordTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                if (countDraft.count > 0) {
                    if (countDraft.count > MAX_RECORD_DETAIL_EDITOR_ROWS) {
                        Text(
                            text = AppCopy.Record.detailEntryUnavailable,
                            color = DailyRecordTextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    } else if (detailsDraft.expanded) {
                        RecordDetailsSection(
                            entries = detailsDraft.entries,
                            accent = moduleSpec.colors.primary,
                            theme = moduleSpec.colors.brandIconTheme,
                            onCollapse = { detailsDraft = detailsDraft.copy(expanded = false) },
                            onTimeClick = { index, target ->
                                val current = detailsDraft.entries.getOrNull(index)
                                val minutes = when (target) {
                                    DetailTimeTarget.Start -> current?.startMinutes
                                    DetailTimeTarget.End -> current?.endMinutes
                                }
                                val initialMinutes = initialTimePickerMinutes(minutes, LocalTime.now())
                                timePickerRequest = TimePickerRequest(index, target, initialMinutes)
                            },
                            onFeelingToggle = { index ->
                                detailsDraft = detailsDraft.update(index) {
                                    it.copy(feelingExpanded = !it.feelingExpanded)
                                }
                            },
                            onFeelingChange = { index, value ->
                                detailsDraft = detailsDraft.update(index) { it.withFeeling(value) }
                            },
                        )
                    } else {
                        DetailEntryButton(
                            count = countDraft.count,
                            accent = moduleSpec.colors.primary,
                            colors = moduleSpec.colors,
                            onClick = {
                                detailsDraft = detailsDraft.resize(countDraft.count)
                                    .copy(expanded = true)
                            },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DailyRecordSpacing.Inline)
                        .height(1.dp)
                        .background(DailyRecordDivider),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_month_summary"),
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dailyRecordGlassBackground(moduleSpec.colors, DailyRecordGlassLevel.Muted)
                        .testTag("record_actions"),
                    color = Color.Transparent,
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
                                val currentDraftCount = countDraft.count
                                val currentDetails = when {
                                    currentDraftCount == 0 -> emptyList()
                                    currentDraftCount <= MAX_RECORD_DETAIL_EDITOR_ROWS ->
                                        detailsDraft.asEntries().take(currentDraftCount)
                                    detailsDraft.hasChanges ->
                                        (storedDetails + detailsDraft.asEntries())
                                            .filter { it.occurrenceIndex in 1..currentDraftCount }
                                            .associateBy(RecordDetailEntry::occurrenceIndex)
                                            .values
                                            .sortedBy(RecordDetailEntry::occurrenceIndex)
                                    else -> null
                                }
                                launchMutation(AppCopy.Record.saveFailure, onSaved) {
                                    if (currentDetails == null) {
                                        controller.saveRecord(date, currentDraftCount)
                                    } else {
                                        controller.saveRecord(date, currentDraftCount, currentDetails)
                                    }
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
            }
        }
    }

    TimePickerHost(
        request = timePickerRequest,
        colors = moduleSpec.colors,
        onSelected = { request, minutes ->
            val current = detailsDraft.entries.getOrNull(request.index) ?: return@TimePickerHost
            if (
                request.target == DetailTimeTarget.End &&
                current.startMinutes != null &&
                minutes < current.startMinutes
            ) {
                errorMessage = AppCopy.Record.detailEndBeforeStart
            } else {
                detailsDraft = detailsDraft.update(request.index) {
                    if (request.target == DetailTimeTarget.Start) {
                        it.copy(startMinutes = minutes)
                    } else {
                        it.copy(endMinutes = minutes)
                    }
                }
            }
            timePickerRequest = null
        },
        onDismiss = { timePickerRequest = null },
    )

    if (showClearDialog) {
        val clearDetailsOnly = detailsDraft.expanded
        DailyRecordConfirmationDialog(
            title = if (clearDetailsOnly) {
                AppCopy.Record.clearDetailsTitle
            } else {
                AppCopy.Record.clearTitle
            },
            subtitle = if (clearDetailsOnly) {
                AppCopy.Record.clearDetailsSubtitle
            } else {
                AppCopy.Record.clearSubtitle
            },
            message = if (clearDetailsOnly) {
                AppCopy.Record.clearDetailsMessage
            } else {
                AppCopy.Record.clearMessage
            },
            cancelLabel = AppCopy.Auth.cancel,
            confirmLabel = if (clearDetailsOnly) {
                AppCopy.Record.confirmClearDetails
            } else {
                AppCopy.Record.confirmClear
            },
            testTag = "clear_record_dialog",
            confirmEnabled = !saving,
            onDismiss = { if (!saving) showClearDialog = false },
            onConfirm = {
                if (!saving) {
                    showClearDialog = false
                    if (clearDetailsOnly) {
                        val currentDraftCount = countDraft.count
                        launchMutation(
                            AppCopy.Record.clearDetailsFailure,
                            { detailsDraft = detailsDraft.clearContent() },
                        ) {
                            controller.clearDetails(date, currentDraftCount)
                        }
                    } else {
                        launchMutation(AppCopy.Record.clearFailure, onBack) {
                            controller.clearRecord(date)
                        }
                    }
                }
            },
        )
    }

    if (showRemoveDetailDialog) {
        DailyRecordConfirmationDialog(
            title = AppCopy.Record.detailDiscardTitle,
            subtitle = AppCopy.Record.detailOccurrence(countDraft.count),
            message = AppCopy.Record.detailDiscardMessage,
            cancelLabel = AppCopy.Auth.cancel,
            confirmLabel = AppCopy.Record.detailConfirmRemove,
            testTag = "remove_detail_dialog",
            onDismiss = { showRemoveDetailDialog = false },
            onConfirm = {
                showRemoveDetailDialog = false
                applyCount((countDraft.count - 1).coerceAtLeast(0))
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
                // The user confirmed the edits are discarded: clear the
                // remembered drafts so reopening the day starts clean.
                countDraft = CountDraft()
                detailsDraft = RecordDetailsDraft()
                onBack()
            },
        )
    }
}

internal fun initialTimePickerMinutes(existingMinutes: Int?, now: LocalTime): Int =
    existingMinutes ?: now.toMinutesOfDay()

@Composable
private fun RecordDetailsSection(
    entries: List<RecordDetailDraft>,
    accent: Color,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onCollapse: () -> Unit,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_details_section"),
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DailyRecordBorders.Standard)
                .background(DailyRecordDivider),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                .clip(DailyRecordShapes.Control)
                .clickable(role = Role.Button, onClick = onCollapse)
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Record.detailCollapse
                }
                .padding(vertical = DailyRecordSpacing.Compact),
        ) {
            Text(
                text = AppCopy.Record.detailSectionTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Record.detailSectionHint,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        entries.forEachIndexed { index, entry ->
            RecordDetailRow(
                index = index,
                entry = entry,
                accent = accent,
                theme = theme,
                onTimeClick = onTimeClick,
                onFeelingToggle = onFeelingToggle,
                onFeelingChange = onFeelingChange,
            )
            if (index < entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DailyRecordBorders.Standard)
                        .background(DailyRecordDivider)
                        .testTag("record_detail_divider_${index + 1}"),
                )
            }
        }
    }
}

@Composable
private fun RecordDetailRow(
    index: Int,
    entry: RecordDetailDraft,
    accent: Color,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_detail_${index + 1}"),
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
        ) {
            OccurrenceBadge(number = index + 1, accent = accent)
            Text(
                text = AppCopy.Record.detailOccurrence(index + 1),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val compactLayout = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.5f
            Column(
                verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
            ) {
                if (compactLayout) {
                    DetailTimeFields(
                        occurrence = index + 1,
                        entry = entry,
                        accent = accent,
                        theme = theme,
                        onTimeClick = onTimeClick,
                        index = index,
                    )
                    FeelingAction(
                        occurrence = index + 1,
                        expanded = entry.feelingExpanded,
                        accent = accent,
                        theme = theme,
                        modifier = Modifier.align(Alignment.End),
                        onClick = { onFeelingToggle(index) },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
                    ) {
                        DetailTimeFields(
                            occurrence = index + 1,
                            entry = entry,
                            accent = accent,
                            theme = theme,
                            onTimeClick = onTimeClick,
                            index = index,
                            modifier = Modifier.weight(1f),
                        )
                        FeelingAction(
                            occurrence = index + 1,
                            expanded = entry.feelingExpanded,
                            accent = accent,
                            theme = theme,
                            onClick = { onFeelingToggle(index) },
                        )
                    }
                }
                if (entry.feelingExpanded) {
                    FeelingEditor(
                        occurrence = index + 1,
                        value = entry.feeling,
                        accent = accent,
                        onValueChange = { onFeelingChange(index, it) },
                    )
                } else if (entry.feeling.isNotEmpty()) {
                    Text(
                        text = entry.feeling,
                        color = DailyRecordTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTimeFields(
    occurrence: Int,
    entry: RecordDetailDraft,
    accent: Color,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.Start,
            label = AppCopy.Record.detailStartTime,
            placeholder = AppCopy.Record.detailStartTimeUnset,
            minutes = entry.startMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.Start) },
        )
        TimeRangeArrow(
            color = DailyRecordTextMuted,
            theme = theme,
            modifier = Modifier.size(width = 24.dp, height = 24.dp),
        )
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.End,
            label = AppCopy.Record.detailEndTime,
            placeholder = AppCopy.Record.detailEndTimeUnset,
            minutes = entry.endMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.End) },
        )
    }
}

@Composable
private fun OccurrenceBadge(
    number: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DetailTimeField(
    occurrence: Int,
    target: DetailTimeTarget,
    label: String,
    placeholder: String,
    minutes: Int?,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clip(DailyRecordShapes.Control)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                accent.copy(alpha = .20f),
                DailyRecordShapes.Control,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailTimeDescription(
                    occurrence = occurrence,
                    label = label,
                    value = minutes?.let(::formatMinutesOfDay) ?: placeholder,
                )
            }
            .testTag("record_detail_${occurrence}_${target.name.lowercase()}_time")
            .padding(horizontal = DailyRecordSpacing.Compact),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = minutes?.let(::formatMinutesOfDay) ?: placeholder,
            color = if (minutes == null) DailyRecordTextMuted else DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimeRangeArrow(
    color: Color,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    modifier: Modifier = Modifier,
) {
    BrandIcon(
        asset = BrandIconAsset.Next,
        theme = theme,
        modifier = modifier,
    )
}

@Composable
private fun FeelingAction(
    occurrence: Int,
    expanded: Boolean,
    accent: Color,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = when {
        expanded -> AppCopy.Record.detailCollapseFeeling
        else -> AppCopy.Record.detailWriteFeeling
    }
    Row(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .clip(DailyRecordShapes.Control)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                accent.copy(alpha = .62f),
                DailyRecordShapes.Control,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailFeelingActionDescription(occurrence, label)
            }
            .testTag("record_detail_${occurrence}_feeling")
            .padding(horizontal = DailyRecordSpacing.Compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        BrandIcon(
            asset = BrandIconAsset.Edit,
            theme = theme,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FeelingEditor(
    occurrence: Int,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            // Wait for the IME inset to settle before asking the scroll
                            // container to reveal the field.
                            delay(300)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
                .clip(DailyRecordShapes.Control)
                .background(DailyRecordSurface)
                .border(DailyRecordBorders.Standard, accent.copy(alpha = .65f), DailyRecordShapes.Control)
                .semantics {
                    contentDescription = AppCopy.Record.detailFeelingEditorDescription(occurrence)
                }
                .testTag("record_detail_${occurrence}_feeling_editor")
                .padding(horizontal = DailyRecordSpacing.Inline, vertical = DailyRecordSpacing.Inline),
            textStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = DailyRecordText)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = AppCopy.Record.detailFeelingHint,
                            color = DailyRecordTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DailyRecordSpacing.Compact),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = AppCopy.Record.detailFeelingCounter(value.visibleCharacterCount()),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DetailEntryButton(
    count: Int,
    accent: Color,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(DailyRecordShapes.Control)
            .dailyRecordGlass(
                shape = DailyRecordShapes.Control,
                moduleColors = colors,
                level = DailyRecordGlassLevel.Base,
                edgeColor = accent,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Record.detailEntry
            }
            .padding(horizontal = DailyRecordSpacing.Inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(DailyRecordShapes.Compact)
                .background(accent.copy(alpha = .10f))
                .border(
                    DailyRecordBorders.Standard,
                    accent.copy(alpha = .55f),
                    DailyRecordShapes.Compact,
                ),
        ) {
            BrandIcon(
                asset = BrandIconAsset.Clock,
                theme = colors.brandIconTheme,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp),
            )
            BrandIcon(
                asset = BrandIconAsset.Note,
                theme = colors.brandIconTheme,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppCopy.Record.detailEntry,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Record.detailEntryHint(count),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ChevronIcon(
            forward = true,
            modifier = Modifier.size(20.dp),
            color = accent,
            theme = colors.brandIconTheme,
        )
    }
}

@Composable
private fun TimePickerHost(
    request: TimePickerRequest?,
    colors: RecordModuleColorTokens,
    onSelected: (TimePickerRequest, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (request != null) {
        RecordTimePickerDialog(
            initialMinutes = request.initialMinutes,
            colors = colors,
            onDismiss = onDismiss,
            onConfirm = { minutes -> onSelected(request, minutes) },
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
                BackChevronIcon(
                    color = DailyRecordText,
                    theme = moduleSpec.colors.brandIconTheme,
                )
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
    accent: Color,
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
