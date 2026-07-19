package io.github.litaog.dailyrecord.core.database

import androidx.room.TypeConverter
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
}
