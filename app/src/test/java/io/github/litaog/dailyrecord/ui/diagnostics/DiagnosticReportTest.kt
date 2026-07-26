package io.github.litaog.dailyrecord.ui.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {
    @Test
    fun reportContainsOperationalStateWithoutUserRecordData() {
        val report = renderDiagnosticReport(
            DiagnosticReportInput(
                appVersion = "1.0",
                versionCode = 1,
                buildType = "debug",
                manufacturer = "Google",
                model = "Pixel 4",
                androidRelease = "14",
                apiLevel = 34,
                syncStatus = "failed_network",
                pendingRecords = "yes",
                databaseState = "ready",
                databaseSchema = 3,
                latestSyncError = "network",
            ),
        )

        assertTrue(report.contains("app_version=1.0"))
        assertTrue(report.contains("pending_records=yes"))
        assertTrue(report.contains("latest_sync_error=network"))
        assertFalse(report.contains("1842043669@qq.com"))
        assertFalse(report.contains("2026-07-22"))
        assertFalse(report.contains("brew_count"))
        assertFalse(report.contains("record_count"))
    }

    @Test
    fun reportFlattensUnexpectedLineBreaks() {
        val report = renderDiagnosticReport(
            DiagnosticReportInput(
                appVersion = "1.0\nsecret",
                versionCode = 1,
                buildType = "debug",
                manufacturer = "Vendor\r\nName",
                model = "Model",
                androidRelease = "14",
                apiLevel = 34,
                syncStatus = "up_to_date",
                pendingRecords = "no",
                databaseState = "ready",
                databaseSchema = 3,
                latestSyncError = "none",
            ),
        )

        assertFalse(report.contains("1.0\nsecret"))
        assertFalse(report.contains("Vendor\r\nName"))
    }
}
