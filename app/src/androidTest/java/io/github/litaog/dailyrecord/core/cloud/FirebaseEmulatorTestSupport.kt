package io.github.litaog.dailyrecord.core.cloud

import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject

internal fun assertAuthEmulatorReachable() {
        val connection = URL(
            "http://10.0.2.2:9099/emulator/v1/projects/demo-daily-record-app/config",
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }

internal fun passwordResetCodeFor(email: String): String {
        val connection = URL(
            "http://10.0.2.2:9099/emulator/v1/projects/demo-daily-record-app/oobCodes",
        ).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            assertEquals(200, connection.responseCode)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val codes = JSONObject(body).getJSONArray("oobCodes")
            (0 until codes.length())
                .asSequence()
                .map(codes::getJSONObject)
                .first { it.optString("email") == email && it.optString("requestType") == "PASSWORD_RESET" }
                .getString("oobCode")
        } finally {
            connection.disconnect()
        }
    }

internal fun confirmPasswordReset(oobCode: String, newPassword: String) {
        val connection = URL(
            "http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:resetPassword" +
                "?key=daily-record-emulator-key",
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(JSONObject().put("oobCode", oobCode).put("newPassword", newPassword).toString())
            }
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }
