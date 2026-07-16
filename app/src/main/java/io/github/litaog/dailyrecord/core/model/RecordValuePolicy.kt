package io.github.litaog.dailyrecord.core.model

data class NormalizedRecordValue(
    val status: RecordStatus,
    val quantity: Long?,
)

object RecordValuePolicy {
    fun normalize(
        measurementType: MeasurementType,
        requestedStatus: RecordStatus,
        quantity: Long?,
    ): NormalizedRecordValue {
        require(quantity == null || quantity >= 0) { "Quantity must be non-negative." }

        return when (measurementType) {
            MeasurementType.BOOLEAN -> {
                require(quantity == null) { "BOOLEAN records do not use quantity." }
                NormalizedRecordValue(requestedStatus, null)
            }

            MeasurementType.COUNT,
            MeasurementType.DURATION,
            -> when (requestedStatus) {
                RecordStatus.UNSET,
                RecordStatus.SKIPPED,
                -> {
                    require(quantity == null) { "$requestedStatus records do not use quantity." }
                    NormalizedRecordValue(requestedStatus, null)
                }

                RecordStatus.DONE,
                RecordStatus.MISSED,
                -> {
                    val resolvedQuantity = requireNotNull(quantity) {
                        "$measurementType records require quantity when explicitly recorded."
                    }
                    NormalizedRecordValue(
                        status = if (resolvedQuantity == 0L) RecordStatus.MISSED else RecordStatus.DONE,
                        quantity = resolvedQuantity,
                    )
                }
            }
        }
    }
}
