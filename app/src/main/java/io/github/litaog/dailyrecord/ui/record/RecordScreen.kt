package io.github.litaog.dailyrecord.ui.record

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.HandBrewModuleController
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.RecordModuleController
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.BackChevronIcon
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.DailyCountControl
import io.github.litaog.dailyrecord.ui.components.DailyRecordConfirmationDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
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
    var timePickerRequest by remember { mutableStateOf<TimePickerRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val editable = date <= today

    LaunchedEffect(dataReady, record, storedDetails) {
        if (dataReady) {
            val latestCount = record?.count ?: 0
            countDraft = countDraft.reconcile(latestCount)
            // Normalize the detail rows to the reconciled draft count: a dirty
            // count keeps its size (no remote truncation of in-progress edits)
            // and a clean count stays in sync with the server.
            detailsDraft = detailsDraft.reconcile(storedDetails, countDraft.count)
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
        countDraft = countDraft.copy(count = nextCount)
        detailsDraft = detailsDraft.resize(nextCount).let {
            if (nextCount == 0) it.copy(expanded = false) else it
        }
    }
    val launchMutation = { failureMessage: String, operation: suspend () -> Unit ->
        if (!saving) {
            saving = true
            scope.launch {
                val result = runCatchingPreservingCancellation(operation)
                saving = false
                if (result.isSuccess) {
                    // Saved and cleared drafts must not be restored when the
                    // day is opened again; the registry would otherwise keep
                    // them alive across navigation.
                    countDraft = CountDraft()
                    detailsDraft = RecordDetailsDraft()
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
        modifier = Modifier
            .fillMaxSize()
            .background(dailyRecordBackdropBrush(moduleSpec.colors))
            .testTag("record_screen"),
        containerColor = Color.Transparent,
        snackbarHost = {
            DailyRecordSnackbarHost(
                hostState = snackbarHostState,
                colors = moduleSpec.colors,
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .dailyRecordGlassBackground(moduleSpec.colors, DailyRecordGlassLevel.Muted),
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
                            val currentDetails = if (currentDraftCount == 0) {
                                emptyList()
                            } else {
                                detailsDraft.asEntries().take(currentDraftCount)
                            }
                            launchMutation(AppCopy.Record.saveFailure) {
                                controller.saveRecord(date, currentDraftCount, currentDetails)
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
        val recordScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(recordScrollState)
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
                if (detailsDraft.expanded) {
                    RecordDetailsSection(
                        entries = detailsDraft.entries,
                        accent = moduleSpec.colors.primary,
                        onCollapse = { detailsDraft = detailsDraft.copy(expanded = false) },
                        onTimeClick = { index, target ->
                            val current = detailsDraft.entries.getOrNull(index)
                            val minutes = when (target) {
                                DetailTimeTarget.Start -> current?.startMinutes
                                DetailTimeTarget.End -> current?.endMinutes
                            } ?: LocalTime.now().let { it.hour * 60 + it.minute }
                            timePickerRequest = TimePickerRequest(index, target, minutes)
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

    TimePickerHost(
        request = timePickerRequest,
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

@Composable
private fun RecordDetailsSection(
    entries: List<RecordDetailDraft>,
    accent: Color,
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
                onTimeClick = onTimeClick,
                onFeelingToggle = onFeelingToggle,
                onFeelingChange = onFeelingChange,
            )
            if (index < entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                        .height(DailyRecordBorders.Standard)
                        .background(DailyRecordDivider),
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
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TimelineNode(
            number = index + 1,
            accent = accent,
            modifier = Modifier.width(36.dp).fillMaxHeight(),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(start = DailyRecordSpacing.Inline),
        ) {
            val compactLayout = maxWidth < 340.dp || LocalDensity.current.fontScale >= 1.5f
            Column(
                modifier = Modifier.testTag("record_detail_${index + 1}"),
                verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
            ) {
                Text(
                    text = AppCopy.Record.detailOccurrence(index + 1),
                    color = DailyRecordText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (compactLayout) {
                    DetailTimeFields(
                        occurrence = index + 1,
                        entry = entry,
                        accent = accent,
                        onTimeClick = onTimeClick,
                        index = index,
                    )
                    FeelingAction(
                        occurrence = index + 1,
                        hasFeeling = entry.feeling.isNotEmpty(),
                        expanded = entry.feelingExpanded,
                        accent = accent,
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
                            onTimeClick = onTimeClick,
                            index = index,
                            modifier = Modifier.weight(1f),
                        )
                        FeelingAction(
                            occurrence = index + 1,
                            hasFeeling = entry.feeling.isNotEmpty(),
                            expanded = entry.feelingExpanded,
                            accent = accent,
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
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.Start,
            label = AppCopy.Record.detailStartTime,
            minutes = entry.startMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.Start) },
        )
        TimeRangeArrow(
            color = DailyRecordTextMuted,
            modifier = Modifier.size(width = 28.dp, height = 24.dp),
        )
        DetailTimeField(
            occurrence = occurrence,
            target = DetailTimeTarget.End,
            label = AppCopy.Record.detailEndTime,
            minutes = entry.endMinutes,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onTimeClick(index, DetailTimeTarget.End) },
        )
    }
}

@Composable
private fun TimelineNode(
    number: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val nodeRadius = 16.dp.toPx()
            drawLine(
                color = accent.copy(alpha = .28f),
                start = androidx.compose.ui.geometry.Offset(centerX, nodeRadius * 2f),
                end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
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
}

@Composable
private fun DetailTimeField(
    occurrence: Int,
    target: DetailTimeTarget,
    label: String,
    minutes: Int?,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = DailyRecordSpacing.Compact),
        )
        Row(
            modifier = Modifier
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
                        value = minutes?.let(::formatMinutes) ?: AppCopy.Record.detailTimeUnset,
                    )
                }
                .testTag("record_detail_${occurrence}_${target.name.lowercase()}_time")
                .padding(horizontal = DailyRecordSpacing.Inline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = minutes?.let(::formatMinutes) ?: AppCopy.Record.detailTimeUnset,
                color = if (minutes == null) DailyRecordTextMuted else DailyRecordText,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.weight(1f))
            ChevronIcon(
                forward = true,
                modifier = Modifier.size(18.dp),
                color = DailyRecordTextMuted,
            )
        }
    }
}

@Composable
private fun TimeRangeArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        val centerY = size.height / 2f
        val startX = size.width * .12f
        val endX = size.width * .78f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(startX, centerY),
            end = androidx.compose.ui.geometry.Offset(endX, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(endX - 5.dp.toPx(), centerY - 5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(endX, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(endX - 5.dp.toPx(), centerY + 5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(endX, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun FeelingAction(
    occurrence: Int,
    hasFeeling: Boolean,
    expanded: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = when {
        expanded -> AppCopy.Record.detailCollapseFeeling
        hasFeeling -> AppCopy.Record.detailEditFeeling
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
            .padding(horizontal = DailyRecordSpacing.Inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Compact),
    ) {
        PencilGlyph(color = accent, modifier = Modifier.size(18.dp))
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
                text = AppCopy.Record.detailFeelingCounter(value.codePointCount(0, value.length)),
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
        Box(modifier = Modifier.size(38.dp)) {
            ClockGlyph(
                color = accent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(26.dp),
            )
            SpeechGlyph(
                color = accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(19.dp),
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
        ChevronIcon(forward = true, modifier = Modifier.size(20.dp), color = accent)
    }
}

@Composable
private fun TimePickerHost(
    request: TimePickerRequest?,
    onSelected: (TimePickerRequest, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // The effect keys on the request only: later Room emissions must not
    // dismiss and re-show the dialog while the user is picking. The wheel
    // values are snapshotted into the request when it is created.
    DisposableEffect(request, context) {
        if (request == null) {
            onDispose { }
        } else {
            val dialog = TimePickerDialog(
                context,
                { _, hour, minute -> onSelected(request, hour * 60 + minute) },
                request.initialMinutes / 60,
                request.initialMinutes % 60,
                true,
            )
            dialog.setOnDismissListener { onDismiss() }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }
}

@Composable
private fun ClockGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        drawCircle(color = color, radius = size.minDimension * .38f, style = Stroke(stroke))
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * .22f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * .50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width * .70f, size.height * .62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SpeechGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        val bubble = androidx.compose.ui.geometry.Rect(
            left = size.width * .12f,
            top = size.height * .14f,
            right = size.width * .88f,
            bottom = size.height * .72f,
        )
        drawRoundRect(
            color = color,
            topLeft = bubble.topLeft,
            size = bubble.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(stroke),
        )
        val tail = Path().apply {
            moveTo(size.width * .34f, size.height * .71f)
            lineTo(size.width * .28f, size.height * .90f)
            lineTo(size.width * .50f, size.height * .72f)
        }
        drawPath(tail, color = color, style = Stroke(stroke, join = StrokeJoin.Round))
    }
}

@Composable
private fun PencilGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.7.dp.toPx()
        val shaft = Path().apply {
            moveTo(size.width * .28f, size.height * .70f)
            lineTo(size.width * .67f, size.height * .31f)
            lineTo(size.width * .82f, size.height * .46f)
            lineTo(size.width * .43f, size.height * .85f)
            close()
        }
        drawPath(
            path = shaft,
            color = color,
            style = Stroke(width = stroke, join = StrokeJoin.Round),
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * .24f, size.height * .76f),
            end = androidx.compose.ui.geometry.Offset(size.width * .17f, size.height * .90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * .17f, size.height * .90f),
            end = androidx.compose.ui.geometry.Offset(size.width * .31f, size.height * .83f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
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

private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private fun weekdayName(date: LocalDate): String = AppCopy.weekdayName(date.dayOfWeek.value)
