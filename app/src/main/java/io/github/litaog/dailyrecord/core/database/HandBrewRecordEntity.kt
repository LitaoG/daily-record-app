package io.github.litaog.dailyrecord.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "hand_brew_records",
    indices = [
        Index(value = ["owner_id", "local_date"], unique = true),
        Index(value = ["owner_id", "sync_state"]),
    ],
)
internal data class HandBrewRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,
    @ColumnInfo(name = "owner_id", defaultValue = LOCAL_OWNER_SQL_DEFAULT)
    val ownerId: String,
    @ColumnInfo(name = "brew_count", defaultValue = "0")
    val brewCount: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean,
    @ColumnInfo(name = "sync_state", defaultValue = "'PENDING'")
    val syncState: String,
    @ColumnInfo(name = "remote_revision", defaultValue = "0")
    val remoteRevision: Long,
)

internal const val LOCAL_OWNER_ID = "__local__"
internal const val LOCAL_OWNER_SQL_DEFAULT = "'__local__'"
internal const val SYNC_PENDING = "PENDING"
internal const val SYNCED = "SYNCED"
