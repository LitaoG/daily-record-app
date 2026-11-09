package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.Composable
import io.github.litaog.dailyrecord.core.common.toMinutesOfDay
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalTime

internal sealed interface RecordLoadState {
    data object Loading : RecordLoadState
    data class Loaded(
        val record: DailyCountEntry?,
        val details: List<RecordDetailEntry>,
    ) : RecordLoadState
}

internal data class TimePickerRequest(
    val index: Int,
    val target: DetailTimeTarget,
    // Snapshotted when the request is created (the picker must not move while
    // open, and reopening the same target must always start from the current
    // entry value, not from a stale first-open value).
    val initialMinutes: Int,
)

@Composable
internal fun TimePickerHost(
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

internal fun initialTimePickerMinutes(existingMinutes: Int?, now: LocalTime): Int =
    existingMinutes ?: now.toMinutesOfDay()

/**
 * Whether decreasing the count must first confirm removal of the last detail
 * row. At or below the editor materialization limit the draft tail decides;
 * above it the draft holds no rows, so a stored tail row with content gets
 * the same confirmation instead of being dropped silently.
 */
internal fun shouldConfirmDetailRemoval(
    count: Int,
    editorRowLimit: Int,
    draftTailHasContent: Boolean,
    storedDetails: List<RecordDetailEntry>,
): Boolean {
    if (count <= 0) return false
    if (count <= editorRowLimit) return draftTailHasContent
    return storedDetails.any {
        it.occurrenceIndex == count &&
            (it.startTime != null || it.endTime != null || it.feeling.isNotEmpty())
    }
}
