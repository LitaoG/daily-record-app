package io.github.litaog.dailyrecord.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
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
import io.github.litaog.dailyrecord.ui.components.HandBrewTextAction
import io.github.litaog.dailyrecord.ui.components.PlaneIcon
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Paper50
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
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
            result.exceptionOrNull()?.let { errorText = authErrorMessage(it) }
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper50)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = Paper0,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Neutral300),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlaneIcon()
                Text("手冲日历", color = Ink900, style = MaterialTheme.typography.headlineLarge)
                Text(
                    "登录后，本机记录会安全合并到你的账号，换手机可自动恢复。",
                    color = Ink700,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuthModeButton("登录", mode == AuthMode.SignIn) {
                        if (!busy) {
                            modeName = AuthMode.SignIn.name
                            errorText = null
                        }
                    }
                    AuthModeButton("注册", mode == AuthMode.Register) {
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
                    label = { Text("邮箱") },
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
                    label = { Text("密码") },
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
                        HandBrewTextAction(
                            label = if (passwordVisible) "隐藏" else "显示",
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !busy,
                            accessibilityLabel = if (passwordVisible) "隐藏密码" else "显示密码",
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                )
                if (mode == AuthMode.SignIn) {
                    HandBrewTextAction(
                        label = "忘记密码？",
                        onClick = { showPasswordReset = true },
                        enabled = productionConfigured && !busy,
                        modifier = Modifier.align(Alignment.End),
                        accessibilityLabel = "打开重置密码",
                    )
                }
                if (mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = null },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("再次输入密码") },
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
                Text(
                    "密码至少 8 位；不使用短信或验证码。请妥善保存密码。",
                    color = Ink500,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!productionConfigured) {
                    Text(
                        "云端开发项目尚未完成配置，当前构建只能连接本地测试环境。",
                        color = Terracotta500,
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
                        busy -> "请稍候…"
                        mode == AuthMode.SignIn -> "登录并恢复记录"
                        else -> "创建账号"
                    },
                    enabled = productionConfigured && !busy && validationError == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = submit,
                )
                HandBrewTextAction(
                    label = "先在本机使用",
                    onClick = onContinueOffline,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "以后登录时，本机记录会合并到你下一次登录的账号。",
                    color = Ink500,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "云端只保存你的手冲日期、次数和同步所需数据。",
            color = Ink500,
            style = MaterialTheme.typography.labelSmall,
        )
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
            .background(if (selected) Paper100 else Paper0)
            .border(
                width = 1.dp,
                color = if (selected) Terracotta500 else Neutral300,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 24.dp)
            .semantics {
                role = Role.Tab
                contentDescription = "$label，${if (selected) "已选择" else "未选择"}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Terracotta500 else Ink500,
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
    email.isBlank() -> "请输入邮箱"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "请输入有效邮箱"
    password.length < 8 -> "密码至少需要 8 位"
    mode == AuthMode.Register && password != confirmPassword -> "两次输入的密码不一致"
    else -> null
}

private fun authErrorMessage(error: Throwable): String {
    val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
    return when (code) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "此邮箱已注册，请直接登录"
        "ERROR_WEAK_PASSWORD" -> "密码强度不足，请使用更长的密码"
        "ERROR_NETWORK_REQUEST_FAILED" -> "网络不可用，请检查连接后重试"
        "ERROR_TOO_MANY_REQUESTS" -> "尝试次数过多，请稍后再试"
        else -> "登录失败，请检查邮箱和密码后重试"
    }
}
