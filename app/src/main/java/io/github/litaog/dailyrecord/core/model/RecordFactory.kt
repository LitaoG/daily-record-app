package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Single source of truth for constructing records and their per-occurrence
 * details: id identity, slot identity and monotonic edit timestamps.
 *
 * The UI controller only maps its projection to these factories; it never
 * generates ids or rewrites creation timestamps itself.
 */
object RecordFactory {

    fun createHandBrewRecord(
        existing: HandBrewRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecord = HandBrewRecord(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        brewCount = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun createSexRecord(
        existing: SexRecord?,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecord = SexRecord(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        sexCount = count,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun createHandBrewDetail(
        existing: HandBrewRecordDetail?,
        localDate: LocalDate,
        occurrenceIndex: Int,
        startTime: LocalTime?,
        endTime: LocalTime?,
        feeling: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecordDetail = HandBrewRecordDetail(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        occurrenceIndex = occurrenceIndex,
        startTime = startTime,
        endTime = endTime,
        feeling = feeling,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun createSexDetail(
        existing: SexRecordDetail?,
        localDate: LocalDate,
        occurrenceIndex: Int,
        startTime: LocalTime?,
        endTime: LocalTime?,
        feeling: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecordDetail = SexRecordDetail(
        id = existing?.id ?: UUID.randomUUID().toString(),
        localDate = localDate,
        occurrenceIndex = occurrenceIndex,
        startTime = startTime,
        endTime = endTime,
        feeling = feeling,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /**
     * Resolves an edit timestamp that stays strictly after the previous one.
     * Falls back to [now] so a clock rollback never rewinds stored revisions,
     * and consecutive writes sharing one clock tick stay strictly monotonic.
     */
    fun resolveUpdatedAt(existing: Instant?, now: Instant): Instant =
        existing?.nextRecordTimestamp()?.takeIf { it.isAfter(now) } ?: now
}

/** Keeps edits strictly monotonic when multiple writes share the same clock tick. */
internal fun Instant.nextRecordTimestamp(): Instant =
    if (this == Instant.MAX) this else plusMillis(1)
