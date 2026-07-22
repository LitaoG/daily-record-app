package io.github.litaog.dailyrecord.ui.auth

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import io.github.litaog.dailyrecord.ui.components.HandBrewDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import kotlinx.coroutines.launch

private const val RESET_SUCCESS_MESSAGE =
    "如果该邮箱已注册，重置邮件将在几分钟内送达。请检查收件箱和垃圾邮件。"

@Composable
internal fun PasswordResetDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onReset: suspend (String) -> Result<Unit>,
    onEmailAccepted: (String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    val normalizedEmail = email.trim().lowercase()
    val validationError = when {
        email.isBlank() -> "请输入邮箱"
        !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> "请输入有效邮箱"
        else -> null
    }
    val scope = rememberCoroutineScope()
    val submit: () -> Unit = submit@{
        if (busy || sent || validationError != null) return@submit
        busy = true
        errorText = null
        scope.launch {
            val result = onReset(normalizedEmail)
            if (result.isSuccess || result.exceptionOrNull().isMissingAccountError()) {
                sent = true
                onEmailAccepted(normalizedEmail)
            } else {
                errorText = passwordResetErrorMessage(requireNotNull(result.exceptionOrNull()))
            }
            busy = false
        }
    }
    val stackActions = LocalDensity.current.fontScale >= 1.35f

    HandBrewDialog(
        title = if (sent) "请查收邮件" else "重置密码",
        subtitle = if (sent) "为了保护账号隐私，我们不会显示该邮箱是否已注册。" else "输入注册邮箱，我们会发送安全的重置链接。",
        testTag = "password_reset_dialog",
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (sent) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("password_reset_success"),
                    shape = RoundedCornerShape(16.dp),
                    color = Paper100,
                    border = BorderStroke(1.dp, Neutral300),
                ) {
                    Text(
                        text = RESET_SUCCESS_MESSAGE,
                        color = Ink700,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                PrimaryActionButton(
                    label = "返回登录",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .testTag("password_reset_email")
                        .semantics {
                            if (errorText != null) liveRegion = LiveRegionMode.Polite
                        },
                    label = { Text("邮箱") },
                    supportingText = {
                        val message = errorText ?: validationError?.takeIf { email.isNotEmpty() }
                        if (message != null) Text(message)
                    },
                    isError = errorText != null || (email.isNotEmpty() && validationError != null),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Ink900,
                        unfocusedTextColor = Ink900,
                        disabledTextColor = Ink500,
                        focusedBorderColor = Terracotta500,
                        unfocusedBorderColor = Neutral300,
                        disabledBorderColor = Neutral300,
                        focusedLabelColor = Terracotta500,
                        unfocusedLabelColor = Ink500,
                        disabledLabelColor = Ink500,
                        cursorColor = Terracotta500,
                        focusedContainerColor = Paper0,
                        unfocusedContainerColor = Paper0,
                        disabledContainerColor = Paper0,
                    ),
                )
                if (stackActions) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PrimaryActionButton(
                            label = if (busy) "正在发送…" else "发送重置邮件",
                            onClick = submit,
                            enabled = !busy && validationError == null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlineActionButton(
                            label = "取消",
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlineActionButton(
                            label = "取消",
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryActionButton(
                            label = if (busy) "正在发送…" else "发送重置邮件",
                            onClick = submit,
                            enabled = !busy && validationError == null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun Throwable?.isMissingAccountError(): Boolean =
    (this as? FirebaseAuthException)?.errorCode == "ERROR_USER_NOT_FOUND"

internal fun passwordResetErrorMessage(error: Throwable): String {
    val causes = generateSequence(error) { it.cause }.toList()
    val code = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()?.errorCode.orEmpty()
    return passwordResetErrorMessageFor(code, causes.any { it is FirebaseNetworkException })
}

internal fun passwordResetErrorMessageFor(code: String, networkFailure: Boolean): String {
    return when (code) {
        "ERROR_NETWORK_REQUEST_FAILED" -> "网络不可用，邮件尚未发送。请检查连接后重试。"
        "ERROR_TOO_MANY_REQUESTS" -> "请求过于频繁，请稍后再试。"
        "ERROR_QUOTA_EXCEEDED" -> "今日发送额度已用完，请稍后再试。"
        else -> if (networkFailure) {
            "网络不可用，邮件尚未发送。请检查连接后重试。"
        } else {
            "暂时无法发送重置邮件，请稍后重试。"
        }
    }
}
