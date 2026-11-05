package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * User-selectable liquid glass color themes (ADR-022).
 *
 * Every theme gives the two record modules their own token set; the modules
 * never share a palette and never borrow each other's colors. [id] is the
 * persisted value, so an id must never be renamed once released.
 *
 * Contrast contract, verified for every entry by `DesignTokensTest`:
 * - white text on [RecordModuleColorTokens.primary] and
 *   [RecordModuleColorTokens.strong] reaches WCAG AA (4.5:1);
 * - graphite text on soft / medium / intense / peak reaches 4.5:1;
 * - unset > soft > medium > intense > peak in relative luminance.
 *
 * Gradients and glows are decorative only and never sit behind body text.
 */
enum class DailyRecordColorTheme(val id: String) {
    Aurora("aurora"),
    GlacialCoral("glacial_coral"),
    TwilightLilac("twilight_lilac"),
    EmeraldBerry("emerald_berry"),
    TitaniumChampagne("titanium_champagne"),
    CreamLavender("pantone_cream_lavender"),
    SpringSky("pantone_spring_sky"),
    BuffGlacier("pantone_buff_glacier"),
    DreamBlush("pantone_dream_blush"),
    ;

    val palette: DailyRecordThemePalette
        get() = paletteFor(this)

    companion object {
        val Default: DailyRecordColorTheme = Aurora

        fun fromId(id: String?): DailyRecordColorTheme =
            entries.firstOrNull { it.id == id } ?: Default
    }
}

@Immutable
data class DailyRecordThemePalette(
    /** Masturbation module tokens. */
    val solo: RecordModuleColorTokens,
    /** Sex module tokens. */
    val duo: RecordModuleColorTokens,
)

/**
 * Process-wide selected theme. Like `AppLanguageState`, it is resolved from the
 * persisted preference before the first composition, and a change recreates
 * the activity so no screen keeps colors from the previous theme.
 */
object DailyRecordThemeState {
    @Volatile
    var current: DailyRecordColorTheme = DailyRecordColorTheme.Default
}

private fun tokens(
    gradient: List<Long>,
    glow: Long,
    unset: Long,
    soft: Long,
    medium: Long,
    intense: Long,
    peak: Long,
    primary: Long,
    strong: Long,
): RecordModuleColorTokens = RecordModuleColorTokens(
    primary = Color(primary),
    strong = Color(strong),
    soft = Color(soft),
    medium = Color(medium),
    intense = Color(intense),
    unset = Color(unset),
    peak = Color(peak),
    onPeak = DailyRecordText,
    gradient = gradient.map { Color(it) },
    glow = Color(glow),
)

private fun palette(
    solo: RecordModuleColorTokens,
    duo: RecordModuleColorTokens,
) = DailyRecordThemePalette(solo = solo, duo = duo)

// Flagship default: Aurora Iris (solo) and Sunset Rose (duo).
// b1 lists #A855F7 as the solo 4+ step, but neither white (3.95:1) nor graphite
// (3.70:1) text is readable on it, so the 4+ step uses #C084FC; the duo accent
// uses #DE1B46 because #E11D48 measures 4.49:1 on the canvas.
private val AuroraPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF7C3AED, 0xFF9333EA, 0xFFC084FC),
        glow = 0x188B5CF6,
        unset = 0xFFFAF7FE,
        soft = 0xFFF5EEFD,
        medium = 0xFFE9D5FF,
        intense = 0xFFD8B4FE,
        peak = 0xFFC084FC,
        primary = 0xFF7C3AED,
        strong = 0xFF6D28D9,
    ),
    duo = tokens(
        gradient = listOf(0xFFE11D48, 0xFFF43F5E, 0xFFFDA4AF),
        glow = 0x18F43F5E,
        unset = 0xFFFFF8F9,
        soft = 0xFFFFF1F2,
        medium = 0xFFFFE4E6,
        intense = 0xFFFECDD3,
        peak = 0xFFFB7185,
        primary = 0xFFDE1B46,
        strong = 0xFFBE123C,
    ),
)

private val GlacialCoralPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF0284C7, 0xFF0EA5E9, 0xFF38BDF8),
        glow = 0x180EA5E9,
        unset = 0xFFF0F9FF,
        soft = 0xFFE0F2FE,
        medium = 0xFFBAE6FD,
        intense = 0xFF7DD3FC,
        peak = 0xFF38BDF8,
        primary = 0xFF0369A1,
        strong = 0xFF075985,
    ),
    duo = tokens(
        gradient = listOf(0xFFEA580C, 0xFFF97316, 0xFFFB923C),
        glow = 0x18F97316,
        unset = 0xFFFFF6EA,
        soft = 0xFFFFEDD5,
        medium = 0xFFFED7AA,
        intense = 0xFFFDBA74,
        peak = 0xFFFB923C,
        primary = 0xFFC2410C,
        strong = 0xFF9A3412,
    ),
)

private val TwilightLilacPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF4F46E5, 0xFF6366F1, 0xFFA5B4FC),
        glow = 0x186366F1,
        unset = 0xFFF0F3FF,
        soft = 0xFFE0E7FF,
        medium = 0xFFC7D2FE,
        intense = 0xFFA5B4FC,
        peak = 0xFF818CF8,
        primary = 0xFF4F46E5,
        strong = 0xFF3730A3,
    ),
    duo = tokens(
        gradient = listOf(0xFFBE123C, 0xFFE11D48, 0xFFF472B6),
        glow = 0x18E11D48,
        unset = 0xFFFFF2F3,
        soft = 0xFFFFE4E6,
        medium = 0xFFFECDD3,
        intense = 0xFFFDA4AF,
        peak = 0xFFF472B6,
        primary = 0xFFBE123C,
        strong = 0xFF9F1239,
    ),
)

private val EmeraldBerryPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF059669, 0xFF10B981, 0xFF6EE7B7),
        glow = 0x1810B981,
        unset = 0xFFE8FDF2,
        soft = 0xFFD1FAE5,
        medium = 0xFFA7F3D0,
        intense = 0xFF6EE7B7,
        peak = 0xFF34D399,
        primary = 0xFF047857,
        strong = 0xFF065F46,
    ),
    duo = tokens(
        gradient = listOf(0xFF9D174D, 0xFFDB2777, 0xFFF472B6),
        glow = 0x18DB2777,
        unset = 0xFFFEF3F9,
        soft = 0xFFFCE7F3,
        medium = 0xFFFBCFE8,
        intense = 0xFFF9A8D4,
        peak = 0xFFF472B6,
        primary = 0xFFBE185D,
        strong = 0xFF9D174D,
    ),
)

private val TitaniumChampagnePalette = palette(
    solo = tokens(
        gradient = listOf(0xFF334155, 0xFF475569, 0xFF94A3B8),
        glow = 0x14475569,
        unset = 0xFFF1F4F8,
        soft = 0xFFE2E8F0,
        medium = 0xFFCBD5E1,
        intense = 0xFFB4C0CF,
        peak = 0xFF94A3B8,
        primary = 0xFF475569,
        strong = 0xFF334155,
    ),
    duo = tokens(
        gradient = listOf(0xFFB45309, 0xFFD97706, 0xFFFBBF24),
        glow = 0x18D97706,
        unset = 0xFFFFF9E3,
        soft = 0xFFFEF3C7,
        medium = 0xFFFDE68A,
        intense = 0xFFFCD34D,
        peak = 0xFFFBBF24,
        primary = 0xFFB45309,
        strong = 0xFF92400E,
    ),
)

// Scheme A: PANTONE 12-0713 TPX Cream -> 15-3917 TPX Lavender.
private val CreamLavenderPalette = palette(
    solo = tokens(
        gradient = listOf(0xFFA78BFA, 0xFFD2CDFF),
        glow = 0x24D2CDFF,
        unset = 0xFFFAF9FF,
        soft = 0xFFF5F3FF,
        medium = 0xFFEDE9FE,
        intense = 0xFFDDD6FE,
        peak = 0xFFC4B5FD,
        primary = 0xFF7C3AED,
        strong = 0xFF5B21B6,
    ),
    duo = tokens(
        gradient = listOf(0xFFFFFFD0, 0xFFFDE047, 0xFFF472B6),
        glow = 0x20F472B6,
        unset = 0xFFFFFEF4,
        soft = 0xFFFEFCE8,
        medium = 0xFFFEF08A,
        intense = 0xFFFBCFE8,
        peak = 0xFFF472B6,
        primary = 0xFFBE185D,
        strong = 0xFF9D174D,
    ),
)

// Scheme B: PANTONE 12-0717 TPX Spring Green -> 15-4015 TPX Blue Gray.
private val SpringSkyPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF38BDF8, 0xFFB9DFFF),
        glow = 0x22B9DFFF,
        unset = 0xFFF8FCFF,
        soft = 0xFFF0F9FF,
        medium = 0xFFE0F2FE,
        intense = 0xFFBAE6FD,
        peak = 0xFF7DD3FC,
        primary = 0xFF0369A1,
        strong = 0xFF075985,
    ),
    duo = tokens(
        gradient = listOf(0xFFCFFFBD, 0xFF86EFAC, 0xFF34D399),
        glow = 0x1CCFFFBD,
        unset = 0xFFF8FEFA,
        soft = 0xFFF0FDF4,
        medium = 0xFFDCFCE7,
        intense = 0xFFBBF7D0,
        peak = 0xFF86EFAC,
        primary = 0xFF15803D,
        strong = 0xFF166534,
    ),
)

// Scheme C: PANTONE 12-0713 TCX Buff -> 15-4015 TPX Ice Blue.
private val BuffGlacierPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF0284C7, 0xFF38BDF8, 0xFFD8EFFF),
        glow = 0x26D8EFFF,
        unset = 0xFFF8FCFF,
        soft = 0xFFF0F9FF,
        medium = 0xFFE0F2FE,
        intense = 0xFFBAE6FD,
        peak = 0xFF38BDF8,
        primary = 0xFF0369A1,
        strong = 0xFF075985,
    ),
    duo = tokens(
        gradient = listOf(0xFFFFFFD0, 0xFFFDE68A, 0xFFFB923C),
        glow = 0x20FFFFD0,
        unset = 0xFFFFFDF5,
        soft = 0xFFFFFBEB,
        medium = 0xFFFEF3C7,
        intense = 0xFFFDE68A,
        peak = 0xFFFBBF24,
        primary = 0xFFC2410C,
        strong = 0xFF9A3412,
    ),
)

// Scheme D: PANTONE 16-4910 TPX Light Blue -> 13-1108 TPX Blush Pink.
private val DreamBlushPalette = palette(
    solo = tokens(
        gradient = listOf(0xFF6366F1, 0xFF818CF8, 0xFFDBDDFF),
        glow = 0x24DBDDFF,
        unset = 0xFFF7F9FF,
        soft = 0xFFEEF2FF,
        medium = 0xFFE0E7FF,
        intense = 0xFFC7D2FE,
        peak = 0xFFA5B4FC,
        primary = 0xFF4F46E5,
        strong = 0xFF3730A3,
    ),
    duo = tokens(
        gradient = listOf(0xFFE11D48, 0xFFF43F5E, 0xFFFFD0D0),
        glow = 0x28FFD0D0,
        unset = 0xFFFFF8F9,
        soft = 0xFFFFF1F2,
        medium = 0xFFFFE4E6,
        intense = 0xFFFECDD3,
        peak = 0xFFFDA4AF,
        primary = 0xFFDE1B46,
        strong = 0xFFBE123C,
    ),
)

private fun paletteFor(theme: DailyRecordColorTheme): DailyRecordThemePalette = when (theme) {
    DailyRecordColorTheme.Aurora -> AuroraPalette
    DailyRecordColorTheme.GlacialCoral -> GlacialCoralPalette
    DailyRecordColorTheme.TwilightLilac -> TwilightLilacPalette
    DailyRecordColorTheme.EmeraldBerry -> EmeraldBerryPalette
    DailyRecordColorTheme.TitaniumChampagne -> TitaniumChampagnePalette
    DailyRecordColorTheme.CreamLavender -> CreamLavenderPalette
    DailyRecordColorTheme.SpringSky -> SpringSkyPalette
    DailyRecordColorTheme.BuffGlacier -> BuffGlacierPalette
    DailyRecordColorTheme.DreamBlush -> DreamBlushPalette
}
