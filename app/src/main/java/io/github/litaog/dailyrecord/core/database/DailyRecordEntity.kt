package io.github.litaog.dailyrecord.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.litaog.dailyrecord.core.model.RecordStatus
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "daily_records",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["owner_id", "id"],
            childColumns = ["owner_id", "activity_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["owner_id", "activity_id", "local_date"], unique = true),
        Index(value = ["owner_id", "local_date"]),
        Index(value = ["activity_id", "local_date"]),
    ],
)
internal data class DailyRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,
    @ColumnInfo(name = "status", defaultValue = "'UNSET'")
    val status: RecordStatus,
    @ColumnInfo(name = "quantity")
    val quantity: Long?,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Instant?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant?,
    @ColumnInfo(name = "revision", defaultValue = "0")
    val revision: Long,
)
