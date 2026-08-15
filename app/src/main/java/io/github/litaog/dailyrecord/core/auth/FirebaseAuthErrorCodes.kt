package io.github.litaog.dailyrecord.core.auth

/**
 * Single source of truth for Firebase Auth error codes used across the auth,
 * account deletion, password reset and sync boundaries.
 *
 * Keep the constants verbatim: Firebase returns these strings from its SDK and
 * they must never be localized or reworded. UI files still own their
 * user-facing mappings; this object only removes duplicated literals.
 */
internal object FirebaseAuthErrorCodes {
    const val USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    const val NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
    const val TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
    const val INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL"
    const val INVALID_LOGIN_CREDENTIALS = "ERROR_INVALID_LOGIN_CREDENTIALS"
    const val WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
    const val EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
    const val WEAK_PASSWORD = "ERROR_WEAK_PASSWORD"
    const val QUOTA_EXCEEDED = "ERROR_QUOTA_EXCEEDED"
    const val OPERATION_NOT_ALLOWED = "ERROR_OPERATION_NOT_ALLOWED"
    const val REQUIRES_RECENT_LOGIN = "ERROR_REQUIRES_RECENT_LOGIN"
    const val USER_MISMATCH = "ERROR_USER_MISMATCH"
    const val USER_TOKEN_EXPIRED = "ERROR_USER_TOKEN_EXPIRED"
    const val INVALID_USER_TOKEN = "ERROR_INVALID_USER_TOKEN"
    const val ID_TOKEN_REVOKED = "ERROR_ID_TOKEN_REVOKED"
    const val INTERNAL_ERROR = "ERROR_INTERNAL_ERROR"
}
