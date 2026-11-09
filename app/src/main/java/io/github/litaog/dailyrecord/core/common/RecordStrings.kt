package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate

internal interface RecordStrings {
        val loading: String
        val saving: String
        val saved: String
        val save: String
        val saveFailure: String
        val clear: String
        val clearFailure: String
        val countOnly: String
        val countFirst: String
        val countAndDetails: String
        val detailEntry: String
        val detailEntryHintFormat: String
        val detailSectionTitle: String
        val detailSectionHint: String
        val detailCollapse: String
        val detailExpand: String
        val detailOccurrenceFormat: String
        val detailStartTime: String
        val detailEndTime: String
        val detailStartTimeUnset: String
        val detailEndTimeUnset: String
        val detailTimeUnset: String
        val detailTimePickerTitle: String
        val detailTimePickerSubtitle: String
        val detailTimePickerHour: String
        val detailTimePickerMinute: String
        val detailTimePickerHint: String
        val detailTimePickerConfirm: String
        val detailWriteFeeling: String
        val detailCollapseFeeling: String
        val detailFeelingLabel: String
        val detailFeelingHint: String
        val detailFeelingCounter: String
        val detailEndBeforeStart: String
        val detailDiscardTitle: String
        val detailDiscardMessage: String
        val detailConfirmRemove: String
        val detailEntryUnavailable: String
        val loadingRecords: String
        val futureUnavailable: String
        val notSaved: String
        val zeroRecorded: String
        val clearTitle: String
        val clearSubtitle: String
        val clearMessage: String
        val confirmClear: String
        val clearDetailsFailure: String
        val clearDetailsTitle: String
        val clearDetailsSubtitle: String
        val clearDetailsMessage: String
        val confirmClearDetails: String
        val discardTitle: String
        val unsavedSubtitle: String
        val discardMessage: String
        val continueEditing: String
        val discard: String
        val backToCalendar: String

        fun savedStatus(count: Int): String

        fun recordedStatus(count: Int): String

        fun explicitZeroHint(text: String): String

        fun monthSaved(month: Int): String

        fun monthSummary(count: Long, days: Int): String

        fun moduleRecordLabel(moduleLabel: String): String

        fun dateLabel(date: LocalDate, weekday: String): String

        fun detailEntryHint(count: Int): String

        fun detailOccurrence(index: Int): String

        fun detailFeelingCounter(count: Int): String

        fun detailTimeDescription(occurrence: Int, label: String, value: String): String

        fun detailTimeWheelCurrent(unit: String, value: String): String

        fun detailTimeWheelOption(unit: String, value: String): String

        fun detailFeelingActionDescription(occurrence: Int, action: String): String

        fun detailFeelingEditorDescription(occurrence: Int): String
    }

internal interface RecordModuleStrings {
        val handBrewLabel: String
        val handBrewAccessibilityLabel: String
        val handBrewQuestionToday: String
        val handBrewQuestionPast: String
        val handBrewZero: String
        val sexLabel: String
        val sexAccessibilityLabel: String
        val sexQuestionToday: String
        val sexQuestionPast: String
        val sexZero: String

        fun recordLabel(label: String): String
    }
