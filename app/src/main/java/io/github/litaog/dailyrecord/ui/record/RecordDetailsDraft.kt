package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import io.github.litaog.dailyrecord.core.common.toLocalTime
import io.github.litaog.dailyrecord.core.common.toMinutesOfDay
import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import io.github.litaog.dailyrecord.core.model.truncateVisibleCharacters
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import java.time.LocalTime

internal data class RecordDetailDraft(
    val occurrenceIndex: Int = 1,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val feeling: String = "",
    val feelingExpanded: Boolean = false,
) {
    init {
        require(occurrenceIndex >= 1) {
            "Detail occurrence index must be positive."
        }
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

    fun toEntry(): RecordDetailEntry = RecordDetailEntry(
        occurrenceIndex = occurrenceIndex,
        startTime = startMinutes?.toLocalTime(),
        endTime = endMinutes?.toLocalTime(),
        feeling = feeling,
    )

    companion object {
        internal fun fromEntry(entry: RecordDetailEntry): RecordDetailDraft = RecordDetailDraft(
            occurrenceIndex = entry.occurrenceIndex,
            startMinutes = entry.startTime?.toMinutesOfDay(),
            endMinutes = entry.endTime?.toMinutesOfDay(),
            feeling = entry.feeling.truncateVisibleCharacters(MAX_RECORD_DETAIL_FEELING_CHARACTERS),
        )
    }
}

internal data class RecordDetailsDraft(
    val entries: List<RecordDetailDraft> = emptyList(),
    val baseline: List<RecordDetailDraft> = emptyList(),
    val count: Int = 0,
    val initialized: Boolean = false,
    val expanded: Boolean = false,
) {
    init {
        require(count >= 0) { "Record detail count must be non-negative." }
    }

    val hasChanges: Boolean
        get() = initialized && entries.contentWithoutExpansion() != baseline.contentWithoutExpansion()

    /**
     * Reconciles the detail rows against the latest remote state.
     *
     * @param latest the latest stored details, projected onto the editor's
     *   row boundary as the baseline the user's edits are compared against
     * @param count the caller's reconciled **draft count** (countDraft.count),
     *   not the raw remote count: when the draft is dirty this keeps the
     *   user's own row size so a remote shrink never truncates in-progress
     *   edits; when the draft is clean this normalizes the rows to the remote
     *   value (a remote grow appends blank rows, a remote shrink truncates
     *   entries beyond the authoritative count, consistent with the save
     *   path's take(count)).
     */
    fun reconcile(
        latest: List<RecordDetailEntry>,
        count: Int,
    ): RecordDetailsDraft {
        require(count >= 0) { "Record detail count must be non-negative." }
        val latestDraft = latest
            .sortedBy(RecordDetailEntry::occurrenceIndex)
            .map(RecordDetailDraft::fromEntry)
            .materializeForEditor(count)
        // count is the caller's draft count (the reconciled countDraft), not
        // the raw remote count: a dirty draft keeps its own size so a remote
        // shrink never truncates entries the user is still editing, while a
        // remote grow (with a clean count) still normalizes the row list to
        // match the displayed count.
        val nextEntries = if (!initialized || !hasChanges) latestDraft else entries.materializeForEditor(count)
        return copy(
            entries = nextEntries,
            baseline = latestDraft,
            count = count,
            initialized = true,
            expanded = expanded && count in 1..MAX_RECORD_DETAIL_EDITOR_ROWS,
        )
    }

    fun resize(count: Int): RecordDetailsDraft {
        require(count >= 0) { "Record detail count must be non-negative." }
        return copy(
            entries = entries.materializeForEditor(count),
            count = count,
            expanded = expanded && count in 1..MAX_RECORD_DETAIL_EDITOR_ROWS,
        )
    }

    fun clearContent(): RecordDetailsDraft {
        val cleared = entries.map {
            it.copy(
                startMinutes = null,
                endMinutes = null,
                feeling = "",
                feelingExpanded = false,
            )
        }
        return copy(
            entries = cleared,
            baseline = cleared,
            initialized = true,
        )
    }

    fun update(index: Int, update: (RecordDetailDraft) -> RecordDetailDraft): RecordDetailsDraft {
        if (index !in entries.indices) return this
        return copy(entries = entries.toMutableList().also { items ->
            items[index] = update(items[index])
        })
    }

    fun asEntries(): List<RecordDetailEntry> = entries
        .map(RecordDetailDraft::toEntry)
        .sortedBy(RecordDetailEntry::occurrenceIndex)

    companion object {
        val Saver: Saver<RecordDetailsDraft, Any> = listSaver(
            save = { draft ->
                listOf(
                    draft.entries.map(::saveDetail),
                    draft.baseline.map(::saveDetail),
                    draft.count,
                    draft.initialized,
                    draft.expanded,
                )
            },
            restore = { values ->
                val currentFormat = values.getOrNull(2) is Int
                val restoredEntries = restoreDetails(values.getOrNull(0))
                val restoredBaseline = restoreDetails(values.getOrNull(1))
                RecordDetailsDraft(
                    entries = restoredEntries,
                    baseline = restoredBaseline,
                    count = if (currentFormat) values[2] as Int else restoredEntries.size,
                    initialized = if (currentFormat) {
                        values.getOrNull(3) as? Boolean ?: false
                    } else {
                        values.getOrNull(2) as? Boolean ?: false
                    },
                    expanded = if (currentFormat) {
                        values.getOrNull(4) as? Boolean ?: false
                    } else {
                        values.getOrNull(3) as? Boolean ?: false
                    },
                )
            },
        )

        private fun saveDetail(detail: RecordDetailDraft): List<Any?> = listOf(
            detail.occurrenceIndex,
            detail.startMinutes,
            detail.endMinutes,
            detail.feeling,
            detail.feelingExpanded,
        )

        private fun restoreDetails(value: Any?): List<RecordDetailDraft> =
            (value as? List<*>).orEmpty().mapIndexedNotNull { index, raw ->
                val cells = raw as? List<*> ?: return@mapIndexedNotNull null
                val hasOccurrenceIndex = cells.size >= 5 && cells[0] is Int
                RecordDetailDraft(
                    occurrenceIndex = if (hasOccurrenceIndex) cells[0] as Int else index + 1,
                    startMinutes = cells.getOrNull(if (hasOccurrenceIndex) 1 else 0) as? Int,
                    endMinutes = cells.getOrNull(if (hasOccurrenceIndex) 2 else 1) as? Int,
                    feeling = cells.getOrNull(if (hasOccurrenceIndex) 3 else 2) as? String ?: "",
                    feelingExpanded = cells.getOrNull(if (hasOccurrenceIndex) 4 else 3) as? Boolean ?: false,
                )
            }
    }
}

/**
 * Aggregate counts intentionally remain Int-sized, but the optional detail
 * editor must never allocate one Compose row per untrusted count value.
 * Counts above this boundary keep their aggregate value and any existing
 * detail data, while the editor remains collapsed until the count is small
 * enough to materialize safely.
 */
internal const val MAX_RECORD_DETAIL_EDITOR_ROWS = 512

private fun List<RecordDetailDraft>.materializeForEditor(count: Int): List<RecordDetailDraft> {
    if (count > MAX_RECORD_DETAIL_EDITOR_ROWS) {
        return filter { it.occurrenceIndex in 1..MAX_RECORD_DETAIL_EDITOR_ROWS }
            .sortedBy(RecordDetailDraft::occurrenceIndex)
    }
    val byOccurrence = associateBy(RecordDetailDraft::occurrenceIndex)
    return List(count) { index ->
        byOccurrence[index + 1] ?: RecordDetailDraft(occurrenceIndex = index + 1)
    }
}

private fun List<RecordDetailDraft>.contentWithoutExpansion(): List<RecordDetailDraft> =
    map(RecordDetailDraft::withoutExpansion)
