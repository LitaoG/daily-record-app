package io.github.litaog.dailyrecord.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.CalendarGlyph
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.isNetworkReachabilityFailure
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import kotlinx.coroutines.launch

internal enum class AuthMode {
    SignIn,
    Register,
}

@Composable
internal fun AuthScreen(
    productionConfigured: Boolean,
    onSignIn: suspend (String, String) -> Result<Unit>,
    onRegister: suspend (String, String) -> Result<Unit>,
    onPasswordReset: suspend (String) -> Result<Unit> = { Result.success(Unit) },
    onBack: (() -> Unit)? = null,
    onContinueOffline: () -> Unit = {},
) {
    var modeName by rememberSaveable { mutableStateOf(AuthMode.SignIn.name) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var showPasswordReset by rememberSaveable { mutableStateOf(false) }
    val mode = AuthMode.entries.firstOrNull { it.name == modeName } ?: AuthMode.SignIn
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val validationError = remember(mode, email, password, confirmPassword) {
        validateCredentials(mode, email, password, confirmPassword)
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = DailyRecordText,
        unfocusedTextColor = DailyRecordText,
        disabledTextColor = DailyRecordTextMuted,
        focusedBorderColor = DailyRecordDefaultAccent,
        unfocusedBorderColor = DailyRecordDivider,
        disabledBorderColor = DailyRecordDivider,
        focusedLabelColor = DailyRecordDefaultAccent,
        unfocusedLabelColor = DailyRecordTextMuted,
        disabledLabelColor = DailyRecordTextMuted,
        cursorColor = DailyRecordDefaultAccent,
        focusedContainerColor = DailyRecordSurface,
        unfocusedContainerColor = DailyRecordSurface,
        disabledContainerColor = DailyRecordSurface,
    )
    val submit: () -> Unit = submit@{
        if (!productionConfigured || busy || validationError != null) return@submit
        busy = true
        errorText = null
        scope.launch {
            val result = if (mode == AuthMode.SignIn) {
                onSignIn(email.trim(), password)
            } else {
                onRegister(email.trim(), password)
            }
            result.exceptionOrNull()?.let { errorText = authErrorMessage(it, mode) }
            busy = false
        }
    }

    BackHandler(enabled = onBack != null && !busy) {
        onBack?.invoke()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dailyRecordBackdropBrush(HandBrewColorTokens))
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CalendarGlyph(color = DailyRecordDefaultAccent, modifier = Modifier.size(36.dp))
            Text(AppCopy.Auth.title, color = DailyRecordText, style = MaterialTheme.typography.headlineLarge)
            Text(
                if (mode == AuthMode.SignIn) AppCopy.Auth.signInSubtitle else AppCopy.Auth.registerSubtitle,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthModeButton(AppCopy.Auth.signIn, mode == AuthMode.SignIn) {
                    if (!busy) {
                        modeName = AuthMode.SignIn.name
                        errorText = null
                    }
                }
                AuthModeButton(AppCopy.Auth.register, mode == AuthMode.Register) {
                    if (!busy) {
                        modeName = AuthMode.Register.name
                        errorText = null
                    }
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorText = null },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(AppCopy.Auth.email) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorText = null },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(AppCopy.Auth.password) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (mode == AuthMode.Register) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (mode == AuthMode.SignIn) submit() },
                ),
                trailingIcon = {
                    DailyRecordTextAction(
                        label = if (passwordVisible) AppCopy.Auth.hide else AppCopy.Auth.show,
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !busy,
                        accessibilityLabel = if (passwordVisible) AppCopy.Auth.hidePassword else AppCopy.Auth.showPassword,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
            )
            if (mode == AuthMode.SignIn) {
                DailyRecordTextAction(
                    label = AppCopy.Auth.forgotPassword,
                    onClick = { showPasswordReset = true },
                    enabled = productionConfigured && !busy,
                    modifier = Modifier.align(Alignment.End),
                    accessibilityLabel = AppCopy.Auth.openPasswordReset,
                )
            }
            if (mode == AuthMode.Register) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorText = null },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppCopy.Auth.confirmPassword) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                )
            }
            if (mode == AuthMode.Register) {
                Text(
                    AppCopy.Auth.passwordPolicy,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vpn_auth_notice"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    if (mode == AuthMode.SignIn) AppCopy.Auth.signInVpnNotice else AppCopy.Auth.registerVpnNotice,
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    if (mode == AuthMode.SignIn) {
                        AppCopy.Auth.signInLocalSyncNotice
                    } else {
                        AppCopy.Auth.registerLocalSyncNotice
                    },
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!productionConfigured) {
                Text(
                    AppCopy.Auth.emulatorNotice,
                    color = DailyRecordDefaultAccent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            (errorText ?: validationError?.takeIf { email.isNotEmpty() || password.isNotEmpty() })?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryActionButton(
                label = when {
                    busy -> AppCopy.Auth.wait
                    mode == AuthMode.SignIn -> AppCopy.Auth.signInAndRestore
                    else -> AppCopy.Auth.createAccount
                },
                enabled = productionConfigured && !busy && validationError == null,
                modifier = Modifier.fillMaxWidth(),
                onClick = submit,
            )
            DailyRecordTextAction(
                label = if (mode == AuthMode.SignIn) {
                    AppCopy.Auth.continueOffline
                } else {
                    AppCopy.Auth.continueOfflineRegister
                },
                onClick = onContinueOffline,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showPasswordReset) {
        PasswordResetDialog(
            initialEmail = email,
            onDismiss = { showPasswordReset = false },
            onReset = onPasswordReset,
            onEmailAccepted = { acceptedEmail ->
                email = acceptedEmail
                password = ""
                confirmPassword = ""
                errorText = null
            },
        )
    }
}

@Composable
private fun AuthModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DailyRecordSurfaceMuted else DailyRecordSurface)
            .border(
                width = 1.dp,
                color = if (selected) DailyRecordDefaultAccent else DailyRecordDivider,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 24.dp)
            .semantics {
                role = Role.Tab
                contentDescription = AppCopy.selectedState(label, selected)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) DailyRecordDefaultAccent else DailyRecordTextMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun validateCredentials(
    mode: AuthMode,
    email: String,
    password: String,
    confirmPassword: String,
): String? = when {
    email.isBlank() -> AppCopy.Auth.emailRequired
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> AppCopy.Auth.emailInvalid
    password.length < 8 -> AppCopy.Auth.passwordTooShort
    mode == AuthMode.Register && password != confirmPassword -> AppCopy.Auth.passwordMismatch
    else -> null
}

internal fun authErrorMessage(error: Throwable, mode: AuthMode): String {
    val code = generateSequence(error) { it.cause }
        .filterIsInstance<com.google.firebase.auth.FirebaseAuthException>()
        .firstOrNull()
        ?.errorCode
        .orEmpty()
    if (error.isNetworkReachabilityFailure() || code == "ERROR_NETWORK_REQUEST_FAILED") {
        return AppCopy.Auth.network
    }
    return when (code) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> AppCopy.Auth.emailAlreadyRegistered
        "ERROR_WEAK_PASSWORD" -> AppCopy.Auth.weakPassword
        "ERROR_TOO_MANY_REQUESTS" -> AppCopy.Auth.tooManyRequests
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_LOGIN_CREDENTIALS",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND",
        -> AppCopy.Auth.invalidCredentials
        else -> when (mode) {
            AuthMode.SignIn -> AppCopy.Auth.signInUnavailable
            AuthMode.Register -> AppCopy.Auth.registerUnavailable
        }
    }
}
