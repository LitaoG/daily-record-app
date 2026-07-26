package io.github.litaog.dailyrecord.ui.diagnostics

import android.os.Build
import io.github.litaog.dailyrecord.BuildConfig
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.SyncDiagnostics
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus

internal data class DiagnosticReportInput(
    val appVersion: String,
    val versionCode: Int,
    val buildType: String,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val apiLevel: Int,
    val syncStatus: String,
    val pendingRecords: String,
    val databaseState: String,
    val databaseSchema: Int,
    val latestSyncError: String,
)

internal fun createDiagnosticReport(
    status: SyncStatus,
    diagnostics: SyncDiagnostics,
): String = renderDiagnosticReport(
    DiagnosticReportInput(
        appVersion = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        syncStatus = status.diagnosticCode(),
        pendingRecords = diagnostics.hasPendingRecords.yesNoUnknown(),
        databaseState = "ready",
        databaseSchema = DailyRecordDatabase.SCHEMA_VERSION,
        latestSyncError = diagnostics.latestFailureKind?.diagnosticCode() ?: "none",
    ),
)

internal fun renderDiagnosticReport(input: DiagnosticReportInput): String = buildString {
    appendLine("Hand Brew Calendar diagnostics")
    appendLine("app_version=${input.appVersion.safeValue()}")
    appendLine("version_code=${input.versionCode}")
    appendLine("build_type=${input.buildType.safeValue()}")
    appendLine("device=${input.manufacturer.safeValue()} ${input.model.safeValue()}")
    appendLine("android=${input.androidRelease.safeValue()} api=${input.apiLevel}")
    appendLine("sync_status=${input.syncStatus.safeValue()}")
    appendLine("pending_records=${input.pendingRecords.safeValue()}")
    appendLine("database_state=${input.databaseState.safeValue()}")
    appendLine("database_schema=${input.databaseSchema}")
    append("latest_sync_error=${input.latestSyncError.safeValue()}")
}

private fun SyncStatus.diagnosticCode(): String = when (this) {
    SyncStatus.NotConfigured -> "not_configured"
    SyncStatus.Offline -> "offline"
    SyncStatus.Syncing -> "syncing"
    SyncStatus.UpToDate -> "up_to_date"
    is SyncStatus.Pending -> "pending"
    is SyncStatus.Failed -> "failed_${kind.diagnosticCode()}"
}

private fun SyncFailureKind.diagnosticCode(): String = name.lowercase()

private fun Boolean?.yesNoUnknown(): String = when (this) {
    true -> "yes"
    false -> "no"
    null -> "unknown"
}

private fun String.safeValue(): String = replace('\n', ' ').replace('\r', ' ').trim()
