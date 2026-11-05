package io.github.litaog.dailyrecord.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vector symbol set (ADR-022) that replaces the pre-rendered purple/wine PNGs.
 *
 * Every symbol uses a 24 x 24 viewport, a 1.8 unit outline with round caps and
 * joins, and is drawn in black so `Icon` can tint it with any theme color.
 * Symbols stay non-explicit: the masturbation module keeps the paper plane and
 * the sex module keeps the two interlocking rings.
 */
internal object DailyRecordIcons {
    val Calendar: ImageVector by lazy {
        lineIcon("Calendar") {
            stroke { roundRect(3.5f, 5f, 20.5f, 20f, 3f) }
            stroke {
                moveTo(3.5f, 9.75f)
                horizontalLineTo(20.5f)
            }
            stroke {
                moveTo(8f, 3f)
                verticalLineTo(6.5f)
                moveTo(16f, 3f)
                verticalLineTo(6.5f)
            }
            filled { roundRect(13f, 13f, 16.5f, 16.5f, 1f) }
        }
    }

    val Statistics: ImageVector by lazy {
        lineIcon("Statistics") {
            stroke {
                moveTo(4f, 20f)
                horizontalLineTo(20f)
            }
            stroke(width = 2.4f) {
                moveTo(7f, 16.5f)
                verticalLineTo(11.5f)
                moveTo(12f, 16.5f)
                verticalLineTo(6f)
                moveTo(17f, 16.5f)
                verticalLineTo(9f)
            }
        }
    }

    val Previous: ImageVector by lazy {
        lineIcon("Previous") {
            stroke {
                moveTo(14.5f, 5.5f)
                lineTo(8f, 12f)
                lineTo(14.5f, 18.5f)
            }
        }
    }

    val Next: ImageVector by lazy {
        lineIcon("Next") {
            stroke {
                moveTo(9.5f, 5.5f)
                lineTo(16f, 12f)
                lineTo(9.5f, 18.5f)
            }
        }
    }

    val Down: ImageVector by lazy {
        lineIcon("Down") {
            stroke {
                moveTo(5.5f, 9.5f)
                lineTo(12f, 16f)
                lineTo(18.5f, 9.5f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        lineIcon("Clock") {
            stroke { circle(12f, 12f, 8.5f) }
            stroke {
                moveTo(12f, 7.5f)
                verticalLineTo(12f)
                lineTo(15.2f, 14f)
            }
        }
    }

    /** Speech bubble used for the per-occurrence feeling note. */
    val Note: ImageVector by lazy {
        lineIcon("Note") {
            stroke {
                moveTo(7.5f, 4.5f)
                horizontalLineTo(16.5f)
                arcTo(3.5f, 3.5f, 0f, false, true, 20f, 8f)
                verticalLineTo(12.5f)
                arcTo(3.5f, 3.5f, 0f, false, true, 16.5f, 16f)
                horizontalLineTo(12f)
                lineTo(7.5f, 19.5f)
                verticalLineTo(16f)
                arcTo(3.5f, 3.5f, 0f, false, true, 4f, 12.5f)
                verticalLineTo(8f)
                arcTo(3.5f, 3.5f, 0f, false, true, 7.5f, 4.5f)
                close()
            }
            stroke {
                moveTo(8f, 9f)
                horizontalLineTo(16f)
                moveTo(8f, 12f)
                horizontalLineTo(13f)
            }
        }
    }

    val Edit: ImageVector by lazy {
        lineIcon("Edit") {
            stroke {
                moveTo(15.2f, 4.8f)
                lineTo(19.2f, 8.8f)
                lineTo(9f, 19f)
                lineTo(4.5f, 19.5f)
                lineTo(5f, 15f)
                close()
            }
            stroke {
                moveTo(13f, 7f)
                lineTo(17f, 11f)
            }
        }
    }

    /** Six-tooth gear: an outline ring, a hub and six short rounded teeth. */
    val Settings: ImageVector by lazy {
        lineIcon("Settings") {
            stroke { circle(12f, 12f, 6.6f) }
            stroke { circle(12f, 12f, 2.6f) }
            stroke(width = 3f) {
                for (tooth in 0 until 6) {
                    val angle = Math.toRadians(90.0 + 60.0 * tooth)
                    val x = cos(angle).toFloat()
                    val y = sin(angle).toFloat()
                    moveTo(12f + 7.7f * x, 12f + 7.7f * y)
                    lineTo(12f + 8.5f * x, 12f + 8.5f * y)
                }
            }
        }
    }

    val CloudSync: ImageVector by lazy {
        lineIcon("CloudSync") {
            stroke {
                moveTo(6.5f, 18.5f)
                arcTo(3.8f, 3.8f, 0f, false, true, 6.5f, 10.9f)
                arcTo(5.4f, 5.4f, 0f, false, true, 16.8f, 9.6f)
                arcTo(4.45f, 4.45f, 0f, false, true, 16.8f, 18.5f)
                close()
            }
            stroke {
                moveTo(11.6f, 16f)
                verticalLineTo(11.6f)
                moveTo(9.6f, 13.6f)
                lineTo(11.6f, 11.6f)
                lineTo(13.6f, 13.6f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        lineIcon("Lock") {
            stroke { roundRect(5.5f, 10.5f, 18.5f, 20.5f, 2.6f) }
            stroke {
                moveTo(8.5f, 10.5f)
                verticalLineTo(8f)
                arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 8f)
                verticalLineTo(10.5f)
            }
            stroke {
                moveTo(12f, 14.2f)
                verticalLineTo(16.6f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        lineIcon("Shield") {
            stroke {
                moveTo(12f, 3.5f)
                lineTo(19f, 6.3f)
                verticalLineTo(11.4f)
                curveTo(19f, 15.6f, 16.1f, 18.9f, 12f, 20.5f)
                curveTo(7.9f, 18.9f, 5f, 15.6f, 5f, 11.4f)
                verticalLineTo(6.3f)
                close()
            }
            stroke {
                moveTo(9f, 12.2f)
                lineTo(11.2f, 14.4f)
                lineTo(15.2f, 10.2f)
            }
        }
    }

    /** Masturbation module symbol: a paper plane. */
    val SoloModule: ImageVector by lazy {
        lineIcon("SoloModule") {
            stroke {
                moveTo(3.5f, 11.3f)
                lineTo(20.5f, 4f)
                lineTo(15.9f, 20f)
                lineTo(11.4f, 13.3f)
                close()
            }
            stroke {
                moveTo(11.4f, 13.3f)
                lineTo(20.5f, 4f)
            }
        }
    }

    /** Sex module symbol: two interlocking rings. */
    val DuoModule: ImageVector by lazy {
        lineIcon("DuoModule") {
            stroke { circle(9.2f, 12f, 5.4f) }
            stroke { circle(14.8f, 12f, 5.4f) }
        }
    }
}

private const val IconStrokeWidth = 1.8f
private val IconInk = SolidColor(Color.Black)

private fun lineIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.stroke(
    width: Float = IconStrokeWidth,
    pathBuilder: PathBuilder.() -> Unit,
) {
    path(
        stroke = IconInk,
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

private fun ImageVector.Builder.filled(pathBuilder: PathBuilder.() -> Unit) {
    path(fill = IconInk, pathBuilder = pathBuilder)
}

private fun PathBuilder.roundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    moveTo(left + radius, top)
    horizontalLineTo(right - radius)
    arcTo(radius, radius, 0f, false, true, right, top + radius)
    verticalLineTo(bottom - radius)
    arcTo(radius, radius, 0f, false, true, right - radius, bottom)
    horizontalLineTo(left + radius)
    arcTo(radius, radius, 0f, false, true, left, bottom - radius)
    verticalLineTo(top + radius)
    arcTo(radius, radius, 0f, false, true, left + radius, top)
    close()
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX + radius, centerY)
    arcTo(radius, radius, 0f, false, true, centerX - radius, centerY)
    arcTo(radius, radius, 0f, false, true, centerX + radius, centerY)
    close()
}
