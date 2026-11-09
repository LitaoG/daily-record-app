package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.HandBrewModuleController
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.handBrewUiSpec
import java.time.LocalDate

// Legacy single-module adapter retained for tests; production uses DailyCountRecordScreen.
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
    moduleSpec = handBrewUiSpec(),
    monthRecords = monthRecords.map(HandBrewRecord::asDailyCountEntry),
    onBack = onBack,
    onSaved = onSaved,
)
