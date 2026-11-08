package io.github.litaog.dailyrecord.ui.record

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun RecordDetailsRegion(
    count: Int,
    detailsExpanded: Boolean,
    entries: List<RecordDetailDraft>,
    accent: Color,
    theme: BrandIconTheme,
    colors: RecordModuleColorTokens,
    onCollapse: () -> Unit,
    onTimeClick: (Int, DetailTimeTarget) -> Unit,
    onFeelingToggle: (Int) -> Unit,
    onFeelingChange: (Int, String) -> Unit,
    onExpand: () -> Unit,
) {
    if (count > 0) {
        if (count > MAX_RECORD_DETAIL_EDITOR_ROWS) {
            Text(
                text = AppCopy.Record.detailEntryUnavailable,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        } else if (detailsExpanded) {
            RecordDetailsSection(
                entries = entries,
                accent = accent,
                theme = theme,
                onCollapse = onCollapse,
                onTimeClick = onTimeClick,
                onFeelingToggle = onFeelingToggle,
                onFeelingChange = onFeelingChange,
            )
        } else {
            DetailEntryButton(
                count = count,
                accent = accent,
                colors = colors,
                onClick = onExpand,
            )
        }
    }
}
