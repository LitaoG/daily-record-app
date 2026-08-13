package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary

/**
 * A quiet, two-tone settings glyph for the private journal.
 *
 * The geometry is adapted from Tabler Icons' `settings-heart` SVG. The source
 * paths are kept as vectors and split into gear and heart so the heart can
 * follow the active record module. The small heart is filled for mobile
 * legibility; the source's original outline is too light at 28dp.
 *
 * Source: https://github.com/tabler/tabler-icons/blob/main/icons/outline/settings-heart.svg
 * Copyright (c) 2020-2026 Paweł Kuna. Licensed under the MIT License.
 */
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    gearTint: Color = DailyRecordTextSecondary,
    heartTint: Color = DailyRecordDefaultAccent,
) {
    val paths = remember { TablerSettingsHeartPaths() }
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(modifier.then(semanticsModifier).size(DailyRecordSizes.SettingsIcon)) {
        val scale = minOf(size.width, size.height) / ViewportSize
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            paths.gear.forEach { path ->
                drawPath(path, color = gearTint, style = TablerStroke)
            }
            drawPath(paths.heart, color = heartTint)
        }
    }
}

private const val ViewportSize = 24f
private val TablerStroke = Stroke(
    width = 1.85f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

private class TablerSettingsHeartPaths {
    val gear = listOf(
        parsePath(
            "M11.231 20.828a1.668 1.668 0 0 1 -.906 -1.145a1.724 1.724 0 0 0 -2.573 -1.066c-1.543 .94 -3.31 -.826 -2.37 -2.37a1.724 1.724 0 0 0 -1.065 -2.572c-1.756 -.426 -1.756 -2.924 0 -3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94 -1.543 .826 -3.31 2.37 -2.37c1 .608 2.296 .07 2.572 -1.065c.426 -1.756 2.924 -1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543 -.94 3.31 .826 2.37 2.37a1.724 1.724 0 0 0 1.065 2.572c.509 .123 .87 .421 1.084 .792",
        ),
        parsePath("M14.882 11.165a3.001 3.001 0 1 0 -4.31 3.474"),
    )
    val heart = Path().apply {
        fillType = PathFillType.NonZero
        moveTo(18f, 20.75f)
        cubicTo(17.6f, 20.4f, 13.05f, 16.85f, 12.25f, 15.85f)
        cubicTo(11.45f, 14.85f, 11.35f, 13.3f, 12.45f, 12.2f)
        cubicTo(13.6f, 11.05f, 15.5f, 11.35f, 16.5f, 12.55f)
        cubicTo(17.5f, 11.35f, 19.4f, 11.05f, 20.55f, 12.2f)
        cubicTo(21.65f, 13.3f, 21.55f, 14.85f, 20.75f, 15.85f)
        cubicTo(19.95f, 16.85f, 18.4f, 18.25f, 18f, 20.75f)
        close()
    }
}

private fun parsePath(pathData: String) = PathParser()
    .parsePathString(pathData)
    .toNodes()
    .toPath()
