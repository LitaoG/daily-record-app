package io.github.litaog.dailyrecord.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "hand_brew_records",
    indices = [Index(value = ["local_date"], unique = true)],
)
internal data class HandBrewRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,
    @ColumnInfo(name = "brew_count", defaultValue = "0")
    val brewCount: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
