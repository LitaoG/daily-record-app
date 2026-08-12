package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.DailyCountRecord
import io.github.litaog.dailyrecord.core.model.nextRecordTimestamp
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Storage contract shared by isolated daily-count modules.
 *
 * Each module still owns its entity, DAO and cloud collection; this interface only removes
 * duplicated orchestration from the UI layer.
 */
interface DailyCountRecordRepository<T : DailyCountRecord> {
    fun observeRecord(localDate: LocalDate): Flow<T?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<T>>

    suspend fun saveRecord(record: T): T

    suspend fun clearRecord(localDate: LocalDate): Boolean
}

/** All repository range reads use a non-empty half-open interval. */
internal fun requireValidRecordRange(startDate: LocalDate, endExclusive: LocalDate) {
    require(startDate < endExclusive) {
        "Date range must be non-empty and use [start, endExclusive)."
    }
}

/** Local persistence stays successful even when best-effort cloud scheduling fails. */
internal fun notifyLocalChangeSafely(onLocalChange: () -> Unit) {
    try {
        onLocalChange()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // Local persistence is the product guarantee; cloud scheduling is best effort.
    }
}
