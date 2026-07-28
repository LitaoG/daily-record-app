package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Keeps the editable count and its last observed Room value together.
 *
 * Remote updates replace a clean draft, but never overwrite an in-progress local edit.
 */
internal data class CountDraft(
    val count: Int = 0,
    val baseline: Int = 0,
    val initialized: Boolean = false,
) {
    init {
        require(count >= 0) { "Draft count must be non-negative." }
        require(baseline >= 0) { "Draft baseline must be non-negative." }
    }

    val hasChanges: Boolean
        get() = initialized && count != baseline

    fun reconcile(latestCount: Int): CountDraft {
        require(latestCount >= 0) { "Latest count must be non-negative." }
        return copy(
            count = if (!initialized || !hasChanges) latestCount else count,
            baseline = latestCount,
            initialized = true,
        )
    }

    fun decrease(): CountDraft = copy(count = (count - 1).coerceAtLeast(0))

    fun increase(): CountDraft =
        if (count == Int.MAX_VALUE) this else copy(count = count + 1)

    companion object {
        val Saver: Saver<CountDraft, Any> = listSaver(
            save = { listOf(it.count, it.baseline, it.initialized) },
            restore = {
                CountDraft(
                    count = it[0] as Int,
                    baseline = it[1] as Int,
                    initialized = it[2] as Boolean,
                )
            },
        )
    }
}
