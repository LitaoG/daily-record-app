package io.github.litaog.dailyrecord.core.model

import java.time.Instant

data class Activity(
    val id: String,
    val ownerId: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val measurementType: MeasurementType,
    val unit: String?,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val revision: Long,
) {
    init {
        require(id.isNotBlank()) { "Activity id must not be blank." }
        require(ownerId.isNotBlank()) { "Activity ownerId must not be blank." }
        require(name.isNotBlank()) { "Activity name must not be blank." }
        require(iconKey.isNotBlank()) { "Activity iconKey must not be blank." }
        require(sortOrder >= 0) { "Activity sortOrder must be non-negative." }
        require(revision >= 0) { "Activity revision must be non-negative." }
    }
}
