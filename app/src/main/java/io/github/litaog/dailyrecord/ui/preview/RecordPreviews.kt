package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleController
import io.github.litaog.dailyrecord.ui.components.DailyCountControl
import io.github.litaog.dailyrecord.ui.components.RollingCounter
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.record.DailyCountRecordScreen
import io.github.litaog.dailyrecord.ui.record.DetailEntryButton
import io.github.litaog.dailyrecord.ui.record.DetailTimeTarget
import io.github.litaog.dailyrecord.ui.record.PinnedStickySaveActionBar
import io.github.litaog.dailyrecord.ui.record.RecordDetailDraft
import io.github.litaog.dailyrecord.ui.record.RecordDetailsSection
import io.github.litaog.dailyrecord.ui.record.RecordTimePickerDialog
import io.github.litaog.dailyrecord.ui.uiSpec
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Pure in-memory Fake controller for Preview isolation
private class PreviewRecordModuleController(
    override val module: RecordModule = RecordModule.HandBrew,
    private val record: DailyCountEntry? = null,
    private val details: List<RecordDetailEntry> = emptyList(),
) : RecordModuleController {
    override fun observeRecord(localDate: LocalDate): Flow<DailyCountEntry?> = flowOf(record)
    override fun observeRecords(startDate: LocalDate, endExclusive: LocalDate): Flow<List<DailyCountEntry>> =
        flowOf(listOfNotNull(record))
    override suspend fun saveRecord(localDate: LocalDate, count: Int) {}
    override fun observeDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>> = flowOf(details)
    override suspend fun saveRecord(localDate: LocalDate, count: Int, details: List<RecordDetailEntry>) {}
    override suspend fun clearDetails(localDate: LocalDate, count: Int) {}
    override suspend fun clearRecord(localDate: LocalDate): Boolean = true
}

// ==========================================
// 1. Full Record Screen Previews
// ==========================================

@Preview(name = "Record Screen - Solo Clean", group = "Record Screen")
@Composable
fun RecordScreenSoloCleanPreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountRecordScreen(
            date = date,
            today = date,
            controller = PreviewRecordModuleController(module = RecordModule.HandBrew),
            moduleSpec = spec,
            monthRecords = emptyList(),
            onBack = {},
            onSaved = {},
        )
    }
}

@Preview(name = "Record Screen - Duo with Details", group = "Record Screen")
@Composable
fun RecordScreenDuoWithDetailsPreview() {
    val date = LocalDate.of(2026, 11, 8)
    val details = listOf(
        RecordDetailEntry(
            occurrenceIndex = 1,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(14, 30),
            feeling = "Cozy and relaxed",
        ),
        RecordDetailEntry(
            occurrenceIndex = 2,
            startTime = LocalTime.of(22, 15),
            endTime = LocalTime.of(22, 45),
            feeling = "Intimate and wonderful",
        ),
    )
    val entry = DailyCountEntry(date, 2)
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyCountRecordScreen(
            date = date,
            today = date,
            controller = PreviewRecordModuleController(
                module = RecordModule.Sex,
                record = entry,
                details = details,
            ),
            moduleSpec = spec,
            monthRecords = listOf(entry),
            onBack = {},
            onSaved = {},
        )
    }
}

@PreviewThemes
@Composable
fun RecordScreenThemesPreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountRecordScreen(
            date = date,
            today = date,
            controller = PreviewRecordModuleController(
                module = RecordModule.HandBrew,
                record = DailyCountEntry(date, 1),
            ),
            moduleSpec = spec,
            monthRecords = listOf(DailyCountEntry(date, 1)),
            onBack = {},
            onSaved = {},
        )
    }
}

@PreviewFontScales
@Composable
fun RecordScreenFontScalesPreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountRecordScreen(
            date = date,
            today = date,
            controller = PreviewRecordModuleController(
                module = RecordModule.HandBrew,
                record = DailyCountEntry(date, 2),
            ),
            moduleSpec = spec,
            monthRecords = listOf(DailyCountEntry(date, 2)),
            onBack = {},
            onSaved = {},
        )
    }
}

// ==========================================
// 2. Component Previews: DailyCountControl & RollingCounter
// ==========================================

@Preview(name = "Daily Count Control - Standard", group = "Record Components")
@Composable
fun DailyCountControlStandardPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountControl(
            count = 3,
            enabled = true,
            onDecrease = {},
            onIncrease = {},
            colors = spec.colors,
            compact = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Daily Count Control - Compact", group = "Record Components")
@Composable
fun DailyCountControlCompactPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyCountControl(
            count = 2,
            enabled = true,
            onDecrease = {},
            onIncrease = {},
            colors = spec.colors,
            compact = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Rolling Counter - States", group = "Record Components")
@Composable
fun RollingCounterStatesPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { _ ->
        Column(modifier = Modifier.padding(16.dp)) {
            RollingCounter(count = 0, enabled = true)
            RollingCounter(count = 7, enabled = true)
            RollingCounter(count = 42, enabled = true)
            RollingCounter(count = 0, enabled = false)
        }
    }
}

// ==========================================
// 3. Component Previews: Pinned Sticky Save Action Bar
// ==========================================

@Preview(name = "Pinned Sticky Action Bar - Clean", group = "Record Components")
@Composable
fun PinnedStickyActionBarCleanPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        PinnedStickySaveActionBar(
            saveLabel = AppCopy.Record.save,
            saveEnabled = false,
            clearVisible = false,
            clearEnabled = false,
            accent = spec.colors.primary,
            colors = spec.colors,
            onSave = {},
            onClear = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Pinned Sticky Action Bar - Dirty (Eligible to Save)", group = "Record Components")
@Composable
fun PinnedStickyActionBarDirtyPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        PinnedStickySaveActionBar(
            saveLabel = AppCopy.Record.save,
            saveEnabled = true,
            clearVisible = true,
            clearEnabled = true,
            accent = spec.colors.primary,
            colors = spec.colors,
            onSave = {},
            onClear = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ==========================================
// 4. Component Previews: Details Section & Dialogs
// ==========================================

@Preview(name = "Record Details Section", group = "Record Components")
@Composable
fun RecordDetailsSectionPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        val entries = listOf(
            RecordDetailDraft(
                occurrenceIndex = 1,
                startMinutes = 14 * 60,
                endMinutes = 14 * 60 + 30,
                feeling = "Cozy afternoon",
                feelingExpanded = false,
            ),
            RecordDetailDraft(
                occurrenceIndex = 2,
                startMinutes = 21 * 60 + 15,
                endMinutes = 21 * 60 + 45,
                feeling = "",
                feelingExpanded = true,
            ),
        )
        RecordDetailsSection(
            entries = entries,
            accent = spec.colors.primary,
            theme = spec.colors.brandIconTheme,
            onCollapse = {},
            onTimeClick = { _, _ -> },
            onFeelingToggle = {},
            onFeelingChange = { _, _ -> },
        )
    }
}

@Preview(name = "Detail Entry Button", group = "Record Components")
@Composable
fun DetailEntryButtonPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DetailEntryButton(
            count = 2,
            accent = spec.colors.primary,
            colors = spec.colors,
            onClick = {},
        )
    }
}

@Preview(name = "Record Time Picker Dialog", group = "Record Components")
@Composable
fun RecordTimePickerDialogPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        RecordTimePickerDialog(
            initialMinutes = 14 * 60 + 30,
            colors = spec.colors,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
