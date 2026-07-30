package io.github.litaog.dailyrecord.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
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
        snackbarHost = { DailyRecordSnackbarHost(snackbarHostState) },
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
                            !dataReady -> "正在读取…"
                            saving -> "正在保存…"
                            !canSave && record != null -> "已保存"
                            else -> "保存记录"
                        },
                        enabled = editable && canSave && !saving,
                        onClick = {
                            if (!editable || !canSave || saving) return@PrimaryActionButton
                            val currentDraftCount = draft.count
                            launchMutation("保存失败，请重试") {
                                controller.saveRecord(date, currentDraftCount)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_record_button"),
                        accent = moduleSpec.colors.primary,
                    )
                    OutlineActionButton(
                        label = "清除记录",
                        enabled = editable && dataReady && record != null && !saving,
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        accent = moduleSpec.colors.primary,
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
            RecordHeader(date = date, onBack = requestBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 132.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(moduleSpec.colors.soft)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DailyRecordSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    moduleSpec.icon(Modifier.size(26.dp), moduleSpec.colors.primary)
                }
                Text(
                    text = if (date == today) moduleSpec.questionToday else moduleSpec.questionPast,
                    color = DailyRecordText,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    "调整次数后点击保存",
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DailyRecordSurface)
                    .border(1.dp, DailyRecordDivider, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when {
                        !dataReady -> "正在读取记录…"
                        !editable -> "未来日期 · 不可记录"
                        record == null -> "尚未填写"
                        record?.count == 0 -> "已记录 · 0 次"
                        else -> "已记录 · " + record?.count + " 次"
                    },
                    color = if (editable) moduleSpec.colors.primary else DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            DailyCountControl(
                count = draft.count,
                enabled = editable && dataReady && draft.initialized && !saving,
                hasRecord = record != null,
                onDecrease = { draft = draft.decrease() },
                onIncrease = { draft = draft.increase() },
                explicitZeroText = moduleSpec.explicitZeroText,
                positiveStateText = moduleSpec.positiveStateText,
                colors = moduleSpec.colors,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DailyRecordSurface)
                    .border(1.dp, DailyRecordDivider, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("记录规则", color = DailyRecordText, style = MaterialTheme.typography.labelLarge)
                Text(
                    "0 次＝${moduleSpec.explicitZeroText}，会保留记录。",
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text("清除记录＝恢复未填写，不进入统计。", color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DailyRecordSurfaceMuted)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    YearMonth.from(date).monthValue.toString() + "月已保存",
                    color = DailyRecordText,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "$storedMonthCount 次 · $storedMonthDays 天",
                    color = DailyRecordText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showClearDialog) {
        DailyRecordConfirmationDialog(
            title = "清除这天的记录？",
            subtitle = "记录会恢复为“未填写”",
            message = "这次操作不能在应用内撤销，也不会计入统计。",
            cancelLabel = "取消",
            confirmLabel = "确认清除",
            testTag = "clear_record_dialog",
            confirmEnabled = !saving,
            onDismiss = { if (!saving) showClearDialog = false },
            onConfirm = {
                if (!saving) {
                    showClearDialog = false
                    launchMutation("清除失败，请重试") {
                        controller.clearRecord(date)
                    }
                }
            },
        )
    }

    if (showDiscardDialog) {
        DailyRecordConfirmationDialog(
            title = "放弃未保存的修改？",
            subtitle = "当前次数还没有保存",
            message = "返回日历后，本次调整会丢失。",
            cancelLabel = "继续编辑",
            confirmLabel = "放弃修改",
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
private fun RecordHeader(date: LocalDate, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DailyRecordSurface)
                .border(1.dp, DailyRecordDivider, CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { role = Role.Button; contentDescription = "返回日历" },
            contentAlignment = Alignment.Center,
        ) {
            BackChevronIcon(color = DailyRecordText)
        }
        Text(
            text = date.monthValue.toString() + "月" + date.dayOfMonth + "日 · " + weekdayName(date),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun weekdayName(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}
