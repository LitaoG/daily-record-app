package io.github.litaog.dailyrecord.core.database

import androidx.room.TypeConverter
import io.github.litaog.dailyrecord.core.model.MeasurementType
import io.github.litaog.dailyrecord.core.model.RecordStatus
import java.time.Instant
import java.time.LocalDate

internal class DatabaseConverters {
    @TypeConverter
    fun instantToEpochMilliseconds(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisecondsToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToIso(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun measurementTypeToName(value: MeasurementType?): String? = value?.name

    @TypeConverter
    fun nameToMeasurementType(value: String?): MeasurementType? = value?.let(MeasurementType::valueOf)

    @TypeConverter
    fun recordStatusToName(value: RecordStatus?): String? = value?.name

    @TypeConverter
    fun nameToRecordStatus(value: String?): RecordStatus? = value?.let(RecordStatus::valueOf)
}
