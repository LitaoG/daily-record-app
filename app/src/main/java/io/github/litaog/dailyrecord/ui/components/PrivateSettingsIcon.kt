package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
 * A restrained calendar-and-cog glyph for the Daily Record settings entry.
 *
 * The geometry is adapted from Lucide's `calendar-cog` SVG. The calendar stays
 * in the app's quiet graphite tone while the small cog uses the active record
 * module color, making the entry feel native to the current module without
 * mixing unrelated symbols.
 *
 * Source: https://github.com/lucide-icons/lucide/blob/main/icons/calendar-cog.svg
 * Copyright (c) 2026 Lucide Icons and Contributors. Licensed under the ISC License.
 */
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    calendarTint: Color = DailyRecordTextSecondary,
    moduleTint: Color = DailyRecordDefaultAccent,
) {
    val paths = remember { LucideCalendarCogPaths() }
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(modifier.then(semanticsModifier).size(DailyRecordSizes.SettingsIcon)) {
        val scale = minOf(size.width, size.height) / ViewportSize
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            paths.calendar.forEach { path ->
                drawPath(path, color = calendarTint, style = CalendarStroke)
            }
            paths.cog.forEach { path ->
                drawPath(path, color = moduleTint, style = CogStroke)
            }
            drawCircle(
                color = moduleTint,
                center = CogCenter,
                radius = CogRadius,
                style = CogStroke,
            )
        }
    }
}

private const val ViewportSize = 24f
private val CogCenter = Offset(18f, 18f)
private const val CogRadius = 3f
private val CalendarStroke = Stroke(
    width = 1.85f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)
private val CogStroke = Stroke(
    width = 1.9f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

private class LucideCalendarCogPaths {
    val calendar = listOf(
        parsePath("M16 2v3"),
        parsePath("M21 10.5V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h5.5"),
        parsePath("M3 9h18"),
        parsePath("M8 2v3"),
    )

    val cog = listOf(
        parsePath("m15.228 16.852-.923-.383"),
        parsePath("m15.228 19.148-.923.383"),
        parsePath("m16.47 14.305.382.923"),
        parsePath("m16.852 20.772-.383.924"),
        parsePath("m19.148 15.228.383-.923"),
        parsePath("m19.53 21.696-.382-.924"),
        parsePath("m20.773 16.852.924-.383"),
        parsePath("m20.773 19.148.924.383"),
    )
}

private fun parsePath(pathData: String) = PathParser()
    .parsePathString(pathData)
    .toNodes()
    .toPath()
