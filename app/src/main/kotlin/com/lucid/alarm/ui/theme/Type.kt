package com.lucid.alarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val defaultPlatformStyle = PlatformTextStyle(includeFontPadding = false)
private val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    )
)

private fun TextStyle.normalize(font: FontFamily): TextStyle = copy(
    fontFamily = font,
    platformStyle = defaultPlatformStyle,
    lineHeightStyle = defaultLineHeightStyle
)

fun getTypographyForAppFont(fontFamily: FontFamily): Typography = Typography(
    bodyLarge = Typography.bodyLarge.normalize(fontFamily),
    bodyMedium = Typography.bodyMedium.normalize(fontFamily),
    bodySmall = Typography.bodySmall.normalize(fontFamily),
    titleLarge = Typography.titleLarge.normalize(fontFamily),
    titleMedium = Typography.titleMedium.normalize(fontFamily),
    titleSmall = Typography.titleSmall.normalize(fontFamily),
    headlineLarge = Typography.headlineLarge.normalize(fontFamily),
    headlineMedium = Typography.headlineMedium.normalize(fontFamily),
    headlineSmall = Typography.headlineSmall.normalize(fontFamily),
    displayLarge = Typography.displayLarge.normalize(fontFamily),
    displayMedium = Typography.displayMedium.normalize(fontFamily),
    displaySmall = Typography.displaySmall.normalize(fontFamily),
    labelLarge = Typography.labelLarge.normalize(fontFamily),
    labelMedium = Typography.labelMedium.normalize(fontFamily),
    labelSmall = Typography.labelSmall.normalize(fontFamily)
)
