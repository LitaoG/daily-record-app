package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif
private val Numeric = FontFamily.Monospace

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
)

val MetricNumberLarge = TextStyle(
    fontFamily = Numeric,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 48.sp,
)

val MetricNumberMedium = TextStyle(
    fontFamily = Numeric,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
)

/** Compact guidance line used under the account sync failure message. */
val SyncFailureGuidance = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 9.33.sp,
    lineHeight = 13.33.sp,
)
