package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import io.github.litaog.dailyrecord.ui.theme.White

enum class StatisticsPeriod(val label: String) {
    Week("周"),
    Month("月"),
    Year("年"),
    All("全部"),
}

@Composable
internal fun HandBrewBottomBar(
    selected: TopDestination,
    onSelected: (TopDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = Paper0,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BottomDestination(
                label = "日历",
                selected = selected == TopDestination.Calendar,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Calendar) },
                icon = { color -> CalendarGlyph(color) },
            )
            BottomDestination(
                label = "统计",
                selected = selected == TopDestination.Statistics,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Statistics) },
                icon = { color -> StatisticsGlyph(color) },
            )
        }
    }
}

@Composable
private fun BottomDestination(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val contentColor = if (selected) White else Ink700
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Terracotta500 else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = label + "，" + if (selected) "已选择" else "未选择"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(contentColor)
        Text(text = label, color = contentColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CalendarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .18f, size.height * .22f),
            size = Size(size.width * .64f, size.height * .62f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        drawLine(color, Offset(size.width * .18f, size.height * .40f), Offset(size.width * .82f, size.height * .40f), stroke)
        drawLine(color, Offset(size.width * .34f, size.height * .12f), Offset(size.width * .34f, size.height * .30f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .66f, size.height * .12f), Offset(size.width * .66f, size.height * .30f), stroke, StrokeCap.Round)
    }
}

@Composable
fun StatisticsGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val barWidth = size.width * .16f
        drawRoundRect(color, Offset(size.width * .18f, size.height * .48f), Size(barWidth, size.height * .34f))
        drawRoundRect(color, Offset(size.width * .42f, size.height * .25f), Size(barWidth, size.height * .57f))
        drawRoundRect(color, Offset(size.width * .66f, size.height * .58f), Size(barWidth, size.height * .24f))
    }
}

@Composable
fun BackChevronIcon(modifier: Modifier = Modifier, color: Color = Ink900) {
    Canvas(modifier.size(20.dp)) {
        val stroke = 2.4.dp.toPx()
        drawLine(
            color,
            Offset(size.width * .62f, size.height * .18f),
            Offset(size.width * .36f, size.height * .50f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * .36f, size.height * .50f),
            Offset(size.width * .62f, size.height * .82f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
fun PlaneIcon(modifier: Modifier = Modifier, color: Color = Terracotta500) {
    Canvas(modifier.size(36.dp)) {
        val path = Path().apply {
            moveTo(size.width * .50f, size.height * .06f)
            lineTo(size.width * .58f, size.height * .42f)
            lineTo(size.width * .90f, size.height * .62f)
            lineTo(size.width * .90f, size.height * .72f)
            lineTo(size.width * .58f, size.height * .62f)
            lineTo(size.width * .58f, size.height * .86f)
            lineTo(size.width * .70f, size.height * .94f)
            lineTo(size.width * .70f, size.height)
            lineTo(size.width * .50f, size.height * .94f)
            lineTo(size.width * .30f, size.height)
            lineTo(size.width * .30f, size.height * .94f)
            lineTo(size.width * .42f, size.height * .86f)
            lineTo(size.width * .42f, size.height * .62f)
            lineTo(size.width * .10f, size.height * .72f)
            lineTo(size.width * .10f, size.height * .62f)
            lineTo(size.width * .42f, size.height * .42f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun PeriodTabs(
    selected: StatisticsPeriod,
    onSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Paper0)
            .border(1.dp, Neutral300, RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatisticsPeriod.entries.forEach { period ->
            val active = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (active) Terracotta500 else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelected(period) }
                    .semantics {
                        this.selected = active
                        role = Role.Tab
                        contentDescription = period.label + "统计，" + if (active) "已选择" else "未选择"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = period.label,
                    color = if (active) White else Ink700,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 112.dp),
        color = Paper0,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Neutral300),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Terracotta500))
                Spacer(Modifier.width(7.dp))
                Text(text = label, color = Ink700, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = Ink900,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = Ink500,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun BrewCountControl(
    count: Int,
    enabled: Boolean,
    hasRecord: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 84.dp),
        color = if (enabled) Paper0 else Paper100,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Neutral300),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CountButton("减少一次", enabled && count > 0, false, onDecrease)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enabled) count.toString() else "—",
                    color = if (enabled) Ink900 else Ink500,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        !enabled -> "未来日期"
                        !hasRecord -> "尚未保存"
                        count == 0 -> "明确没冲"
                        else -> "已手冲"
                    },
                    color = Ink500,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            CountButton("增加一次", enabled && count < Int.MAX_VALUE, true, onIncrease)
        }
    }
}

@Composable
private fun CountButton(
    description: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> Paper100
        primary -> Terracotta500
        else -> Color(0xFFFFE7DE)
    }
    val content = when {
        !enabled -> Ink500
        primary -> White
        else -> Terracotta500
    }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = 2.5.dp.toPx()
            drawLine(
                color = content,
                start = Offset(2.dp.toPx(), size.height / 2f),
                end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (primary) {
                drawLine(
                    color = content,
                    start = Offset(size.width / 2f, 2.dp.toPx()),
                    end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Terracotta500 else Paper100)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) White else Ink500, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Paper0)
            .border(1.dp, if (enabled) Terracotta500 else Neutral300, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) Terracotta500 else Ink500, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatisticRow(
    label: String,
    countText: String,
    daysText: String,
    future: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (future) Paper100 else Paper0)
            .border(1.dp, Neutral300, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (future) Ink500 else Ink900,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = countText,
            color = if (future) Ink500 else Ink700,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(74.dp),
        )
        Text(
            text = daysText,
            color = if (future) Ink500 else Ink700,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(74.dp),
        )
    }
}
