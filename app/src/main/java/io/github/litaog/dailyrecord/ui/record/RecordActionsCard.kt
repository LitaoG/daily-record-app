package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.RecordModuleController
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun RecordActionsCard(
    date: LocalDate,
    editable: Boolean,
    dataReady: Boolean,
    saving: Boolean,
    canSave: Boolean,
    record: DailyCountEntry?,
    count: Int,
    detailsDraft: RecordDetailsDraft,
    storedDetails: List<RecordDetailEntry>,
    moduleSpec: RecordModuleUiSpec,
    launchMutation: (String, () -> Unit, suspend () -> Unit) -> Unit,
    onSaved: () -> Unit,
    controller: RecordModuleController,
    onClearRequest: () -> Unit,
) {
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
                    val currentDraftCount = count
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
                onClick = onClearRequest,
            )
        }
    }
}

@Composable
internal fun RecordMonthSummaryCard(
    date: LocalDate,
    storedMonthCount: Long,
    storedMonthDays: Int,
) {
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
}
