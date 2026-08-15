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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionAuthPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryConflictException
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.auth.FirebaseAuthErrorCodes
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.sync.syncFailureKind
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.components.dailyRecordFieldColors
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccentSoft
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
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
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
                val result: Result<Unit> = runCatchingPreservingCancellation {
                    onDeleteAccount(password, localData)
                }.getOrElse { error -> Result.failure(error) }
                result.exceptionOrNull()?.let { errorText = accountDeletionErrorMessage(it) }
                busy = false
            }
        }
    }

    DailyRecordDialog(
        title = if (step == AccountDeletionStep.Warning) AppCopy.Deletion.warningTitle else AppCopy.Deletion.confirmationTitle,
        subtitle = if (step == AccountDeletionStep.Warning) {
            AppCopy.Deletion.irreversible
        } else {
            AppCopy.Deletion.verifyPassword
        },
        testTag = "account_deletion_dialog",
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        if (step == AccountDeletionStep.Warning) {
            Text(
                AppCopy.Deletion.warningMessage,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                AppCopy.Deletion.localChoice,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeletionChoiceCard(
                    title = AppCopy.Deletion.keepLocalTitle,
                    description = AppCopy.Deletion.keepLocalDescription,
                    selected = localData == LocalDataAfterAccountDeletion.Keep,
                    onClick = { localDataName = LocalDataAfterAccountDeletion.Keep.name },
                )
                DeletionChoiceCard(
                    title = AppCopy.Deletion.deleteLocalTitle,
                    description = AppCopy.Deletion.deleteLocalDescription,
                    selected = localData == LocalDataAfterAccountDeletion.Delete,
                    onClick = { localDataName = LocalDataAfterAccountDeletion.Delete.name },
                )
            }
            PrimaryActionButton(
                label = AppCopy.Deletion.continueVerification,
                onClick = { stepName = AccountDeletionStep.Verify.name },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton(
                label = AppCopy.Auth.cancel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        } else {
            Text(
                if (localData == LocalDataAfterAccountDeletion.Keep) {
                    AppCopy.Deletion.confirmationKeepLocal
                } else {
                    AppCopy.Deletion.confirmationDeleteLocal
                },
                color = DailyRecordTextSecondary,
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
                label = { Text(AppCopy.Deletion.currentPassword) },
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
                label = if (busy) AppCopy.Deletion.deleting else AppCopy.Deletion.deletePermanently,
                onClick = submit,
                enabled = !busy && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton(
                label = AppCopy.Account.back,
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DailyRecordDefaultAccentSoft else DailyRecordSurface)
            .border(
                1.dp,
                if (selected) DailyRecordDefaultAccent else DailyRecordDivider,
                RoundedCornerShape(14.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = AppCopy.Deletion.selectionDescription(title, selected)
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (selected) "●" else "○",
            color = if (selected) DailyRecordDefaultAccent else DailyRecordTextMuted,
            style = MaterialTheme.typography.titleMedium,
        )
        Column {
            Text(title, color = DailyRecordText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun deletionFieldColors() = dailyRecordFieldColors()

internal fun accountDeletionErrorMessage(error: Throwable): String {
    if (error is AccountDeletionAuthPendingException) {
        return AppCopy.Deletion.authDeletionPending
    }
    if (error is AccountDeletionLocalRecoveryPendingException) {
        return AppCopy.Deletion.localRecoveryPending
    }
    if (error is AccountDeletionLocalRecoveryConflictException) {
        return AppCopy.Deletion.recoveryConflict
    }
    if (error is AccountDeletionLocalCleanupPendingException) {
        return AppCopy.Deletion.localCleanupPending
    }
    val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
    if (code.isNotEmpty()) return accountDeletionErrorMessageForCode(code)
    return when (error.syncFailureKind()) {
        SyncFailureKind.Network ->
            AppCopy.Deletion.networkError
        SyncFailureKind.Authentication ->
            AppCopy.Deletion.authError
        SyncFailureKind.Permission ->
            AppCopy.Deletion.permissionError
        SyncFailureKind.Quota, SyncFailureKind.Service ->
            AppCopy.Deletion.serviceError
        SyncFailureKind.Data, SyncFailureKind.Unknown ->
            AppCopy.Deletion.unknownError
    }
}

internal fun accountDeletionErrorMessageForCode(code: String): String = when (code) {
    FirebaseAuthErrorCodes.WRONG_PASSWORD, FirebaseAuthErrorCodes.INVALID_CREDENTIAL ->
        AppCopy.Deletion.wrongPassword
    FirebaseAuthErrorCodes.NETWORK_REQUEST_FAILED -> AppCopy.Deletion.networkAuthError
    FirebaseAuthErrorCodes.TOO_MANY_REQUESTS -> AppCopy.Deletion.tooManyAttempts
    FirebaseAuthErrorCodes.USER_MISMATCH,
    FirebaseAuthErrorCodes.USER_NOT_FOUND,
    FirebaseAuthErrorCodes.REQUIRES_RECENT_LOGIN,
    -> AppCopy.Deletion.authError
    else -> AppCopy.Deletion.unknownError
}
