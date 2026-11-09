package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate

internal interface AppCopyRecord {
        val loading: String get() = AppLanguageState.current.record.loading
        val saving: String get() = AppLanguageState.current.record.saving
        val saved: String get() = AppLanguageState.current.record.saved
        val save: String get() = AppLanguageState.current.record.save
        val saveFailure: String get() = AppLanguageState.current.record.saveFailure
        val clear: String get() = AppLanguageState.current.record.clear
        val clearFailure: String get() = AppLanguageState.current.record.clearFailure
        val countOnly: String get() = AppLanguageState.current.record.countOnly
        val countFirst: String get() = AppLanguageState.current.record.countFirst
        val countAndDetails: String get() = AppLanguageState.current.record.countAndDetails
        val detailEntry: String get() = AppLanguageState.current.record.detailEntry
        val detailEntryHintFormat: String get() = AppLanguageState.current.record.detailEntryHintFormat
        val detailSectionTitle: String get() = AppLanguageState.current.record.detailSectionTitle
        val detailSectionHint: String get() = AppLanguageState.current.record.detailSectionHint
        val detailCollapse: String get() = AppLanguageState.current.record.detailCollapse
        val detailExpand: String get() = AppLanguageState.current.record.detailExpand
        val detailOccurrenceFormat: String get() = AppLanguageState.current.record.detailOccurrenceFormat
        val detailStartTime: String get() = AppLanguageState.current.record.detailStartTime
        val detailEndTime: String get() = AppLanguageState.current.record.detailEndTime
        val detailStartTimeUnset: String get() = AppLanguageState.current.record.detailStartTimeUnset
        val detailEndTimeUnset: String get() = AppLanguageState.current.record.detailEndTimeUnset
        val detailTimeUnset: String get() = AppLanguageState.current.record.detailTimeUnset
        val detailTimePickerTitle: String get() = AppLanguageState.current.record.detailTimePickerTitle
        val detailTimePickerSubtitle: String get() = AppLanguageState.current.record.detailTimePickerSubtitle
        val detailTimePickerHour: String get() = AppLanguageState.current.record.detailTimePickerHour
        val detailTimePickerMinute: String get() = AppLanguageState.current.record.detailTimePickerMinute
        val detailTimePickerHint: String get() = AppLanguageState.current.record.detailTimePickerHint
        val detailTimePickerConfirm: String get() = AppLanguageState.current.record.detailTimePickerConfirm
        val detailWriteFeeling: String get() = AppLanguageState.current.record.detailWriteFeeling
        val detailCollapseFeeling: String get() = AppLanguageState.current.record.detailCollapseFeeling
        val detailFeelingLabel: String get() = AppLanguageState.current.record.detailFeelingLabel
        val detailFeelingHint: String get() = AppLanguageState.current.record.detailFeelingHint
        val detailFeelingCounter: String get() = AppLanguageState.current.record.detailFeelingCounter
        val detailEndBeforeStart: String get() = AppLanguageState.current.record.detailEndBeforeStart
        val detailDiscardTitle: String get() = AppLanguageState.current.record.detailDiscardTitle
        val detailDiscardMessage: String get() = AppLanguageState.current.record.detailDiscardMessage
        val detailConfirmRemove: String get() = AppLanguageState.current.record.detailConfirmRemove
        val detailEntryUnavailable: String get() = AppLanguageState.current.record.detailEntryUnavailable
        val loadingRecords: String get() = AppLanguageState.current.record.loadingRecords
        val futureUnavailable: String get() = AppLanguageState.current.record.futureUnavailable
        val notSaved: String get() = AppLanguageState.current.record.notSaved
        val zeroRecorded: String get() = AppLanguageState.current.record.zeroRecorded
        val clearTitle: String get() = AppLanguageState.current.record.clearTitle
        val clearSubtitle: String get() = AppLanguageState.current.record.clearSubtitle
        val clearMessage: String get() = AppLanguageState.current.record.clearMessage
        val confirmClear: String get() = AppLanguageState.current.record.confirmClear
        val clearDetailsFailure: String get() = AppLanguageState.current.record.clearDetailsFailure
        val clearDetailsTitle: String get() = AppLanguageState.current.record.clearDetailsTitle
        val clearDetailsSubtitle: String get() = AppLanguageState.current.record.clearDetailsSubtitle
        val clearDetailsMessage: String get() = AppLanguageState.current.record.clearDetailsMessage
        val confirmClearDetails: String get() = AppLanguageState.current.record.confirmClearDetails
        val discardTitle: String get() = AppLanguageState.current.record.discardTitle
        val unsavedSubtitle: String get() = AppLanguageState.current.record.unsavedSubtitle
        val discardMessage: String get() = AppLanguageState.current.record.discardMessage
        val continueEditing: String get() = AppLanguageState.current.record.continueEditing
        val discard: String get() = AppLanguageState.current.record.discard
        val backToCalendar: String get() = AppLanguageState.current.record.backToCalendar

        fun savedStatus(count: Int): String = AppLanguageState.current.record.savedStatus(count)

        fun recordedStatus(count: Int): String = AppLanguageState.current.record.recordedStatus(count)

        fun explicitZeroHint(text: String): String =
            AppLanguageState.current.record.explicitZeroHint(text)

        fun monthSaved(month: Int): String = AppLanguageState.current.record.monthSaved(month)

        fun monthSummary(count: Long, days: Int): String =
            AppLanguageState.current.record.monthSummary(count, days)

        fun moduleRecordLabel(moduleLabel: String): String =
            AppLanguageState.current.record.moduleRecordLabel(moduleLabel)

        fun dateLabel(date: LocalDate, weekday: String): String =
            AppLanguageState.current.record.dateLabel(date, weekday)

        fun detailEntryHint(count: Int): String = AppLanguageState.current.record.detailEntryHint(count)

        fun detailOccurrence(index: Int): String = AppLanguageState.current.record.detailOccurrence(index)

        fun detailFeelingCounter(count: Int): String =
            AppLanguageState.current.record.detailFeelingCounter(count)

        fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            AppLanguageState.current.record.detailTimeDescription(occurrence, label, value)

        fun detailTimeWheelCurrent(unit: String, value: String): String =
            AppLanguageState.current.record.detailTimeWheelCurrent(unit, value)

        fun detailTimeWheelOption(unit: String, value: String): String =
            AppLanguageState.current.record.detailTimeWheelOption(unit, value)

        fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            AppLanguageState.current.record.detailFeelingActionDescription(occurrence, action)

        fun detailFeelingEditorDescription(occurrence: Int): String =
            AppLanguageState.current.record.detailFeelingEditorDescription(occurrence)
    }

internal interface AppCopyRecordModule {
        val handBrewLabel: String get() = AppLanguageState.current.recordModule.handBrewLabel
        val handBrewAccessibilityLabel: String get() =
            AppLanguageState.current.recordModule.handBrewAccessibilityLabel
        val handBrewQuestionToday: String get() = AppLanguageState.current.recordModule.handBrewQuestionToday
        val handBrewQuestionPast: String get() = AppLanguageState.current.recordModule.handBrewQuestionPast
        val handBrewZero: String get() = AppLanguageState.current.recordModule.handBrewZero
        val sexLabel: String get() = AppLanguageState.current.recordModule.sexLabel
        val sexAccessibilityLabel: String get() =
            AppLanguageState.current.recordModule.sexAccessibilityLabel
        val sexQuestionToday: String get() = AppLanguageState.current.recordModule.sexQuestionToday
        val sexQuestionPast: String get() = AppLanguageState.current.recordModule.sexQuestionPast
        val sexZero: String get() = AppLanguageState.current.recordModule.sexZero

        fun recordLabel(label: String): String =
            AppLanguageState.current.recordModule.recordLabel(label)
    }
