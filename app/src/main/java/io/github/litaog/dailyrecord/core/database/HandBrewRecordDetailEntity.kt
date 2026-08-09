package io.github.litaog.dailyrecord.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "hand_brew_record_details",
    indices = [
        Index(value = ["owner_id", "local_date", "occurrence_index"], unique = true),
        Index(value = ["owner_id", "local_date"]),
    ],
)
internal data class HandBrewRecordDetailEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,
    @ColumnInfo(name = "owner_id", defaultValue = LOCAL_OWNER_SQL_DEFAULT)
    val ownerId: String,
    @ColumnInfo(name = "occurrence_index")
    val occurrenceIndex: Int,
    @ColumnInfo(name = "start_time")
    val startTime: LocalTime?,
    @ColumnInfo(name = "end_time")
    val endTime: LocalTime?,
    @ColumnInfo(name = "feeling", defaultValue = "")
    val feeling: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
