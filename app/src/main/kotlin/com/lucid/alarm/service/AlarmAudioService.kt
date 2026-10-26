package com.lucid.alarm.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.lucid.alarm.AlarmRingingActivity
import com.lucid.alarm.R
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.data.prefs.AlarmVibrationPreset
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.VolumePolicy
import com.lucid.alarm.receiver.AlarmReceiver
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.ui.formatNotificationTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class AlarmAudioService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousStreamVolume: Int? = null

    private var currentAlarmId: Int = -1
    private var currentLabel: String = ""
    private var currentHour: Int = -1
    private var currentMinute: Int = -1
    private var currentVolumeLevel: Float = 1.0f
    private var currentRingtoneUri: String? = null
    private var currentIs24Hour = true
    private var currentVibrate: Boolean = true
    private var currentVibePreset: String = "HEARTBEAT"
    private var currentSnoozeCount: Int = 0
    private var currentSnoozeLimit: Int = -1
    private var forceMaxVolume: Boolean = false
    private var isAwakeCheckActive: Boolean = false

    private val nowBar: NowBarNotification by lazy { NowBarNotification(this) }

    private var accentArgb: Int = 0xFFF7B500.toInt()
    private var isDark: Boolean = true

    private var snoozeTickerJob: Job? = null

    private var snoozeTriggerAtMillis: Long = 0L

    private var isSnoozed: Boolean = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        ensureAlarmNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> {
                val dismissedAlarmId = intent.getIntExtra(
                    AlarmReceiver.EXTRA_ALARM_ID,
                    currentAlarmId
                )
                val wasAwakeCheck = intent.getBooleanExtra(
                    AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE,
                    false
                ) || isAwakeCheckActive

                stopOngoingPlayback()
                sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
                if (dismissedAlarmId >= 0) {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        maybePostAwakeCheck(dismissedAlarmId, wasAwakeCheck)
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_CANCEL_SNOOZE -> {

                cancelSnooze()
                return START_NOT_STICKY
            }

            ACTION_SNOOZE_ALARM -> {

                if (currentSnoozeLimit >= 0 && currentSnoozeCount >= currentSnoozeLimit) {
                    return START_NOT_STICKY
                }

                val snoozeMinutes = intent?.getIntExtra(
                    AlarmReceiver.EXTRA_SNOOZE_MINUTES,
                    SettingsDataStore.DEFAULT_SNOOZE_MINUTES
                ) ?: SettingsDataStore.DEFAULT_SNOOZE_MINUTES
                AlarmScheduler(this).scheduleSnooze(
                    alarmId = currentAlarmId,
                    label = currentLabel,
                    hour = currentHour,
                    minute = currentMinute,
                    volumeLevel = currentVolumeLevel,
                    ringtoneUri = currentRingtoneUri,
                    vibrate = currentVibrate,
                    snoozeMinutes = snoozeMinutes,
                    vibrationPreset = currentVibePreset,
                    snoozeCount = currentSnoozeCount + 1,
                    snoozeLimit = currentSnoozeLimit
                )

                stopOngoingPlayback()
                ensureSnoozeNotificationChannel()
                enterSnoozedState(snoozeMinutes)
                return START_NOT_STICKY
            }

            else -> {
                if (intent == null || intent.action != ACTION_START_ALARM) {

                    stopAlarmAndSelf()
                    return START_NOT_STICKY
                }

                val incomingAlarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)

                snoozeTickerJob?.cancel()
                snoozeTickerJob = null
                isSnoozed = false

                val replacingOngoingAlarm =
                    currentAlarmId != -1 && incomingAlarmId != -1 && incomingAlarmId != currentAlarmId
                if (replacingOngoingAlarm) {
                    stopOngoingPlayback()
                }

                currentAlarmId = incomingAlarmId
                currentLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL).orEmpty()
                currentRingtoneUri = intent.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
                    ?.takeIf { it.isNotBlank() }
                currentIs24Hour = intent.getBooleanExtra(
                    AlarmReceiver.EXTRA_ALARM_IS_24_HOUR,
                    DateFormat.is24HourFormat(this)
                )
                val nowCal = Calendar.getInstance()

                currentHour = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, -1)
                    .takeIf { it in 0..23 } ?: nowCal.get(Calendar.HOUR_OF_DAY)
                currentMinute = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, -1)
                    .takeIf { it in 0..59 } ?: nowCal.get(Calendar.MINUTE)
                currentVolumeLevel =
                    intent.getFloatExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, 1.0f)
                currentVibePreset =
                    intent.getStringExtra(AlarmReceiver.EXTRA_VIBE_PRESET) ?: "HEARTBEAT"
                currentSnoozeCount = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0)
                currentSnoozeLimit = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, -1)
                currentVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)
                forceMaxVolume =
                    intent.getBooleanExtra(AlarmReceiver.EXTRA_FORCE_MAX_VOLUME, false)
                isAwakeCheckActive =
                    intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, false)

                val richInitialNotification = runCatching {
                    buildRingingNotification(currentAlarmId, currentLabel, currentHour, currentMinute)
                }.getOrElse {
                    buildSafeInitialNotification(currentLabel, currentHour, currentMinute)
                }

                try {
                    startForeground(
                        FOREGROUND_NOTIFICATION_ID,
                        richInitialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to enter foreground; aborting ringing", e)
                    stopAlarmAndSelf()
                    return START_NOT_STICKY
                }

                serviceScope.launch { startAudioAndVibration() }
                if (replacingOngoingAlarm) {

                    bringRingingActivityForward()
                }
                refreshNotificationFromDatabase()
            }
        }
        return START_NOT_STICKY
    }

    private fun refreshNotificationFromDatabase() {
        val alarmId = currentAlarmId
        if (alarmId == -1) return
        serviceScope.launch {

            val loadedPrefs = runCatching {
                SettingsDataStore(applicationContext).currentNowBarPreferences()
            }.getOrNull()
            val loadedAccent = loadedPrefs?.accentArgb
            if (loadedPrefs != null) {
                accentArgb = loadedPrefs.accentArgb
                isDark = loadedPrefs.isDark
            }

            val stored = runCatching {
                AppDatabase.getDatabase(applicationContext).alarmDao().getAlarmById(alarmId)
            }.getOrNull()
            val storedFormat = runCatching {
                SettingsDataStore(applicationContext).timeFormatFlow.first()
            }.getOrNull()
            val is24Hour = storedFormat?.is24Hour ?: currentIs24Hour
            val formatChanged = is24Hour != currentIs24Hour
            if (stored != null) {
                currentHour = stored.hour
                currentMinute = stored.minute
                if (stored.label.isNotBlank()) currentLabel = stored.label
            }
            currentIs24Hour = is24Hour

            if (isSnoozed) {
                if (loadedAccent != null) {
                    runCatching {
                        val remaining = snoozeTriggerAtMillis - System.currentTimeMillis()
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(
                            FOREGROUND_NOTIFICATION_ID,
                            buildSnoozedNotification(remaining)
                        )
                    }.onFailure {
                        Log.w(TAG, "Unable to re-tint snoozed notification with accent", it)
                    }
                }
                return@launch
            }

            if (loadedAccent == null && stored == null && !formatChanged) return@launch
            refreshRingingNotification()
        }
    }

    private suspend fun startAudioAndVibration() {
        val settingsDataStore = SettingsDataStore(applicationContext)

        val volumePolicy = runCatching { settingsDataStore.volumePolicyFlow.first() }
            .getOrDefault(VolumePolicy.PER_ALARM)
        val unifiedVolume = runCatching { settingsDataStore.unifiedVolumeFlow.first() }
            .getOrDefault(1.0f)

        val targetNormalizedVolume = when {
            forceMaxVolume -> 1.0f
            volumePolicy == VolumePolicy.UNIFIED -> unifiedVolume
            else -> currentVolumeLevel
        }.coerceIn(MIN_VOLUME, 1.0f)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioManager?.let { am ->
            val maxStreamVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            previousStreamVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)

            val targetStreamVolume = if (forceMaxVolume) {
                maxStreamVolume
            } else {
                (maxStreamVolume * targetNormalizedVolume)
                    .roundToInt()
                    .coerceIn(1, maxStreamVolume)
            }
            try {
                am.setStreamVolume(AudioManager.STREAM_ALARM, targetStreamVolume, 0)
            } catch (se: SecurityException) {
                Log.w(TAG, "Unable to change STREAM_ALARM volume due to DND policy", se)
            }

            val focusReq = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            audioFocusRequest = focusReq
            am.requestAudioFocus(focusReq)
        }

        val globalRingtoneUri = runCatching {
            settingsDataStore.defaultAlarmRingtoneUriFlow.first()
        }.getOrDefault("")
        val resolvedUri: Uri = currentRingtoneUri
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
            ?: globalRingtoneUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                try {
                    setDataSource(applicationContext, resolvedUri)
                } catch (e: Exception) {
                    val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setDataSource(applicationContext, fallback)
                }
                isLooping = true
                setVolume(targetNormalizedVolume, targetNormalizedVolume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm audio", e)
        }

        if (currentVibrate) {
            startAlarmVibration()
        }
    }

    private fun startAlarmVibration() {
        val preset = AlarmVibrationPreset.fromName(currentVibePreset)
        val effect = VibrationEffect.createWaveform(
            preset.waveform,
            if (preset.repeat) 0 else -1
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val vibrationAttrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator?.vibrate(effect, vibrationAttrs)
        } else {
            @Suppress("DEPRECATION")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator?.vibrate(effect, audioAttrs)
        }
    }

    private fun buildSafeInitialNotification(
        label: String,
        hour: Int,
        minute: Int
    ): Notification {
        ensureAlarmNotificationChannel()
        val title = if (label.isNotBlank()) label else "Alarm"
        val text = runCatching { formatNotificationTime(hour, minute, currentIs24Hour) }
            .getOrDefault("Alarm")
        return NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun refreshRingingNotification() {
        if (isSnoozed) return
        val notification = runCatching {
            buildRingingNotification(currentAlarmId, currentLabel, currentHour, currentMinute)
        }.getOrElse {
            buildSafeInitialNotification(currentLabel, currentHour, currentMinute)
        }
        runCatching {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }.onFailure {
            Log.w(TAG, "Unable to refresh ringing foreground notification", it)
        }
    }

    private fun buildRingingNotification(
        alarmId: Int,
        label: String,
        hour: Int,
        minute: Int
    ): Notification {
        ensureAlarmNotificationChannel()

        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, currentVolumeLevel)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, currentRingtoneUri)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, currentVibrate)
            putExtra(AlarmReceiver.EXTRA_VIBE_PRESET, currentVibePreset)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, currentSnoozeCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, currentSnoozeLimit)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId + 10_000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, isAwakeCheckActive)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            alarmId + 20_000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId + 30_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = formatNotificationTime(hour, minute, currentIs24Hour)
        val title = if (label.isNotBlank()) label else "Alarm"

        val safeLargeIcon = runCatching {
            nowBar.largeIcon(NowBarNotification.Kind.ALARM, accentArgb, isDark)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(formattedTime)

            .also { if (safeLargeIcon != null) it.setLargeIcon(safeLargeIcon) }
            .setColor(accentArgb)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                buildNowBarAction(
                    R.drawable.ic_action_snooze,
                    "Snooze",
                    snoozePendingIntent
                )
            )
            .addAction(
                buildNowBarAction(
                    R.drawable.ic_action_close,
                    "Dismiss",
                    stopPendingIntent
                )
            )

            .setDeleteIntent(snoozePendingIntent)

        runCatching { builder.addExtras(buildNowBarExtras(formattedTime)) }
            .onFailure { Log.w(TAG, "Unable to attach Now Bar extras to ringing notification", it) }
        return builder.build()
    }

    private fun buildNowBarAction(
        iconRes: Int,
        title: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action {
        val safeIcon = runCatching {
            IconCompat.createWithResource(this, iconRes)
        }.getOrNull()
        val actionBuilder = if (safeIcon != null) {
            NotificationCompat.Action.Builder(safeIcon, title, pendingIntent)
        } else {

            @Suppress("DEPRECATION")
            NotificationCompat.Action.Builder(android.R.drawable.ic_lock_idle_alarm, title, pendingIntent)
        }
        return actionBuilder
            .setShowsUserInterface(false)
            .build()
    }

    private fun buildNowBarExtras(formattedTime: String): Bundle = Bundle().apply {
        putBoolean(NOWBAR_REQUEST_PROMOTED_ONGOING, true)
        putString(NOWBAR_SHORT_CRITICAL_TEXT, formattedTime)
        putInt(NOWBAR_STYLE, 1)
        putString(NOWBAR_PRIMARY_INFO, formattedTime)
        putString(NOWBAR_SECONDARY_INFO, "Ringing")
        putString(NOWBAR_NOWBAR_PRIMARY_INFO, formattedTime)
        putString(NOWBAR_NOWBAR_SECONDARY_INFO, "Ringing")
        putInt(NOWBAR_CHIP_BG_COLOR, accentArgb)
        runCatching {
            Icon.createWithBitmap(nowBar.largeIcon(NowBarNotification.Kind.ALARM, accentArgb, isDark))
        }.getOrNull()?.let { putParcelable(NOWBAR_CHIP_ICON, it) }
        putString(NOWBAR_CHIP_EXPANDED_TEXT, formattedTime)

        putInt(NOWBAR_ACTION_TYPE, 1)
        putInt(NOWBAR_ACTION_PRIMARY_SET, 0)
    }

    private fun enterSnoozedState(snoozeMinutes: Int) {
        isSnoozed = true
        snoozeTriggerAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val notification = buildSnoozedNotification(snoozeMinutes)
        try {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to enter snoozed foreground state", e)
            stopAlarmAndSelf()
            return
        }
        startSnoozeTicker()
    }

    private fun startSnoozeTicker() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = serviceScope.launch {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            while (isActive) {
                val remaining = snoozeTriggerAtMillis - System.currentTimeMillis()
                if (remaining <= 0L) break
                notificationManager.notify(
                    FOREGROUND_NOTIFICATION_ID,
                    buildSnoozedNotification(remaining)
                )
                delay(1_000L)
            }
        }
    }

    private fun buildSnoozedNotification(snoozeMinutes: Int): Notification =
        buildSnoozedNotification(snoozeMinutes * 60_000L)

    private fun buildSnoozedNotification(remainingMillis: Long): Notification {
        ensureAlarmNotificationChannel()
        val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val countdown = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val formattedSnoozeTargetTime = formatSnoozeTargetTime(snoozeTriggerAtMillis)
        val primary = countdown
        val secondary = "Snoozed until $formattedSnoozeTargetTime"

        val cancelIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = ACTION_CANCEL_SNOOZE
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            currentAlarmId + 35_000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SNOOZE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(countdown)
            .setContentText(secondary)
            .also { b ->
                runCatching {
                    nowBar.largeIcon(NowBarNotification.Kind.ALARM, accentArgb, isDark)
                }.getOrNull()?.let { icon -> b.setLargeIcon(icon) }
            }
            .setColor(accentArgb)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                buildNowBarAction(
                    R.drawable.ic_action_close,
                    "Dismiss",
                    cancelPendingIntent
                )
            )

        runCatching { builder.addExtras(buildSnoozeExtras(primary, secondary)) }
        return builder.build()
    }

    private fun formatSnoozeTargetTime(triggerAtMillis: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        return formatNotificationTime(
            hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            minute = cal.get(java.util.Calendar.MINUTE),
            is24Hour = currentIs24Hour,
            removeLeadingZero = true
        )
    }

    private fun buildSnoozeExtras(primary: String, secondary: String): Bundle = Bundle().apply {
        putBoolean(NOWBAR_REQUEST_PROMOTED_ONGOING, true)
        putString(NOWBAR_SHORT_CRITICAL_TEXT, primary)
        putInt(NOWBAR_STYLE, 1)
        putString(NOWBAR_PRIMARY_INFO, primary)
        putString(NOWBAR_SECONDARY_INFO, secondary)
        putString(NOWBAR_NOWBAR_PRIMARY_INFO, primary)
        putString(NOWBAR_NOWBAR_SECONDARY_INFO, secondary)
        putInt(NOWBAR_CHIP_BG_COLOR, accentArgb)
        runCatching {
            Icon.createWithBitmap(nowBar.largeIcon(NowBarNotification.Kind.ALARM, accentArgb, isDark))
        }.getOrNull()?.let { putParcelable(NOWBAR_CHIP_ICON, it) }
        putString(NOWBAR_CHIP_EXPANDED_TEXT, primary)
        putInt(NOWBAR_ACTION_TYPE, 1)
        putInt(NOWBAR_ACTION_PRIMARY_SET, 0)
    }

    private fun ensureSnoozeNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(SNOOZE_CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SNOOZE_CHANNEL_ID,
                "Snoozed Alarms",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live countdown while an alarm is snoozed"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun cancelSnooze() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = null
        val alarmId = currentAlarmId
        if (alarmId != -1) {
            val cancelIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                AlarmScheduler.SNOOZE_REQUEST_BASE + alarmId,
                cancelIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
        sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        isSnoozed = false
        stopSelf()
    }

    private suspend fun maybePostAwakeCheck(alarmId: Int, isAwakeCheckActive: Boolean = false) {
        val storedAlarm = withContext(Dispatchers.IO) {
            runCatching {
                AppDatabase.getDatabase(applicationContext).alarmDao().getAlarmById(alarmId)
            }.getOrNull()
        }
        val settingsDataStore = SettingsDataStore(applicationContext)
        val globalEnabled = settingsDataStore.awakeCheckEnabledFlow.first()
        val shouldCheck = isAwakeCheckActive || (storedAlarm != null && storedAlarm.effectiveAreYouAwake(globalEnabled))
        if (!shouldCheck) return
        val minutes = if (storedAlarm != null && storedAlarm.areYouAwakeMinutes > 0) {
            storedAlarm.areYouAwakeMinutes.coerceIn(1, 15)
        } else {
            settingsDataStore.awakeCheckMinutesFlow.first()
        }
        postAwakeCheckNotification(alarmId, minutes)
        scheduleAwakeCheckTimeout(alarmId, minutes)
    }

    private fun postAwakeCheckNotification(alarmId: Int, minutes: Int) {
        ensureAlarmNotificationChannel()
        val notificationId = awakeCheckNotificationId(alarmId)
        val confirmIntent = PendingIntent.getBroadcast(
            this,
            AWAKE_CONFIRM_REQUEST_BASE + alarmId,
            Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_CONFIRM_AWAKE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Are you awake?")
            .setContentText("Confirm within $minutes min or the alarm rings again at full volume.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(confirmIntent)
            .addAction(android.R.drawable.checkbox_on_background, "I'm awake", confirmIntent)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun scheduleAwakeCheckTimeout(alarmId: Int, minutes: Int) {
        val timeoutIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_AWAKE_CHECK_TIMEOUT
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            AWAKE_TIMEOUT_REQUEST_BASE + alarmId,
            timeoutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact awake-check alarm denied; using inexact fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Unable to schedule awake-check timeout", e)
        }
    }

    private fun ensureAlarmNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching {
            notificationManager.deleteNotificationChannel("lucid_high_priority_alarm_channel")
        }
        if (notificationManager.getNotificationChannel(ALARM_CHANNEL_ID) != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Firing Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical channel for firing alarms and lock-screen wakeups"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setSound(alarmSound, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopOngoingPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        vibrator?.cancel()

        audioFocusRequest?.let { req ->
            audioManager?.abandonAudioFocusRequest(req)
        }
        audioFocusRequest = null

        previousStreamVolume?.let { prevVol ->
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, prevVol, 0)
            } catch (_: SecurityException) {
            }
        }
        previousStreamVolume = null
    }

    private fun bringRingingActivityForward() {
        val alarmId = currentAlarmId
        if (alarmId == -1) return
        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentLabel)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, currentHour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, currentMinute)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, currentVolumeLevel)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, currentRingtoneUri)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, currentVibrate)
            putExtra(AlarmReceiver.EXTRA_VIBE_PRESET, currentVibePreset)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, currentSnoozeCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, currentSnoozeLimit)
        }
        try {
            startActivity(ringingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to bring ringing screen forward", e)
        }
    }

    private fun stopAlarmAndSelf() {        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        vibrator?.cancel()

        audioFocusRequest?.let { req ->
            audioManager?.abandonAudioFocusRequest(req)
        }
        audioFocusRequest = null

        previousStreamVolume?.let { prevVol ->
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, prevVol, 0)
            } catch (_: SecurityException) {
            }
        }
        previousStreamVolume = null

        sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = null
        serviceScope.cancel()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        vibrator?.cancel()
        audioFocusRequest?.let { req ->
            audioManager?.abandonAudioFocusRequest(req)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmAudioService"
        private const val MIN_VOLUME = 0.05f

        const val ALARM_CHANNEL_ID = "lucid_high_priority_alarm_channel_v2"
        const val SNOOZE_CHANNEL_ID = "lucid_snoozed_alarm_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        const val ACTION_START_ALARM = "com.lucid.alarm.service.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.lucid.alarm.service.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.lucid.alarm.service.ACTION_SNOOZE_ALARM"
        const val ACTION_CANCEL_SNOOZE = "com.lucid.alarm.service.ACTION_CANCEL_SNOOZE"
        const val ACTION_RINGING_DISMISSED = "com.lucid.alarm.ACTION_RINGING_DISMISSED"

        private const val NOWBAR_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private const val NOWBAR_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
        private const val NOWBAR_STYLE = "android.ongoingActivityNoti.style"
        private const val NOWBAR_PRIMARY_INFO = "android.ongoingActivityNoti.primaryInfo"
        private const val NOWBAR_SECONDARY_INFO = "android.ongoingActivityNoti.secondaryInfo"
        private const val NOWBAR_NOWBAR_PRIMARY_INFO = "android.ongoingActivityNoti.nowbarPrimaryInfo"
        private const val NOWBAR_NOWBAR_SECONDARY_INFO =
            "android.ongoingActivityNoti.nowbarSecondaryInfo"
        private const val NOWBAR_CHIP_BG_COLOR = "android.ongoingActivityNoti.chipBgColor"
        private const val NOWBAR_CHIP_ICON = "android.ongoingActivityNoti.chipIcon"
        private const val NOWBAR_CHIP_EXPANDED_TEXT = "android.ongoingActivityNoti.chipExpandedText"
        private const val NOWBAR_ACTION_TYPE = "android.ongoingActivityNoti.actionType"
        private const val NOWBAR_ACTION_PRIMARY_SET = "android.ongoingActivityNoti.actionPrimarySet"

        const val AWAKE_CHECK_NOTIFICATION_ID_BASE = 90_000
        private const val AWAKE_CONFIRM_REQUEST_BASE = 40_000
        const val AWAKE_TIMEOUT_REQUEST_BASE = 50_000

        fun awakeCheckNotificationId(alarmId: Int): Int =
            AWAKE_CHECK_NOTIFICATION_ID_BASE + alarmId
    }
}
