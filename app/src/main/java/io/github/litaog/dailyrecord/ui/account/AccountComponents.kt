package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Terracotta400
import io.github.litaog.dailyrecord.ui.theme.Terracotta500

internal const val VPN_SYNC_DIALOG_MESSAGE =
    "请检查网络或 VPN（梯子），然后点击“立即同步”。"

internal enum class SyncFailureAction {
    Retry,
    Reauthenticate,
}

internal data class SyncFailurePresentation(
    val title: String,
    val guidance: String,
    val actionLabel: String,
    val action: SyncFailureAction,
)

internal fun SyncFailureKind.presentation(): SyncFailurePresentation = when (this) {
    SyncFailureKind.Network -> SyncFailurePresentation(
        title = "网络连接异常",
        guidance = VPN_SYNC_DIALOG_MESSAGE,
        actionLabel = "立即同步",
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Authentication -> SyncFailurePresentation(
        title = "登录状态已失效",
        guidance = "请重新登录账号，然后再次同步本机记录。",
        actionLabel = "重新登录",
        action = SyncFailureAction.Reauthenticate,
    )
    SyncFailureKind.Permission -> SyncFailurePresentation(
        title = "账号没有云端访问权限",
        guidance = "请重新登录；如果仍然失败，请稍后重试或联系开发者。",
        actionLabel = "重新登录",
        action = SyncFailureAction.Reauthenticate,
    )
    SyncFailureKind.Quota -> SyncFailurePresentation(
        title = "云服务额度暂时受限",
        guidance = "本机记录不会丢失，请稍后再点击“立即同步”。",
        actionLabel = "立即同步",
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Service -> SyncFailurePresentation(
        title = "云服务暂时不可用",
        guidance = "可能是 Firebase 临时故障，本机记录不会丢失，请稍后重试。",
        actionLabel = "立即同步",
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Data -> SyncFailurePresentation(
        title = "部分记录无法同步",
        guidance = "原始记录已保存在本机，请不要清除应用数据，并在稍后重试。",
        actionLabel = "立即同步",
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Unknown -> SyncFailurePresentation(
        title = "暂时无法完成同步",
        guidance = "未能确定失败原因，本机记录不会丢失，请稍后重试。",
        actionLabel = "立即同步",
        action = SyncFailureAction.Retry,
    )
}

@Composable
internal fun AccountTopBar(
    status: SyncStatus,
    onClick: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(color = Paper0, shadowElevation = 2.dp) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountTitle("记录每天的私密次数")
                SyncStatusChip(
                    status = status,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle("记录每天的私密次数")
                SyncStatusChip(status = status, onClick = onClick)
            }
        }
    }
}

@Composable
internal fun LocalAccountTopBar(
    onClick: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(color = Paper0, shadowElevation = 2.dp) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AccountTitle("本机记录无需 VPN（梯子），可离线使用")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DailyRecordTextAction(
                        label = "诊断",
                        onClick = onDiagnostics,
                        accessibilityLabel = "查看本机诊断信息",
                    )
                    DailyRecordTextAction(
                        label = "登录同步",
                        onClick = onClick,
                        accessibilityLabel = "登录账号并开启云同步",
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle("本机记录无需 VPN（梯子），可离线使用")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DailyRecordTextAction(
                        label = "诊断",
                        onClick = onDiagnostics,
                        accessibilityLabel = "查看本机诊断信息",
                    )
                    DailyRecordTextAction(
                        label = "登录同步",
                        onClick = onClick,
                        accessibilityLabel = "登录账号并开启云同步",
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountTitle(subtitle: String) {
    Column {
        Text(
            "私密日历",
            color = Ink900,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(subtitle, color = Ink500, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SyncStatusChip(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Terracotta400)
            .border(1.dp, Neutral300, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics {
                role = Role.Button
                contentDescription = "账号与云同步，${status.label()}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
            drawCircle(status.color())
        }
        Text(status.shortLabel(), color = Ink900, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun AccountDialog(
    email: String,
    status: SyncStatus,
    onSyncNow: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val failurePresentation = (status as? SyncStatus.Failed)?.kind?.presentation()
    DailyRecordDialog(
        title = if (confirmSignOut) "确认退出登录？" else "账号与云同步",
        subtitle = if (confirmSignOut) "云端数据会保留" else "换手机后仍可恢复全部记录",
        testTag = "account_sync_dialog",
        onDismissRequest = onDismiss,
    ) {
        if (confirmSignOut) {
            Text(
                "退出后不会删除云端记录；本机缓存仍按账号隔离，下次登录会继续同步。",
                color = Ink700,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineActionButton("返回", { confirmSignOut = false }, Modifier.weight(1f))
                DangerActionButton("确认退出", onSignOut, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(io.github.litaog.dailyrecord.ui.theme.Paper100)
                    .border(
                        1.dp,
                        Neutral300,
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(email, color = Ink900, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(9.dp)) { drawCircle(status.color()) }
                    Text(status.label(), color = Ink700, style = MaterialTheme.typography.labelLarge)
                }
                if (status is SyncStatus.Failed && failurePresentation != null) {
                    Text(
                        status.message,
                        color = Ink700,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        failurePresentation.guidance,
                        color = Terracotta500,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 9.33.sp,
                            lineHeight = 13.33.sp,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag(
                                if (status.networkRelated) {
                                    "account_vpn_sync_guidance"
                                } else {
                                    "account_sync_failure_guidance"
                                },
                            )
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Text(
                "记录会先保存在本机，断网时照常使用；联网后自动上传，并可在其他手机登录恢复。",
                color = Ink700,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp),
            )
            PrimaryActionButton(
                label = when {
                    status is SyncStatus.Syncing -> "正在同步"
                    failurePresentation != null -> failurePresentation.actionLabel
                    else -> "立即同步"
                },
                onClick = if (failurePresentation?.action == SyncFailureAction.Reauthenticate) {
                    onSignOut
                } else {
                    onSyncNow
                },
                enabled = status !is SyncStatus.Syncing,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton("关闭", onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
            DailyRecordTextAction(
                label = "查看诊断信息",
                onClick = onOpenDiagnostics,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            )
            DailyRecordTextAction(
                label = "退出登录",
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                danger = true,
            )
            DailyRecordTextAction(
                label = "删除账号与云端数据",
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                danger = true,
            )
        }
    }
}

internal fun SyncStatus.label(): String = when (this) {
    SyncStatus.NotConfigured -> "云端尚未配置"
    SyncStatus.Offline -> "当前离线，记录已保存在本机"
    SyncStatus.Syncing -> "正在同步"
    SyncStatus.UpToDate -> "云端已同步"
    is SyncStatus.Pending -> "有 $count 条记录等待同步"
    is SyncStatus.Failed -> kind.presentation().title
}

private fun SyncStatus.shortLabel(): String = when (this) {
    SyncStatus.NotConfigured -> "未配置"
    SyncStatus.Offline -> "离线"
    SyncStatus.Syncing -> "同步中"
    SyncStatus.UpToDate -> "已同步"
    is SyncStatus.Pending -> "待同步 $count"
    is SyncStatus.Failed -> "需重试"
}

private fun SyncStatus.color() = when (this) {
    SyncStatus.UpToDate -> androidx.compose.ui.graphics.Color(0xFF2E7D5B)
    SyncStatus.Syncing -> Terracotta500
    SyncStatus.Offline, is SyncStatus.Pending -> androidx.compose.ui.graphics.Color(0xFF8A6A18)
    SyncStatus.NotConfigured, is SyncStatus.Failed -> androidx.compose.ui.graphics.Color(0xFF9B3A32)
}
