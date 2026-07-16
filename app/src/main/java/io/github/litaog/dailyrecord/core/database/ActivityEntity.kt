package io.github.litaog.dailyrecord.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.litaog.dailyrecord.core.model.MeasurementType
import java.time.Instant

@Entity(
    tableName = "activities",
    indices = [
        Index(value = ["owner_id", "id"], unique = true),
        Index(value = ["owner_id", "is_archived", "sort_order"]),
    ],
)
internal data class ActivityEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String,
    @ColumnInfo(name = "color_argb")
    val colorArgb: Int,
    @ColumnInfo(name = "measurement_type")
    val measurementType: MeasurementType,
    @ColumnInfo(name = "unit")
    val unit: String?,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int,
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Instant?,
    @ColumnInfo(name = "revision", defaultValue = "0")
    val revision: Long,
)
