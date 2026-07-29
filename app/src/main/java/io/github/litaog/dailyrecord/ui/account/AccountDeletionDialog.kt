package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.syncFailureKind
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
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

private enum class AccountDeletionStep {
    Warning,
    Verify,
}

@Composable
internal fun AccountDeletionDialog(
    onDeleteAccount: suspend (String, LocalDataAfterAccountDeletion) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    var stepName by rememberSaveable { mutableStateOf(AccountDeletionStep.Warning.name) }
    var localDataName by rememberSaveable { mutableStateOf(LocalDataAfterAccountDeletion.Keep.name) }
    var password by remember { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    val step = AccountDeletionStep.entries.firstOrNull { it.name == stepName }
        ?: AccountDeletionStep.Warning
    val localData = LocalDataAfterAccountDeletion.entries.firstOrNull { it.name == localDataName }
        ?: LocalDataAfterAccountDeletion.Keep
    val scope = rememberCoroutineScope()

    val submit = {
        if (!busy && password.isNotBlank()) {
            busy = true
            errorText = null
            scope.launch {
                val result = onDeleteAccount(password, localData)
                result.exceptionOrNull()?.let { errorText = accountDeletionErrorMessage(it) }
                busy = false
            }
        }
    }

    DailyRecordDialog(
        title = if (step == AccountDeletionStep.Warning) "删除账号与云端数据？" else "再次确认永久删除",
        subtitle = if (step == AccountDeletionStep.Warning) {
            "此操作无法撤销"
        } else {
            "输入当前密码验证身份"
        },
        testTag = "account_deletion_dialog",
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        if (step == AccountDeletionStep.Warning) {
            Text(
                "继续后会先验证密码，再删除该账号的全部云端手冲、做爱记录和登录账号。",
                color = Ink700,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                "选择本机记录的处理方式",
                color = Ink900,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeletionChoiceCard(
                    title = "保留在本机（推荐）",
                    description = "删除账号后继续离线使用这些记录",
                    selected = localData == LocalDataAfterAccountDeletion.Keep,
                    onClick = { localDataName = LocalDataAfterAccountDeletion.Keep.name },
                )
                DeletionChoiceCard(
                    title = "同时删除本机记录",
                    description = "云端和这台手机都不再保留",
                    selected = localData == LocalDataAfterAccountDeletion.Delete,
                    onClick = { localDataName = LocalDataAfterAccountDeletion.Delete.name },
                )
            }
            PrimaryActionButton(
                label = "继续验证身份",
                onClick = { stepName = AccountDeletionStep.Verify.name },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton(
                label = "取消",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        } else {
            Text(
                if (localData == LocalDataAfterAccountDeletion.Keep) {
                    "云端记录和账号会永久删除；本机记录将转为离线记录。"
                } else {
                    "云端记录、账号和这台手机里的账号记录都会永久删除。"
                },
                color = Ink700,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorText = null
                },
                enabled = !busy,
                label = { Text("当前密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(16.dp),
                colors = deletionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("account_deletion_password"),
            )
            if (errorText != null) {
                Text(
                    errorText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("account_deletion_error"),
                )
            }
            DangerActionButton(
                label = if (busy) "正在永久删除…" else "永久删除账号",
                onClick = submit,
                enabled = !busy && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton(
                label = "返回",
                onClick = {
                    password = ""
                    errorText = null
                    stepName = AccountDeletionStep.Warning.name
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun DeletionChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (selected) Paper100 else Paper0, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (selected) Terracotta500 else Neutral300,
                RoundedCornerShape(14.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$title，${if (selected) "已选择" else "未选择"}"
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (selected) "●" else "○",
            color = if (selected) Terracotta500 else Ink500,
            style = MaterialTheme.typography.titleMedium,
        )
        Column {
            Text(title, color = Ink900, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = Ink500, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun deletionFieldColors() = OutlinedTextFieldDefaults.colors(
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

internal fun accountDeletionErrorMessage(error: Throwable): String {
    val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
    if (code.isNotEmpty()) return accountDeletionErrorMessageForCode(code)
    return when (error.syncFailureKind()) {
        SyncFailureKind.Network ->
            "网络中断，删除未完成；本机记录仍保留，请开启 VPN（梯子）后重试"
        SyncFailureKind.Authentication ->
            "登录状态已变化，请退出后重新登录再删除"
        SyncFailureKind.Permission ->
            "账号暂无删除权限；本机记录仍保留，请重新登录后重试"
        SyncFailureKind.Quota, SyncFailureKind.Service ->
            "云服务暂时不可用；本机记录仍保留，请稍后重试"
        SyncFailureKind.Data, SyncFailureKind.Unknown ->
            "删除未完成，本机记录仍保留；部分云端记录可能已先删除，请直接重试"
    }
}

internal fun accountDeletionErrorMessageForCode(code: String): String = when (code) {
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "密码不正确，请重新输入"
    "ERROR_NETWORK_REQUEST_FAILED" -> "网络不可用，请确认 VPN（梯子）已开启后重试"
    "ERROR_TOO_MANY_REQUESTS" -> "尝试次数过多，请稍后再试"
    "ERROR_USER_MISMATCH", "ERROR_USER_NOT_FOUND", "ERROR_REQUIRES_RECENT_LOGIN" ->
        "登录状态已变化，请退出后重新登录再删除"
    else -> "删除未完成，本机记录仍保留；部分云端记录可能已先删除，请直接重试"
}
