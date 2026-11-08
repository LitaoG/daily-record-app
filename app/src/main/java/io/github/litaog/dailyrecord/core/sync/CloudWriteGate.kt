package io.github.litaog.dailyrecord.core.sync

internal interface CloudWriteGate {
    suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T
}

internal object NoOpCloudWriteGate : CloudWriteGate {
    override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T = block()
}

internal fun DeletionBarrier.cloudWriteGate(): CloudWriteGate =
    object : CloudWriteGate {
        override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T =
            withCloudWrite(ownerId, block)
    }
