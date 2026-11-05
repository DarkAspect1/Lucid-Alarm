package com.lucid.alarm.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class ClockRenderOutput(
    val clockBounds: RectF,
    val dateBounds: RectF
)

object ClockWidgetRenderer {

    fun applyAlpha(color: Int, alphaRatio: Float): Int {
        val a = (Color.alpha(color) * alphaRatio).toInt().coerceIn(0, 255)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a, r, g, b)
    }

    fun calculateOptimalDigitalSize(
        style: ClockStyle,
        availableWidthPx: Float,
        availableHeightPx: Float,
        timeStr: String,
        hourStr: String,
        minStr: String,
        amPmStr: String,
        clockTypeface: Typeface,
        appTypeface: Typeface,
        density: Float
    ): Float {
        var low = 14f * density
        var high = minOf(availableWidthPx, availableHeightPx) * 0.95f
        var optimal = low

        val padX = 16f * density
        val padTop = 10f * density
        val padBottom = 10f * density
        val maxW = (availableWidthPx - padX).coerceAtLeast(60f)

        val isStacked = style == ClockStyle.STYLE_2 || style == ClockStyle.STYLE_5

        for (iter in 0 until 16) {
            val mid = (low + high) / 2f
            val amPmSize = (mid * 0.28f).coerceIn(10f * density, 22f * density)

            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = mid
                typeface = clockTypeface
            }
            val aPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = amPmSize
                typeface = appTypeface
            }

            val tFm = tPaint.fontMetrics

            val fits: Boolean = if (!isStacked) {
                val timeW = tPaint.measureText(timeStr)
                val amPmW = if (amPmStr.isNotEmpty()) aPaint.measureText(amPmStr) + (mid * 0.12f) else 0f
                val totalW = timeW + 2f * amPmW

                val timeHeight = tFm.bottom - tFm.top
                val timeHalfH = timeHeight / 2f
                val topFits = timeHalfH <= (availableHeightPx / 2f) - padTop
                val bottomFits = timeHalfH <= (availableHeightPx / 2f) - padBottom

                totalW <= maxW && topFits && bottomFits
            } else {
                val hourW = tPaint.measureText(hourStr)
                val minW = tPaint.measureText(minStr)
                val amPmW = if (amPmStr.isNotEmpty()) aPaint.measureText(amPmStr) + (mid * 0.12f) else 0f
                val totalW = maxOf(hourW, minW) + 2f * amPmW

                val digitH = -tFm.ascent
                val gap = mid * 0.10f
                val stackH = digitH * 2f + gap
                val stackHalfH = stackH / 2f
                val topFits = stackHalfH <= (availableHeightPx / 2f) - padTop
                val bottomFits = stackHalfH <= (availableHeightPx / 2f) - padBottom

                totalW <= maxW && topFits && bottomFits
            }

            if (fits) {
                optimal = mid
                low = mid + 1f
            } else {
                high = mid - 1f
            }
        }
        return optimal
    }

    fun renderClock(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        config: ClockWidgetConfig,
        cal: Calendar,
        is24Hour: Boolean,
        removeLeadingZero: Boolean,
        clockTypeface: Typeface,
        appTypeface: Typeface,
        density: Float,
        amPmStr: String
    ): RectF {
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f
        val clockCenterX = centerX + (config.clockOffsetX * widthPx)
        val clockCenterY = centerY + (config.clockOffsetY * heightPx)

        if (config.style.isAnalog) {
            val dialPadding = 8f * density
            val baseRadius = minOf(widthPx / 2f - dialPadding, heightPx / 2f - dialPadding).coerceAtLeast(24f * density)
            val radius = (baseRadius * config.clockScale).coerceIn(12f * density, minOf(widthPx / 2f, heightPx / 2f))

            val analogShader = if (config.clockGradientEnabled) {
                LinearGradient(
                    clockCenterX - radius, clockCenterY - radius,
                    clockCenterX + radius, clockCenterY + radius,
                    config.colorHex, config.clockGradientEndColorHex,
                    Shader.TileMode.CLAMP
                )
            } else null

            val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = applyAlpha(config.colorHex, 0.45f)
                style = Paint.Style.STROKE
                strokeWidth = (1.6f * density * config.clockScale).coerceIn(1f, 8f)
                shader = analogShader
            }
            canvas.drawCircle(clockCenterX, clockCenterY, radius, outerPaint)

            if (config.style == ClockStyle.STYLE_4) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.colorHex
                    textSize = (radius * 0.22f).coerceIn(8f * density, 40f * density)
                    typeface = clockTypeface
                    textAlign = Paint.Align.CENTER
                    shader = analogShader
                }
                val fm = textPaint.fontMetrics
                val yOffset = (fm.descent + fm.ascent) / 2f
                val numRadius = radius - (textPaint.textSize * 0.9f)

                for (hour in 1..12) {
                    val angleDeg = hour * 30.0 - 90.0
                    val rad = Math.toRadians(angleDeg)
                    val x = clockCenterX + (numRadius * cos(rad)).toFloat()
                    val y = clockCenterY + (numRadius * sin(rad)).toFloat() - yOffset
                    canvas.drawText(hour.toString(), x, y, textPaint)
                }
            } else {
                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.colorHex
                    strokeCap = Paint.Cap.ROUND
                    shader = analogShader
                }
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val isMajor = i % 3 == 0
                    val tickLen = (if (isMajor) 9f * density else 4.5f * density) * config.clockScale
                    val startR = radius - tickLen
                    val startX = clockCenterX + (startR * cos(angle)).toFloat()
                    val startY = clockCenterY + (startR * sin(angle)).toFloat()
                    val endX = clockCenterX + (radius * cos(angle)).toFloat()
                    val endY = clockCenterY + (radius * sin(angle)).toFloat()

                    tickPaint.strokeWidth = (if (isMajor) 2.5f * density else 1.2f * density) * config.clockScale
                    canvas.drawLine(startX, startY, endX, endY, tickPaint)
                }
            }

            val hours = cal.get(Calendar.HOUR)
            val minutes = cal.get(Calendar.MINUTE)
            val hourDeg = (hours % 12 + minutes / 60f) * 30f
            val minDeg = minutes * 6f

            canvas.save()
            canvas.rotate(hourDeg, clockCenterX, clockCenterY)
            val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorHex
                strokeWidth = (if (config.style == ClockStyle.STYLE_4) 4.2f * density else 3.2f * density) * config.clockScale
                strokeCap = Paint.Cap.ROUND
                shader = analogShader
            }
            canvas.drawLine(clockCenterX, clockCenterY + (6f * density * config.clockScale), clockCenterX, clockCenterY - (radius * 0.55f), hourHandPaint)
            canvas.restore()

            canvas.save()
            canvas.rotate(minDeg, clockCenterX, clockCenterY)
            val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorHex
                strokeWidth = (if (config.style == ClockStyle.STYLE_4) 2.8f * density else 2.2f * density) * config.clockScale
                strokeCap = Paint.Cap.ROUND
                shader = analogShader
            }
            canvas.drawLine(clockCenterX, clockCenterY + (8f * density * config.clockScale), clockCenterX, clockCenterY - (radius * 0.82f), minHandPaint)
            canvas.restore()

            val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorHex
                style = Paint.Style.FILL
                shader = analogShader
            }
            canvas.drawCircle(clockCenterX, clockCenterY, 3.8f * density * config.clockScale, pivotPaint)

            return RectF(
                clockCenterX - radius,
                clockCenterY - radius,
                clockCenterX + radius,
                clockCenterY + radius
            )
        }

        val hourVal = if (is24Hour) {
            cal.get(Calendar.HOUR_OF_DAY)
        } else {
            val h = cal.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minVal = cal.get(Calendar.MINUTE)
        val hourStr = if (removeLeadingZero) hourVal.toString() else String.format(Locale.US, "%02d", hourVal)
        val minStr = String.format(Locale.US, "%02d", minVal)
        val timeStr = "$hourStr:$minStr"

        val baseSizePx = calculateOptimalDigitalSize(
            style = config.style,
            availableWidthPx = widthPx,
            availableHeightPx = heightPx,
            timeStr = timeStr,
            hourStr = hourStr,
            minStr = minStr,
            amPmStr = amPmStr,
            clockTypeface = clockTypeface,
            appTypeface = appTypeface,
            density = density
        )

        val clockTextSize = (baseSizePx * config.clockScale).coerceIn(10f * density, 300f * density)
        val amPmTextSize = (clockTextSize * 0.28f).coerceIn(8f * density, 40f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.colorHex
            textSize = clockTextSize
            typeface = clockTypeface
        }
        val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.colorHex
            textSize = amPmTextSize
            typeface = appTypeface
        }

        val isStacked = config.style == ClockStyle.STYLE_2 || config.style == ClockStyle.STYLE_5

        if (!isStacked) {
            val timeBounds = Rect()
            timePaint.getTextBounds(timeStr, 0, timeStr.length, timeBounds)
            val timeWidth = timePaint.measureText(timeStr)
            val amPmGap = if (amPmStr.isNotEmpty()) clockTextSize * 0.12f else 0f
            val amPmWidth = if (amPmStr.isNotEmpty()) amPmPaint.measureText(amPmStr) else 0f
            val startX = clockCenterX - timeWidth / 2f
            val timeBaseline = clockCenterY - (timeBounds.top + timeBounds.bottom) / 2f

            val amPmX = startX + timeWidth + amPmGap
            val rightEdge = if (amPmStr.isNotEmpty()) amPmX + amPmWidth else startX + timeWidth

            if (config.clockGradientEnabled) {
                val textTop = timeBaseline + timeBounds.top
                val textBottom = timeBaseline + timeBounds.bottom
                val shader = LinearGradient(
                    startX, textTop, rightEdge, textBottom,
                    config.colorHex, config.clockGradientEndColorHex,
                    Shader.TileMode.CLAMP
                )
                timePaint.shader = shader
                amPmPaint.shader = shader
            }

            canvas.drawText(timeStr, startX, timeBaseline, timePaint)
            if (amPmStr.isNotEmpty()) {
                canvas.drawText(amPmStr, amPmX, timeBaseline, amPmPaint)
            }

            val amPmBounds = Rect()
            if (amPmStr.isNotEmpty()) {
                amPmPaint.getTextBounds(amPmStr, 0, amPmStr.length, amPmBounds)
            }
            val glyphTop = if (amPmStr.isNotEmpty()) minOf(timeBounds.top, amPmBounds.top) else timeBounds.top
            val glyphBottom = if (amPmStr.isNotEmpty()) maxOf(timeBounds.bottom, amPmBounds.bottom) else timeBounds.bottom
            val typoPad = 1.5f * density
            return RectF(
                startX - typoPad,
                timeBaseline + glyphTop - typoPad,
                rightEdge + typoPad,
                timeBaseline + glyphBottom + typoPad
            )
        } else {
            val hourBounds = Rect()
            timePaint.getTextBounds(hourStr, 0, hourStr.length, hourBounds)
            val minuteBounds = Rect()
            timePaint.getTextBounds(minStr, 0, minStr.length, minuteBounds)

            val gap = clockTextSize * 0.10f
            val totalStackH = hourBounds.height() + gap + minuteBounds.height()
            val stackTopY = clockCenterY - (totalStackH / 2f)

            val hourBaseline = stackTopY - hourBounds.top
            val hourWidth = timePaint.measureText(hourStr)
            val hourStartX = clockCenterX - hourWidth / 2f

            val minWidth = timePaint.measureText(minStr)
            val minStartX = clockCenterX - minWidth / 2f

            val minuteStartY = stackTopY + hourBounds.height() + gap
            val minBaseline = minuteStartY - minuteBounds.top

            val amPmGap = if (amPmStr.isNotEmpty()) clockTextSize * 0.12f else 0f
            val amPmWidth = if (amPmStr.isNotEmpty()) amPmPaint.measureText(amPmStr) else 0f
            val amPmX = maxOf(minStartX + minWidth + amPmGap, hourStartX + hourWidth + (clockTextSize * 0.06f))

            val left = minOf(hourStartX, minStartX)
            val baseRight = maxOf(hourStartX + hourWidth, minStartX + minWidth)
            val right = if (amPmStr.isNotEmpty()) maxOf(baseRight, amPmX + amPmWidth) else baseRight

            if (config.clockGradientEnabled) {
                val shader = LinearGradient(
                    left, stackTopY, right, stackTopY + totalStackH,
                    config.colorHex, config.clockGradientEndColorHex,
                    Shader.TileMode.CLAMP
                )
                timePaint.shader = shader
                amPmPaint.shader = shader
            }

            canvas.drawText(hourStr, hourStartX, hourBaseline, timePaint)
            canvas.drawText(minStr, minStartX, minBaseline, timePaint)
            if (amPmStr.isNotEmpty()) {
                canvas.drawText(amPmStr, amPmX, minBaseline, amPmPaint)
            }

            val amPmBounds = Rect()
            if (amPmStr.isNotEmpty()) {
                amPmPaint.getTextBounds(amPmStr, 0, amPmStr.length, amPmBounds)
            }
            val typoPad = 1.5f * density
            val actualTop = stackTopY - typoPad
            val minBottom = if (amPmStr.isNotEmpty()) maxOf(minuteBounds.bottom, amPmBounds.bottom) else minuteBounds.bottom
            val actualBottom = maxOf(stackTopY + totalStackH, minBaseline + minBottom) + typoPad

            return RectF(
                left - typoPad,
                actualTop,
                right + typoPad,
                actualBottom
            )
        }
    }

    fun renderDate(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        config: ClockWidgetConfig,
        dateStr: String,
        dateTypeface: Typeface,
        density: Float
    ): RectF {
        if (!config.showDate || dateStr.isEmpty()) {
            return RectF(0f, 0f, 0f, 0f)
        }

        val targetCenterX = (widthPx / 2f) + (config.dateOffsetX * widthPx)
        val targetCenterY = (heightPx / 2f) + (config.dateOffsetY * heightPx)

        val baseDateSizePx = 14f * density
        val dateTextSize = (baseDateSizePx * config.dateScale).coerceIn(6f * density, 300f * density)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.dateColorHex
            textSize = dateTextSize
            typeface = dateTypeface
            textAlign = Paint.Align.CENTER
        }
        val dateBounds = Rect()
        subPaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)
        val dateWidth = subPaint.measureText(dateStr)
        val baselineY = targetCenterY - dateBounds.exactCenterY()

        val dateLeft = targetCenterX - (dateWidth / 2f)
        val dateTop = baselineY + dateBounds.top
        val dateRight = dateLeft + dateWidth
        val dateBottom = baselineY + dateBounds.bottom

        if (config.dateGradientEnabled) {
            val shader = LinearGradient(
                dateLeft, dateTop, dateRight, dateBottom,
                config.dateColorHex, config.dateGradientEndColorHex,
                Shader.TileMode.CLAMP
            )
            subPaint.shader = shader
        }

        canvas.drawText(dateStr, targetCenterX, baselineY, subPaint)

        val typoPad = 1.5f * density
        return RectF(
            dateLeft - typoPad,
            dateTop - typoPad,
            dateRight + typoPad,
            dateBottom + typoPad
        )
    }

    fun render(
        canvas: Canvas,
        widthPx: Float,
        heightPx: Float,
        config: ClockWidgetConfig,
        cal: Calendar,
        is24Hour: Boolean,
        removeLeadingZero: Boolean,
        clockTypeface: Typeface,
        appTypeface: Typeface,
        dateTypeface: Typeface = appTypeface,
        density: Float,
        amPmStr: String,
        dateStr: String
    ): ClockRenderOutput {
        val clockBounds = renderClock(
            canvas = canvas,
            widthPx = widthPx,
            heightPx = heightPx,
            config = config,
            cal = cal,
            is24Hour = is24Hour,
            removeLeadingZero = removeLeadingZero,
            clockTypeface = clockTypeface,
            appTypeface = appTypeface,
            density = density,
            amPmStr = amPmStr
        )
        val dateBounds = renderDate(
            canvas = canvas,
            widthPx = widthPx,
            heightPx = heightPx,
            config = config,
            dateStr = dateStr,
            dateTypeface = dateTypeface,
            density = density
        )
        return ClockRenderOutput(clockBounds, dateBounds)
    }
}
