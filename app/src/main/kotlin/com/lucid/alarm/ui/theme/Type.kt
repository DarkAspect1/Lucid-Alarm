package com.lucid.alarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
)

fun getTypographyForAppFont(fontFamily: FontFamily): Typography = Typography(
    bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = Typography.bodySmall.copy(fontFamily = fontFamily),
    titleLarge = Typography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = Typography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = Typography.titleSmall.copy(fontFamily = fontFamily),
    headlineLarge = Typography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily),
    displayLarge = Typography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = Typography.displaySmall.copy(fontFamily = fontFamily),
    labelLarge = Typography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = Typography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = Typography.labelSmall.copy(fontFamily = fontFamily)
)
