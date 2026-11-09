package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.MetricNumberLarge
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import io.github.litaog.dailyrecord.ui.uiSpec

/**
 * Preview template: Liquid Glass Elevation Tiers.
 * Visualizes the 4 elevation tiers (Flat, Subtle, Elevated, Emphasis) with their
 * directional top-lit rim highlights, milk-glass opacity, and soft module-tinted shadows.
 */
@Preview(name = "Liquid Glass Elevations - Solo", group = "Liquid Glass")
@Composable
fun LiquidGlassElevationsSoloPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DailyRecordSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Liquid Glass Visual Depth (${spec.label})",
                style = MaterialTheme.typography.titleLarge,
                color = DailyRecordText,
            )

            // 1. Flat Elevation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleLiquidGlass(
                        shape = DailyRecordShapes.Card,
                        glowColor = spec.colors.primary,
                        elevation = LiquidGlassElevation.Flat,
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text("Elevation: Flat (0dp)", fontWeight = FontWeight.Bold, color = spec.colors.primary)
                    Text("Nested container, flush surfaces, subtle border", color = DailyRecordTextSecondary, fontSize = 12.sp)
                }
            }

            // 2. Subtle Elevation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleLiquidGlass(
                        shape = DailyRecordShapes.Card,
                        glowColor = spec.colors.primary,
                        elevation = LiquidGlassElevation.Subtle,
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text("Elevation: Subtle (3dp)", fontWeight = FontWeight.Bold, color = spec.colors.primary)
                    Text("Interactive chips, capsules, and selector tracks", color = DailyRecordTextSecondary, fontSize = 12.sp)
                }
            }

            // 3. Elevated Elevation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleLiquidGlass(
                        shape = DailyRecordShapes.Card,
                        glowColor = spec.colors.primary,
                        elevation = LiquidGlassElevation.Elevated,
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text("Elevation: Elevated (8dp)", fontWeight = FontWeight.Bold, color = spec.colors.primary)
                    Text("Standard cards, today quick log island, charts", color = DailyRecordTextSecondary, fontSize = 12.sp)
                }
            }

            // 4. Emphasis Elevation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleLiquidGlass(
                        shape = DailyRecordShapes.Card,
                        glowColor = spec.colors.primary,
                        elevation = LiquidGlassElevation.Emphasis,
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text("Elevation: Emphasis (16dp)", fontWeight = FontWeight.Bold, color = spec.colors.primary)
                    Text("Focus dialogs, quick adjust popovers, app lock barrier", color = DailyRecordTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(name = "Liquid Glass Elevations - Duo", group = "Liquid Glass")
@Composable
fun LiquidGlassElevationsDuoPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DailyRecordSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Liquid Glass Visual Depth (${spec.label})",
                style = MaterialTheme.typography.titleLarge,
                color = DailyRecordText,
            )

            listOf(
                LiquidGlassElevation.Flat to "Flat (0dp) - Nested surface",
                LiquidGlassElevation.Subtle to "Subtle (3dp) - Chips & capsules",
                LiquidGlassElevation.Elevated to "Elevated (8dp) - Standard cards",
                LiquidGlassElevation.Emphasis to "Emphasis (16dp) - Modal & lock card",
            ).forEach { (elevation, label) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleLiquidGlass(
                            shape = DailyRecordShapes.Card,
                            glowColor = spec.colors.primary,
                            elevation = elevation,
                        )
                        .padding(16.dp),
                ) {
                    Text(label, fontWeight = FontWeight.SemiBold, color = spec.colors.primary)
                }
            }
        }
    }
}

/**
 * Preview template: Design Tokens Progression Ladder.
 * Displays each count ladder step (unset, 0, 1, 2, 3, 4+, disabled) confirming
 * WCAG AA contrast, luminance ordering, and pin-sharp typography.
 */
@Preview(name = "Token Progression Ladder - Solo & Duo", group = "Design Tokens")
@Composable
fun TokenProgressionLadderPreview() {
    DailyRecordPreviewTheme { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            listOf(RecordModule.HandBrew, RecordModule.Sex).forEach { mod ->
                val spec = mod.uiSpec()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${spec.label} Token Progression Ladder",
                        style = MaterialTheme.typography.titleMedium,
                        color = DailyRecordText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val states = listOf(
                            "Unset" to RecordVisualState.Unset,
                            "Zero" to RecordVisualState.ExplicitZero,
                            "1" to RecordVisualState.One,
                            "2" to RecordVisualState.Two,
                            "3" to RecordVisualState.Three,
                            "4+" to RecordVisualState.FourPlus,
                            "Future" to RecordVisualState.Disabled,
                        )
                        states.forEach { (name, state) ->
                            val visual = spec.colors.colorsFor(state)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(visual.background)
                                        .border(1.dp, visual.outline, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = when (state) {
                                            RecordVisualState.Unset -> "-"
                                            RecordVisualState.ExplicitZero -> "0"
                                            RecordVisualState.One -> "1"
                                            RecordVisualState.Two -> "2"
                                            RecordVisualState.Three -> "3"
                                            RecordVisualState.FourPlus -> "4"
                                            RecordVisualState.Disabled -> ""
                                            RecordVisualState.Focused -> "F"
                                        },
                                        color = visual.content,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                }
                                Text(name, style = MaterialTheme.typography.labelSmall, color = DailyRecordTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview template: Primary and Secondary Action Buttons.
 */
@Preview(name = "Action Buttons Preview", group = "Components")
@Composable
fun ActionButtonsPreview() {
    DailyRecordPreviewTheme { _ ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DangerActionButton(
                label = "Danger Action (Delete)",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            DailyRecordTextAction(
                label = "Secondary Action (Cancel)",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Preview template: Metric Typography Scale.
 */
@Preview(name = "Metric Typography Scale", group = "Typography")
@Composable
fun MetricTypographyPreview() {
    DailyRecordPreviewTheme { _ ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Metric Large: 42", style = MetricNumberLarge, color = DailyRecordText)
            Text("Metric Medium: 18", style = MetricNumberMedium, color = DailyRecordTextSecondary)
            Text("Display Small", style = MaterialTheme.typography.displaySmall, color = DailyRecordText)
            Text("Headline Large", style = MaterialTheme.typography.headlineLarge, color = DailyRecordText)
            Text("Title Large", style = MaterialTheme.typography.titleLarge, color = DailyRecordText)
            Text("Body Large", style = MaterialTheme.typography.bodyLarge, color = DailyRecordText)
            Text("Label Large", style = MaterialTheme.typography.labelLarge, color = DailyRecordText)
        }
    }
}
