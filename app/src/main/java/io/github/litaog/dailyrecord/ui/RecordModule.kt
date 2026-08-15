package io.github.litaog.dailyrecord.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.DailyCountRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.DailyCountRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.core.model.RecordFactory
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import io.github.litaog.dailyrecord.ui.components.HandBrewIcon
import io.github.litaog.dailyrecord.ui.components.SexIcon
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal enum class RecordModule {
    HandBrew,
    Sex,
}

internal data class RecordModuleUiSpec(
    val module: RecordModule,
    val label: String,
    val questionToday: String,
    val questionPast: String,
    val explicitZeroText: String,
    val semanticCountLabel: String,
    val colors: RecordModuleColorTokens,
    val icon: @Composable (Modifier, Color) -> Unit,
)

internal val HandBrewModuleSpec = RecordModuleUiSpec(
    module = RecordModule.HandBrew,
    label = AppCopy.RecordModule.handBrewLabel,
    questionToday = AppCopy.RecordModule.handBrewQuestionToday,
    questionPast = AppCopy.RecordModule.handBrewQuestionPast,
    explicitZeroText = AppCopy.RecordModule.handBrewZero,
    semanticCountLabel = AppCopy.RecordModule.handBrewLabel,
    colors = HandBrewColorTokens,
    icon = { modifier, color ->
        HandBrewIcon(
            modifier = modifier,
            tint = color.takeIf { it == DailyRecordOnAccent },
        )
    },
)

internal val SexModuleSpec = RecordModuleUiSpec(
    module = RecordModule.Sex,
    label = AppCopy.RecordModule.sexLabel,
    questionToday = AppCopy.RecordModule.sexQuestionToday,
    questionPast = AppCopy.RecordModule.sexQuestionPast,
    explicitZeroText = AppCopy.RecordModule.sexZero,
    semanticCountLabel = AppCopy.RecordModule.sexLabel,
    colors = SexColorTokens,
    icon = { modifier, color ->
        SexIcon(
            modifier = modifier,
            tint = color.takeIf { it == DailyRecordOnAccent },
        )
    },
)

internal fun RecordModule.uiSpec(): RecordModuleUiSpec = when (this) {
    RecordModule.HandBrew -> HandBrewModuleSpec
    RecordModule.Sex -> SexModuleSpec
}

internal data class RecordDetailEntry(
    val occurrenceIndex: Int,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val feeling: String = "",
) {
    init {
        require(occurrenceIndex >= 1) { "Detail occurrence index must be positive." }
    }
}

internal interface RecordModuleController {
    val module: RecordModule

    fun observeRecord(localDate: LocalDate): Flow<DailyCountEntry?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<DailyCountEntry>>

    suspend fun saveRecord(localDate: LocalDate, count: Int)

    fun observeDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>>

    suspend fun saveRecord(
        localDate: LocalDate,
        count: Int,
        details: List<RecordDetailEntry>,
    )

    suspend fun clearDetails(localDate: LocalDate, count: Int)

    suspend fun clearRecord(localDate: LocalDate): Boolean
}

internal abstract class RepositoryRecordModuleController<T : DailyCountRecord>(
    final override val module: RecordModule,
    private val repository: DailyCountRecordRepository<T>,
) : RecordModuleController {
    final override fun observeRecord(localDate: LocalDate): Flow<DailyCountEntry?> =
        repository.observeRecord(localDate).map { it?.asDailyCountEntry() }

    final override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<DailyCountEntry>> =
        repository.observeRecords(startDate, endExclusive).map { records ->
            records.map(DailyCountRecord::asDailyCountEntry)
        }

    final override suspend fun saveRecord(localDate: LocalDate, count: Int) {
        saveRecordInternal(localDate, count, details = null)
    }

    final override fun observeDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>> =
        observeModuleDetails(localDate)

    final override suspend fun saveRecord(
        localDate: LocalDate,
        count: Int,
        details: List<RecordDetailEntry>,
    ) {
        saveRecordInternal(localDate, count, details)
    }

    final override suspend fun clearDetails(localDate: LocalDate, count: Int) {
        saveRecordInternal(localDate, count, details = emptyList())
    }

    private suspend fun saveRecordInternal(
        localDate: LocalDate,
        count: Int,
        details: List<RecordDetailEntry>?,
    ) {
        val existing = repository.getRecord(localDate)
        val now = Instant.now()
        val updatedAt = RecordFactory.resolveUpdatedAt(existing?.updatedAt, now)
        val record = createRecord(
            existing = existing,
            localDate = localDate,
            count = count,
            createdAt = existing?.createdAt ?: now,
            updatedAt = updatedAt,
        )
        if (details == null) {
            repository.saveRecord(record)
        } else {
            saveRecordWithDetails(record, localDate, details)
        }
    }

    protected abstract fun observeModuleDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>>

    protected abstract suspend fun saveRecordWithDetails(
        record: T,
        localDate: LocalDate,
        details: List<RecordDetailEntry>,
    )

    protected abstract fun createRecord(
        existing: T?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): T

    final override suspend fun clearRecord(localDate: LocalDate): Boolean =
        repository.clearRecord(localDate)

    /**
     * Merges a detail draft with the stored detail models of the same slots.
     * A detail's id is its slot identity (see the repository's index-based
     * matching), so edits must not churn ids or rewrite creation timestamps:
     * existing slots keep their id/createdAt, new slots mint fresh values.
     */
    protected suspend fun <TD> mergeDetailsWithStored(
        localDate: LocalDate,
        drafts: List<RecordDetailEntry>,
        stored: suspend () -> List<TD>,
        occurrenceIndexOf: (TD) -> Int,
        createdAtOf: (TD) -> Instant,
        create: (
            existing: TD?,
            draft: RecordDetailEntry,
            createdAt: Instant,
            updatedAt: Instant,
        ) -> TD,
    ): List<TD> {
        val now = Instant.now()
        val existingByIndex = stored().associateBy(occurrenceIndexOf)
        return drafts.map { draft ->
            val existing = existingByIndex[draft.occurrenceIndex]
            create(
                existing,
                draft,
                existing?.let(createdAtOf) ?: now,
                now,
            )
        }
    }
}

internal class HandBrewModuleController(
    private val handBrewRepository: HandBrewRecordRepository,
) : RepositoryRecordModuleController<HandBrewRecord>(
    module = RecordModule.HandBrew,
    repository = handBrewRepository,
) {
    override fun observeModuleDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>> =
        handBrewRepository.observeDetails(localDate).map { details ->
            details.map {
                RecordDetailEntry(it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
            }
        }

    override suspend fun saveRecordWithDetails(
        record: HandBrewRecord,
        localDate: LocalDate,
        details: List<RecordDetailEntry>,
    ) {
        handBrewRepository.saveRecord(
            record,
            mergeDetailsWithStored(
                localDate = localDate,
                drafts = details,
                stored = { handBrewRepository.observeDetails(localDate).first() },
                occurrenceIndexOf = HandBrewRecordDetail::occurrenceIndex,
                createdAtOf = HandBrewRecordDetail::createdAt,
                create = { existing, draft, createdAt, updatedAt ->
                    RecordFactory.createHandBrewDetail(
                        existing = existing,
                        localDate = localDate,
                        occurrenceIndex = draft.occurrenceIndex,
                        startTime = draft.startTime,
                        endTime = draft.endTime,
                        feeling = draft.feeling,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    )
                },
            ),
        )
    }

    override fun createRecord(
        existing: HandBrewRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecord = RecordFactory.createHandBrewRecord(
        existing = existing,
        localDate = localDate,
        count = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal class SexModuleController(
    private val sexRepository: SexRecordRepository,
) : RepositoryRecordModuleController<SexRecord>(
    module = RecordModule.Sex,
    repository = sexRepository,
) {
    override fun observeModuleDetails(localDate: LocalDate): Flow<List<RecordDetailEntry>> =
        sexRepository.observeDetails(localDate).map { details ->
            details.map {
                RecordDetailEntry(it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
            }
        }

    override suspend fun saveRecordWithDetails(
        record: SexRecord,
        localDate: LocalDate,
        details: List<RecordDetailEntry>,
    ) {
        sexRepository.saveRecord(
            record,
            mergeDetailsWithStored(
                localDate = localDate,
                drafts = details,
                stored = { sexRepository.observeDetails(localDate).first() },
                occurrenceIndexOf = SexRecordDetail::occurrenceIndex,
                createdAtOf = SexRecordDetail::createdAt,
                create = { existing, draft, createdAt, updatedAt ->
                    RecordFactory.createSexDetail(
                        existing = existing,
                        localDate = localDate,
                        occurrenceIndex = draft.occurrenceIndex,
                        startTime = draft.startTime,
                        endTime = draft.endTime,
                        feeling = draft.feeling,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                    )
                },
            ),
        )
    }

    override fun createRecord(
        existing: SexRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecord = RecordFactory.createSexRecord(
        existing = existing,
        localDate = localDate,
        count = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun DailyCountRecord.asDailyCountEntry() = DailyCountEntry(
    localDate = localDate,
    count = count,
)
