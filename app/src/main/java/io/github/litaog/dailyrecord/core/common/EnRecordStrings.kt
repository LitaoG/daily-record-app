package io.github.litaog.dailyrecord.core.common

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import java.time.LocalDate

internal object EnRecordStrings : RecordStrings {
        override val loading = "Loading…"
        override val saving = "Saving…"
        override val saved = "Saved"
        override val save = "Save"
        override val saveFailure = "Save failed. Retry."
        override val clear = "Clear"
        override val clearFailure = "Clear failed. Retry."
        override val countOnly = "Count only"
        override val countFirst = "Count first"
        override val countAndDetails = "Count & details"
        override val detailEntry = "Add time & notes"
        override val detailEntryHintFormat = "Add details for %d times"
        override val detailSectionTitle = "Details"
        override val detailSectionHint = "Each +1 adds one entry"
        override val detailCollapse = "Hide details"
        override val detailExpand = "Show details"
        override val detailOccurrenceFormat = "#%d"
        override val detailStartTime = "Start"
        override val detailEndTime = "End"
        override val detailStartTimeUnset = "Start time"
        override val detailEndTimeUnset = "End time"
        override val detailTimeUnset = "Pick time"
        override val detailTimePickerTitle = "Pick time"
        override val detailTimePickerSubtitle = "Swipe to set hour and minute"
        override val detailTimePickerHour = "Hour"
        override val detailTimePickerMinute = "Minute"
        override val detailTimePickerHint = "Auto-aligns when scrolling stops"
        override val detailTimePickerConfirm = "OK"
        override val detailWriteFeeling = "Add note"
        override val detailCollapseFeeling = "Hide"
        override val detailFeelingLabel = "Note (optional)"
        override val detailFeelingHint = "Write how this moment felt"
        override val detailFeelingCounter = "%d / ${MAX_RECORD_DETAIL_FEELING_CHARACTERS}"
        override val detailEndBeforeStart = "End time can't be before start time"
        override val detailDiscardTitle = "Remove this entry?"
        override val detailDiscardMessage = "Its time and note will be removed too."
        override val detailConfirmRemove = "Remove entry"
        override val detailEntryUnavailable = "Too many entries: edit total only. Existing details within range are kept."
        override val loadingRecords = "Loading records…"
        override val futureUnavailable = "Future date, can't record"
        override val notSaved = "Unset"
        override val zeroRecorded = "Recorded · 0"
        override val clearTitle = "Clear this day?"
        override val clearSubtitle = "This module only"
        override val clearMessage = "Clears to unset; not counted in stats."
        override val confirmClear = "Clear"
        override val clearDetailsFailure = "Failed to clear details. Retry."
        override val clearDetailsTitle = "Clear these details?"
        override val clearDetailsSubtitle = "Times and notes only"
        override val clearDetailsMessage = "Keeps today's count; removes times and notes."
        override val confirmClearDetails = "Clear details"
        override val discardTitle = "Discard unsaved changes?"
        override val unsavedSubtitle = "Count or details not saved"
        override val discardMessage = "Going back loses these changes."
        override val continueEditing = "Keep editing"
        override val discard = "Discard"
        override val backToCalendar = "Back to calendar"

        override fun savedStatus(count: Int): String = "Unsaved · $count"

        override fun recordedStatus(count: Int): String = "Recorded · $count"

        override fun explicitZeroHint(text: String): String = "0 means no $text that day; the record is kept."

        override fun monthSaved(month: Int): String = "${monthLabel(month)} records"

        override fun monthSummary(count: Long, days: Int): String = "$count times · $days days"

        override fun moduleRecordLabel(moduleLabel: String): String = "$moduleLabel records"

        override fun dateLabel(date: LocalDate, weekday: String): String =
            "${monthLabel(date.monthValue)} ${date.dayOfMonth} · $weekday"

        override fun detailEntryHint(count: Int): String = detailEntryHintFormat.format(count)

        override fun detailOccurrence(index: Int): String = detailOccurrenceFormat.format(index)

        override fun detailFeelingCounter(count: Int): String = detailFeelingCounter.format(count)

        override fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            "${detailOccurrence(occurrence)}, $label, $value"

        override fun detailTimeWheelCurrent(unit: String, value: String): String =
            "$unit, current $value"

        override fun detailTimeWheelOption(unit: String, value: String): String =
            "Choose $unit $value"

        override fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            "${detailOccurrence(occurrence)}, $action"

        override fun detailFeelingEditorDescription(occurrence: Int): String =
            "${detailOccurrence(occurrence)}, $detailFeelingLabel"

        private val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        private fun monthLabel(month: Int): String = monthNames.getOrElse(month - 1) { "$month" }
    }

internal object EnRecordModule : RecordModuleStrings {
        // ADR-022: English uses neutral journal wording (Solo / Duo); the
        // Chinese labels stay direct until the Play listing decision (D1).
        override val handBrewLabel = "Solo"
        override val handBrewAccessibilityLabel = "Solo journal"
        override val handBrewQuestionToday = "How many solo moments today?"
        override val handBrewQuestionPast = "How many times that day?"
        override val handBrewZero = "No solo moments that day"
        override val sexLabel = "Duo"
        override val sexAccessibilityLabel = "Intimate journal"
        override val sexQuestionToday = "How many intimate moments today?"
        override val sexQuestionPast = "How many times that day?"
        override val sexZero = "No intimate moments that day"

        override fun recordLabel(label: String): String = "$label records"
    }
