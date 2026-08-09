package io.github.litaog.dailyrecord.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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
    fun localTimeToIso(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun isoToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}
