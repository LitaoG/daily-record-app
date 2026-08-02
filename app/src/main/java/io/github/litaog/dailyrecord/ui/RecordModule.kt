package io.github.litaog.dailyrecord.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.DailyCountRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.model.DailyCountRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.ui.components.IntimacyIcon
import io.github.litaog.dailyrecord.ui.components.PlaneIcon
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
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
    label = "手冲",
    questionToday = "今天手冲了几次？",
    questionPast = "这天手冲了几次？",
    explicitZeroText = "明确没冲",
    semanticCountLabel = "手冲",
    colors = HandBrewColorTokens,
    icon = { modifier, color -> PlaneIcon(modifier = modifier, color = color) },
)

internal val SexModuleSpec = RecordModuleUiSpec(
    module = RecordModule.Sex,
    label = "做爱",
    questionToday = "今天做爱了几次？",
    questionPast = "这天做爱了几次？",
    explicitZeroText = "明确没有",
    semanticCountLabel = "做爱",
    colors = SexColorTokens,
    icon = { modifier, color -> IntimacyIcon(modifier = modifier, color = color) },
)

internal fun RecordModule.uiSpec(): RecordModuleUiSpec = when (this) {
    RecordModule.HandBrew -> HandBrewModuleSpec
    RecordModule.Sex -> SexModuleSpec
}

internal data class DailyCountEntry(
    val localDate: LocalDate,
    val count: Int,
) {
    init {
        require(count >= 0) { "Daily count must be non-negative." }
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
        val existing = repository.observeRecord(localDate).firstValue()
        val now = Instant.now()
        val safeUpdatedAt = existing?.updatedAt
            ?.plusMillis(1)
            ?.takeIf { it.isAfter(now) }
            ?: now
        repository.saveRecord(createRecord(
            existing = existing,
            localDate = localDate,
            count = count,
            createdAt = existing?.createdAt ?: now,
            updatedAt = safeUpdatedAt,
        ))
    }

    protected abstract fun createRecord(
        existing: T?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): T

    final override suspend fun clearRecord(localDate: LocalDate): Boolean =
        repository.clearRecord(localDate)
}

internal class HandBrewModuleController(
    repository: HandBrewRecordRepository,
) : RepositoryRecordModuleController<HandBrewRecord>(
    module = RecordModule.HandBrew,
    repository = repository,
) {
    override fun createRecord(
        existing: HandBrewRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ) = HandBrewRecord(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        brewCount = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal class SexModuleController(
    repository: SexRecordRepository,
) : RepositoryRecordModuleController<SexRecord>(
    module = RecordModule.Sex,
    repository = repository,
) {
    override fun createRecord(
        existing: SexRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ) = SexRecord(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        sexCount = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun DailyCountRecord.asDailyCountEntry() = DailyCountEntry(
    localDate = localDate,
    count = count,
)

private suspend fun <T> Flow<T>.firstValue(): T = first()
