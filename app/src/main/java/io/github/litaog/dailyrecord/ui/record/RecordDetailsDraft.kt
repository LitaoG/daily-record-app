package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import io.github.litaog.dailyrecord.core.model.truncateVisibleCharacters
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import java.time.LocalTime

internal data class RecordDetailDraft(
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val feeling: String = "",
    val feelingExpanded: Boolean = false,
) {
    init {
        require(startMinutes == null || startMinutes in 0..(24 * 60 - 1)) {
            "Start time must be a minute within the day."
        }
        require(endMinutes == null || endMinutes in 0..(24 * 60 - 1)) {
            "End time must be a minute within the day."
        }
    }

    val hasContent: Boolean
        get() = startMinutes != null || endMinutes != null || feeling.isNotEmpty()

    val feelingCharacterCount: Int
        get() = feeling.codePointCount(0, feeling.length)

    fun withFeeling(value: String): RecordDetailDraft = copy(
        feeling = value.truncateVisibleCharacters(MAX_RECORD_DETAIL_FEELING_CHARACTERS),
    )

    fun withoutExpansion(): RecordDetailDraft = copy(feelingExpanded = false)

    fun toEntry(occurrenceIndex: Int): RecordDetailEntry = RecordDetailEntry(
        occurrenceIndex = occurrenceIndex,
        startTime = startMinutes?.toLocalTime(),
        endTime = endMinutes?.toLocalTime(),
        feeling = feeling,
    )

    companion object {
        internal fun fromEntry(entry: RecordDetailEntry): RecordDetailDraft = RecordDetailDraft(
            startMinutes = entry.startTime?.toMinutesOfDay(),
            endMinutes = entry.endTime?.toMinutesOfDay(),
            feeling = entry.feeling.truncateVisibleCharacters(MAX_RECORD_DETAIL_FEELING_CHARACTERS),
        )
    }
}

internal data class RecordDetailsDraft(
    val entries: List<RecordDetailDraft> = emptyList(),
    val baseline: List<RecordDetailDraft> = emptyList(),
    val initialized: Boolean = false,
    val expanded: Boolean = false,
) {
    val hasChanges: Boolean
        get() = initialized && entries.contentWithoutExpansion() != baseline.contentWithoutExpansion()

    fun reconcile(
        latest: List<RecordDetailEntry>,
        count: Int,
    ): RecordDetailsDraft {
        require(count >= 0) { "Record detail count must be non-negative." }
        val latestDraft = latest
            .sortedBy(RecordDetailEntry::occurrenceIndex)
            .map(RecordDetailDraft::fromEntry)
            .resize(count)
        // An in-progress edit keeps its own entry list: the draft count governs
        // the UI, so a concurrent remote shrink must never truncate entries the
        // user is still editing. The baseline still tracks the latest remote
        // state so a clean draft always follows the server.
        val nextEntries = if (!initialized || !hasChanges) latestDraft else entries
        return copy(
            entries = nextEntries,
            baseline = latestDraft,
            initialized = true,
        )
    }

    fun resize(count: Int): RecordDetailsDraft {
        require(count >= 0) { "Record detail count must be non-negative." }
        val resized = entries.resize(count)
        return copy(entries = resized)
    }

    fun update(index: Int, update: (RecordDetailDraft) -> RecordDetailDraft): RecordDetailsDraft {
        if (index !in entries.indices) return this
        return copy(entries = entries.toMutableList().also { items ->
            items[index] = update(items[index])
        })
    }

    fun asEntries(): List<RecordDetailEntry> = entries.mapIndexed { index, draft ->
        draft.toEntry(index + 1)
    }

    companion object {
        val Saver: Saver<RecordDetailsDraft, Any> = listSaver(
            save = { draft ->
                listOf(
                    draft.entries.map(::saveDetail),
                    draft.baseline.map(::saveDetail),
                    draft.initialized,
                    draft.expanded,
                )
            },
            restore = { values ->
                RecordDetailsDraft(
                    entries = restoreDetails(values[0]),
                    baseline = restoreDetails(values[1]),
                    initialized = values[2] as Boolean,
                    expanded = values[3] as Boolean,
                )
            },
        )

        private fun saveDetail(detail: RecordDetailDraft): List<Any?> = listOf(
            detail.startMinutes,
            detail.endMinutes,
            detail.feeling,
            detail.feelingExpanded,
        )

        private fun restoreDetails(value: Any?): List<RecordDetailDraft> =
            (value as? List<*>).orEmpty().mapNotNull { raw ->
                val cells = raw as? List<*> ?: return@mapNotNull null
                RecordDetailDraft(
                    startMinutes = cells.getOrNull(0) as? Int,
                    endMinutes = cells.getOrNull(1) as? Int,
                    feeling = cells.getOrNull(2) as? String ?: "",
                    feelingExpanded = cells.getOrNull(3) as? Boolean ?: false,
                )
            }
    }
}

private fun List<RecordDetailDraft>.resize(count: Int): List<RecordDetailDraft> = when {
    size > count -> take(count)
    size < count -> this + List(count - size) { RecordDetailDraft() }
    else -> this
}

private fun List<RecordDetailDraft>.contentWithoutExpansion(): List<RecordDetailDraft> =
    map(RecordDetailDraft::withoutExpansion)

private fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
