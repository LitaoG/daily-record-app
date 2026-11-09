package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordLiquidBackdrop
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState
import io.github.litaog.dailyrecord.ui.uiSpec

/**
 * MultiPreview annotations providing standardized device, module, and theme matrices.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Preview(
    name = "Solo Module (Masturbation)",
    group = "Module Modes",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
)
@Preview(
    name = "Duo Module (Sex)",
    group = "Module Modes",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
)
annotation class PreviewModules

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Preview(name = "1. Aurora (Default)", group = "Themes", showBackground = true)
@Preview(name = "2. Glacial Coral", group = "Themes", showBackground = true)
@Preview(name = "3. Twilight Lilac", group = "Themes", showBackground = true)
@Preview(name = "4. Emerald Berry", group = "Themes", showBackground = true)
@Preview(name = "5. Titanium Champagne", group = "Themes", showBackground = true)
@Preview(name = "6. Cream Lavender (Pantone)", group = "Themes", showBackground = true)
@Preview(name = "7. Spring Sky (Pantone)", group = "Themes", showBackground = true)
@Preview(name = "8. Buff Glacier (Pantone)", group = "Themes", showBackground = true)
@Preview(name = "9. Dream Blush (Pantone)", group = "Themes", showBackground = true)
annotation class PreviewThemes

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Preview(name = "Font 100% (Normal)", group = "Accessibility", fontScale = 1.0f, showBackground = true)
@Preview(name = "Font 150% (Medium)", group = "Accessibility", fontScale = 1.5f, showBackground = true)
@Preview(name = "Font 200% (WCAG Maximum)", group = "Accessibility", fontScale = 2.0f, showBackground = true)
annotation class PreviewFontScales

/**
 * Reusable Jetpack Compose `@Preview` foundation wrapper (ADR-022 compliant).
 *
 * Configures:
 * 1. Theme selection ([DailyRecordThemeState.current] set to [theme]).
 * 2. Module selection ([RecordModule.HandBrew] vs [RecordModule.Sex]).
 * 3. Language & Terminology style ([AppLanguageState.current] set to [language] + [terminologyStyle]).
 * 4. Liquid Glass Backdrop atmosphere with pearl canvas and faint ambient glow.
 * 5. Restores previous global state upon disposal to avoid test/runtime cross-talk.
 */
@Composable
internal fun DailyRecordPreviewTheme(
    modifier: Modifier = Modifier,
    theme: DailyRecordColorTheme = DailyRecordColorTheme.Default,
    module: RecordModule = RecordModule.HandBrew,
    language: AppLanguage = AppLanguage.ZH,
    terminologyStyle: TerminologyStyle = TerminologyStyle.ELEGANT,
    showBackdrop: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.(moduleSpec: RecordModuleUiSpec) -> Unit,
) {
    // Preserve ambient state to guarantee isolation
    val previousTheme = DailyRecordThemeState.current
    val previousStrings = AppLanguageState.current

    DailyRecordThemeState.current = theme
    AppLanguageState.current = language.strings(terminologyStyle)

    DisposableEffect(theme, language, terminologyStyle) {
        onDispose {
            DailyRecordThemeState.current = previousTheme
            AppLanguageState.current = previousStrings
        }
    }

    val spec = module.uiSpec()

    DailyRecordTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DailyRecordCanvas),
            contentAlignment = contentAlignment,
        ) {
            if (showBackdrop) {
                DailyRecordLiquidBackdrop(colors = spec.colors)
            }
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = contentAlignment,
            ) {
                content(spec)
            }
        }
    }
}
