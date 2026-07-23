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
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.HandBrewDialog
import io.github.litaog.dailyrecord.ui.components.HandBrewTextAction
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
    "请打开 VPN（梯子），然后点击“立即同步”。"

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
                AccountTitle("只记录手冲次数")
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
                AccountTitle("只记录手冲次数")
                SyncStatusChip(status = status, onClick = onClick)
            }
        }
    }
}

@Composable
internal fun LocalAccountTopBar(onClick: () -> Unit) {
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
                HandBrewTextAction(
                    label = "登录同步",
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth(),
                    accessibilityLabel = "登录账号并开启云同步",
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
                AccountTitle("本机记录无需 VPN（梯子），可离线使用")
                HandBrewTextAction(
                    label = "登录同步",
                    onClick = onClick,
                    accessibilityLabel = "登录账号并开启云同步",
                )
            }
        }
    }
}

@Composable
private fun AccountTitle(subtitle: String) {
    Column {
        Text(
            "手冲日历",
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
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    HandBrewDialog(
        title = if (confirmSignOut) "确认退出登录？" else "账号与云同步",
        subtitle = if (confirmSignOut) "云端数据会保留" else "换手机后仍可恢复手冲记录",
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
                if (status is SyncStatus.Failed && status.networkRelated) {
                    Text(
                        VPN_SYNC_DIALOG_MESSAGE,
                        color = Terracotta500,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag("account_vpn_sync_guidance")
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
                label = if (status is SyncStatus.Syncing) "正在同步" else "立即同步",
                onClick = onSyncNow,
                enabled = status !is SyncStatus.Syncing,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton("关闭", onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
            HandBrewTextAction(
                label = "退出登录",
                onClick = { confirmSignOut = true },
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
    is SyncStatus.Failed -> "同步失败：$message"
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
