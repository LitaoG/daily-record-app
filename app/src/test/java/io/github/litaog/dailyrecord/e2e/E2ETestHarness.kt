package io.github.litaog.dailyrecord.e2e

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.litaog.dailyrecord.core.data.DailyCountRecordRepository
import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Shared test infrastructure, in-memory repository fakes, and utility fixtures
 * for the opaque-box E2E test suite.
 */
class E2EFakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String) {
        data[key] = value
    }

    fun putBoolean(key: String, value: Boolean) {
        data[key] = value
    }

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        data[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private class FakeEditor(private val backingData: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val staged = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) staged[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                backingData.clear()
            }
            removed.forEach { backingData.remove(it) }
            backingData.putAll(staged)
        }
    }
}

/** In-memory implementation of HandBrewRecordRepository for opaque-box testing. */
class InMemoryHandBrewRecordRepository : HandBrewRecordRepository {
    private val records = MutableStateFlow<Map<LocalDate, HandBrewRecord>>(emptyMap())
    private val details = MutableStateFlow<Map<LocalDate, List<HandBrewRecordDetail>>>(emptyMap())

    override fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?> =
        records.map { it[localDate] }

    override suspend fun getRecord(localDate: LocalDate): HandBrewRecord? =
        records.value[localDate]

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>> = records.map { map ->
        map.filterKeys { date -> date >= startDate && date < endExclusive }.values.sortedBy { it.localDate }
    }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord {
        records.update { it + (record.localDate to record) }
        return record
    }

    override suspend fun saveRecord(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ): HandBrewRecord {
        records.update { it + (record.localDate to record) }
        this.details.update { it + (record.localDate to details) }
        return record
    }

    override fun observeDetails(localDate: LocalDate): Flow<List<HandBrewRecordDetail>> =
        details.map { it[localDate] ?: emptyList() }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        var existed = false
        records.update {
            existed = it.containsKey(localDate)
            it - localDate
        }
        details.update { it - localDate }
        return existed
    }
}

/** In-memory implementation of SexRecordRepository for opaque-box testing. */
class InMemorySexRecordRepository : SexRecordRepository {
    private val records = MutableStateFlow<Map<LocalDate, SexRecord>>(emptyMap())
    private val details = MutableStateFlow<Map<LocalDate, List<SexRecordDetail>>>(emptyMap())

    override fun observeRecord(localDate: LocalDate): Flow<SexRecord?> =
        records.map { it[localDate] }

    override suspend fun getRecord(localDate: LocalDate): SexRecord? =
        records.value[localDate]

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecord>> = records.map { map ->
        map.filterKeys { date -> date >= startDate && date < endExclusive }.values.sortedBy { it.localDate }
    }

    override suspend fun saveRecord(record: SexRecord): SexRecord {
        records.update { it + (record.localDate to record) }
        return record
    }

    override suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord {
        records.update { it + (record.localDate to record) }
        this.details.update { it + (record.localDate to details) }
        return record
    }

    override fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> =
        details.map { it[localDate] ?: emptyList() }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        var existed = false
        records.update {
            existed = it.containsKey(localDate)
            it - localDate
        }
        details.update { it - localDate }
        return existed
    }
}

/** Helper functions for record generation and WCAG contrast validation. */
object E2ETestFixtures {
    private val fixedTimestamp = Instant.parse("2026-11-07T12:00:00Z")

    fun handBrewRecord(date: LocalDate, count: Int): HandBrewRecord =
        HandBrewRecord(
            id = "hb_${date}_$count",
            localDate = date,
            brewCount = count,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp,
        )

    fun sexRecord(date: LocalDate, count: Int): SexRecord =
        SexRecord(
            id = "sex_${date}_$count",
            localDate = date,
            sexCount = count,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp,
        )

    fun handBrewDetail(
        date: LocalDate,
        index: Int,
        start: LocalTime? = null,
        end: LocalTime? = null,
        feeling: String = "",
    ): HandBrewRecordDetail =
        HandBrewRecordDetail(
            id = "hb_detail_${date}_$index",
            localDate = date,
            occurrenceIndex = index,
            startTime = start,
            endTime = end,
            feeling = feeling,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp,
        )

    fun sexDetail(
        date: LocalDate,
        index: Int,
        start: LocalTime? = null,
        end: LocalTime? = null,
        feeling: String = "",
    ): SexRecordDetail =
        SexRecordDetail(
            id = "sex_detail_${date}_$index",
            localDate = date,
            occurrenceIndex = index,
            startTime = start,
            endTime = end,
            feeling = feeling,
            createdAt = fixedTimestamp,
            updatedAt = fixedTimestamp,
        )

    /** Relative luminance per WCAG 2.1 specs. */
    fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        fun srgb(c: Int): Double {
            val v = (c and 0xFF) / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val r = srgb(argb shr 16)
        val g = srgb(argb shr 8)
        val b = srgb(argb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Contrast ratio per WCAG 2.1 specs (range 1.0 to 21.0). */
    fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
