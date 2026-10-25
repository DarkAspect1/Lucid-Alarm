package com.lucid.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.res.ResourcesCompat
import com.lucid.alarm.MainActivity
import com.lucid.alarm.R
import com.lucid.alarm.receiver.NowBarActionReceiver

/**
 * Builds the ongoing "Now Bar" notification for the Timer and the Stopwatch.
 *
 * One UI's Live Notification / Now Bar renderer ignores buttons placed inside a custom
 * `RemoteViews` card and reads standard [NotificationCompat.Action] items instead. The notification
 * is therefore a plain system notification: title, text, small icon, an accent-tinted squircle
 * large icon, two text actions and the `android.ongoingActivityNoti.*` extras that promote it to
 * Samsung's Ongoing Activity chip.
 *
 * It deliberately contains **no** `MediaSession`, no `MediaStyle` and no transport controls, so
 * neither Android nor One UI can mistake a countdown for a music/video playback.
 */
class NowBarNotification(private val context: Context) {

    /** Which activity the Now Bar is currently rendering. */
    enum class Kind { TIMER, STOPWATCH, ALARM }

    private val density = context.resources.displayMetrics.density

    private var cachedIcon: Bitmap? = null
    private var cachedIconKey: String? = null

    /** Creates the low-importance channel the ongoing notification lives on. */
    fun ensureChannel(channelId: String, name: String, description: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW).apply {
                this.description = description
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /**
     * Builds the event squircle badge with a soft drop shadow, accent color background,
     * and theme-adaptive glyph (white in dark mode, black/dark slate in light mode).
     *
     * Used for both the notification large icon and Samsung's `ongoingActivityNoti.chipIcon`
     * so the Now Bar displays the event icon instead of falling back to the app logo.
     */
    fun largeIcon(kind: Kind, accentColor: Int, isDark: Boolean = true): Bitmap {
        val badgeSize = BADGE_SIZE_DP * density
        val shadowMargin = 4f * density
        val totalSize = (badgeSize + shadowMargin * 2f).toInt().coerceAtLeast(1)
        val key = "$kind:$accentColor:$totalSize:$isDark"
        cachedIcon?.let { if (cachedIconKey == key) return it }

        val bitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = BADGE_CORNER_RADIUS_DP * density
        val squircleBounds = RectF(
            shadowMargin,
            shadowMargin - 1f * density,
            shadowMargin + badgeSize,
            shadowMargin + badgeSize - 1f * density
        )

        // Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            maskFilter = BlurMaskFilter(2.5f * density, BlurMaskFilter.Blur.NORMAL)
        }
        val shadowBounds = RectF(
            squircleBounds.left,
            squircleBounds.top + 2f * density,
            squircleBounds.right,
            squircleBounds.bottom + 2f * density
        )
        canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)

        // Accent squircle background with subtle vertical sheen
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            squircleBounds.left,
            squircleBounds.top,
            squircleBounds.left,
            squircleBounds.bottom,
            intArrayOf(blend(accentColor, Color.WHITE, 0.08f), blend(accentColor, Color.BLACK, 0.08f)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(squircleBounds, radius, radius, paint)

        // Event-specific glyph
        val glyphRes = when (kind) {
            Kind.STOPWATCH -> R.drawable.ic_stopwatch
            Kind.ALARM -> R.drawable.ic_alarm
            Kind.TIMER -> R.drawable.ic_hourglass
        }
        ResourcesCompat.getDrawable(context.resources, glyphRes, null)?.let { glyph ->
            val iconColor = if (isDark) Color.WHITE else 0xFF1E2024.toInt()
            glyph.setTint(iconColor)
            val glyphSize = (squircleBounds.width() * GLYPH_SIZE_FRACTION).toInt()
            val insetX = (squircleBounds.left + (squircleBounds.width() - glyphSize) / 2f).toInt()
            val insetY = (squircleBounds.top + (squircleBounds.height() - glyphSize) / 2f).toInt()
            glyph.setBounds(insetX, insetY, insetX + glyphSize, insetY + glyphSize)
            glyph.draw(canvas)
        }
        cachedIcon = bitmap
        cachedIconKey = key
        return bitmap
    }

    /** Pending intent that opens the app directly on [initialTab] (Timer or Stopwatch). */
    fun openAppPendingIntent(initialTab: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_INITIAL_TAB, initialTab)
        }
        val requestCode = if (initialTab == INITIAL_TAB_STOPWATCH) {
            OPEN_STOPWATCH_REQUEST
        } else {
            OPEN_TIMER_REQUEST
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Pending intent that drives one of the Now Bar actions.
     *
     * The action is delivered to [NowBarActionReceiver], which starts the owning foreground service
     * from the notification action context (an allowlisted foreground start) instead of launching
     * `MainActivity`.
     */
    fun actionPendingIntent(
        action: String,
        targetService: String,
        requestCode: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, NowBarActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TARGET_SERVICE, targetService)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun blend(color: Int, towards: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(color) + (Color.red(towards) - Color.red(color)) * f).toInt()
        val g = (Color.green(color) + (Color.green(towards) - Color.green(color)) * f).toInt()
        val b = (Color.blue(color) + (Color.blue(towards) - Color.blue(color)) * f).toInt()
        return Color.argb(Color.alpha(color), r, g, b)
    }

    companion object {
        /** Extra naming the tab to open when the notification body is tapped. */
        const val EXTRA_INITIAL_TAB = "INITIAL_TAB"

        /** Values for [EXTRA_INITIAL_TAB]. */
        const val INITIAL_TAB_TIMER = "TIMER"
        const val INITIAL_TAB_STOPWATCH = "STOPWATCH"

        /** Extra naming the service that must receive an action broadcast. */
        const val EXTRA_TARGET_SERVICE = "com.lucid.alarm.nowbar.TARGET_SERVICE"

        /** Values for [EXTRA_TARGET_SERVICE]. */
        const val TARGET_TIMER = "timer"
        const val TARGET_STOPWATCH = "stopwatch"

        /** Request code of the first split action (left) of the Timer notification. */
        const val LEFT_TIMER_REQUEST = 9102

        /** Request code of the second split action (right) of the Timer notification. */
        const val RIGHT_TIMER_REQUEST = 9103

        /** Request code of the first split action (left) of the Stopwatch notification. */
        const val LEFT_STOPWATCH_REQUEST = 9106

        /** Request code of the second split action (right) of the Stopwatch notification. */
        const val RIGHT_STOPWATCH_REQUEST = 9107

        private const val OPEN_TIMER_REQUEST = 9104
        private const val OPEN_STOPWATCH_REQUEST = 9105

        private const val BADGE_CORNER_RADIUS_DP = 18f
        private const val BADGE_SIZE_DP = 54f
        private const val GLYPH_SIZE_FRACTION = 0.56f
    }
}
