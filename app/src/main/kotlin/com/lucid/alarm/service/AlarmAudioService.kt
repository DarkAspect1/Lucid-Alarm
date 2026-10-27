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
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.lucid.alarm.AlarmRingingActivity
import com.lucid.alarm.MainActivity
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

data class RingingAlarmState(
    val alarmId: Int,
    val label: String,
    val hour: Int,
    val minute: Int,
    val volumeLevel: Float,
    val ringtoneUri: String?,
    val vibrate: Boolean,
    val vibePreset: String,
    val snoozeCount: Int,
    val snoozeLimit: Int,
    val isAwakeCheckActive: Boolean,
    val forceMaxVolume: Boolean,
    val is24Hour: Boolean
)

class AlarmAudioService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousStreamVolume: Int? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val playbackLock = Any()
    private var timeoutJob: Job? = null

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
        initSilentAlarmChannels(this)
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

                stopCurrentPlaybackAndVibration()
                ringingState.value = null
                sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
                if (dismissedAlarmId >= 0) {
                    AlarmScheduler(this).cancelAllAuxiliaryTimers(dismissedAlarmId)
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            maybeStartAwakeCheckOnDismiss(dismissedAlarmId, wasAwakeCheck)
                        } finally {
                            withContext(Dispatchers.Main) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                        }
                    }
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_CANCEL_SNOOZE -> {
                val snoozedAlarmId = currentAlarmId
                val wasAwakeCheck = isAwakeCheckActive
                cancelSnooze()
                if (snoozedAlarmId >= 0) {
                    AlarmScheduler(this).cancelAllAuxiliaryTimers(snoozedAlarmId)
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            maybeStartAwakeCheckOnDismiss(snoozedAlarmId, wasAwakeCheck)
                        } finally {
                            withContext(Dispatchers.Main) {
                                stopSelf()
                            }
                        }
                    }
                } else {
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_SNOOZE_ALARM -> {
                if (currentSnoozeLimit in 1..Int.MAX_VALUE && currentSnoozeCount >= currentSnoozeLimit) {
                    return START_NOT_STICKY
                }

                cancelAllAwakeChecks(this)

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

                stopCurrentPlaybackAndVibration()
                ringingState.value = null
                sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))

                enterSnoozedState(snoozeMinutes)

                return START_NOT_STICKY
            }

            else -> {
                if (intent == null || intent.action != ACTION_START_ALARM) {
                    stopAlarmAndSelf()
                    return START_NOT_STICKY
                }

                val incomingAlarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)

                cancelAllAwakeChecks(this)
                cancelAllOtherSnoozes(incomingAlarmId)

                val ongoing = synchronized(playbackLock) { ringingState.value }

                if (ongoing != null && incomingAlarmId != -1 && incomingAlarmId == ongoing.alarmId && !isSnoozed) {
                    bringRingingActivityForward()
                    return START_NOT_STICKY
                }

                val replacingOngoingAlarm = ongoing != null && incomingAlarmId != -1 && incomingAlarmId != ongoing.alarmId

                if (replacingOngoingAlarm) {
                    stopCurrentPlaybackAndVibration()

                    val supersededAlarmId = ongoing.alarmId
                    val supersededLabel = ongoing.label
                    val supersededHour = ongoing.hour
                    val supersededMinute = ongoing.minute
                    val supersededIs24Hour = ongoing.is24Hour

                    AlarmScheduler(this).cancelAllAuxiliaryTimers(supersededAlarmId)

                    serviceScope.launch(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val alarmDao = db.alarmDao()
                        val alarm1 = alarmDao.getAlarmById(supersededAlarmId)
                        if (alarm1 != null) {
                            if (alarm1.daysOfWeek.isEmpty()) {
                                alarmDao.update(alarm1.copy(isEnabled = false))
                            } else {
                                val settings = SettingsDataStore(applicationContext)
                                val globalUpcoming = settings.upcomingAlarmEnabledFlow.first()
                                val lead = if (!alarm1.effectiveUpcomingNotification(globalUpcoming)) {
                                    0
                                } else if (alarm1.upcomingNotificationLeadMinutes > 0) {
                                    alarm1.upcomingNotificationLeadMinutes
                                } else {
                                    settings.rawUpcomingAlarmMinutesFlow.first()
                                }
                                AlarmScheduler(applicationContext).scheduleAlarm(alarm1, lead)
                            }
                        }
                        postMissedAlarmNotification(
                            applicationContext,
                            supersededAlarmId,
                            supersededLabel,
                            supersededHour,
                            supersededMinute,
                            supersededIs24Hour
                        )
                    }
                } else {
                    stopCurrentPlaybackAndVibration()
                }

                snoozeTickerJob?.cancel()
                snoozeTickerJob = null
                isSnoozed = false

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

                val newState = RingingAlarmState(
                    alarmId = currentAlarmId,
                    label = currentLabel,
                    hour = currentHour,
                    minute = currentMinute,
                    volumeLevel = currentVolumeLevel,
                    ringtoneUri = currentRingtoneUri,
                    vibrate = currentVibrate,
                    vibePreset = currentVibePreset,
                    snoozeCount = currentSnoozeCount,
                    snoozeLimit = currentSnoozeLimit,
                    isAwakeCheckActive = isAwakeCheckActive,
                    forceMaxVolume = forceMaxVolume,
                    is24Hour = currentIs24Hour
                )
                ringingState.value = newState

                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (wakeLock == null) {
                    wakeLock = powerManager?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "Lucid:AlarmAudioServiceWakeLock"
                    )?.apply {
                        setReferenceCounted(false)
                        acquire(15 * 60_000L)
                    }
                } else if (wakeLock?.isHeld != true) {
                    wakeLock?.acquire(15 * 60_000L)
                }

                initSilentAlarmChannels(this)
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

                timeoutJob?.cancel()
                timeoutJob = serviceScope.launch {
                    delay(UNANSWERED_TIMEOUT_MS)
                    handleUnansweredTimeout()
                }

                bringRingingActivityForward()
                refreshNotificationFromDatabase()
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelAllOtherSnoozes(incomingAlarmId: Int) {
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                val activeAlarms = AppDatabase.getDatabase(applicationContext).alarmDao().getActiveAlarmsSync()
                val scheduler = AlarmScheduler(applicationContext)
                for (alarm in activeAlarms) {
                    if (alarm.id != incomingAlarmId) {
                        scheduler.cancelSnooze(alarm.id)
                    }
                }
            }
        }
    }

    private fun stopCurrentPlaybackAndVibration() {
        synchronized(playbackLock) {
            timeoutJob?.cancel()
            timeoutJob = null

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
    }

    private fun handleUnansweredTimeout() {
        val unansweredAlarmId = currentAlarmId
        val unansweredLabel = currentLabel
        val unansweredHour = currentHour
        val unansweredMinute = currentMinute
        val unansweredVolumeLevel = currentVolumeLevel
        val unansweredRingtoneUri = currentRingtoneUri
        val unansweredVibrate = currentVibrate
        val unansweredVibePreset = currentVibePreset
        val unansweredSnoozeCount = currentSnoozeCount
        val unansweredSnoozeLimit = currentSnoozeLimit
        val unansweredIs24Hour = currentIs24Hour
        val wasAwakeCheck = isAwakeCheckActive

        stopCurrentPlaybackAndVibration()
        ringingState.value = null
        sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))

        if (unansweredAlarmId < 0) {
            stopAlarmAndSelf()
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val alarmDao = db.alarmDao()
            val alarm = alarmDao.getAlarmById(unansweredAlarmId)
            val settings = SettingsDataStore(applicationContext)
            val globalSnoozeMinutes = settings.ringingSnoozeMinutesFlow.first()
            val globalUpcoming = settings.upcomingAlarmEnabledFlow.first()
            val globalAwakeEnabled = settings.awakeCheckEnabledFlow.first()

            val snoozeEnabled = alarm?.snoozeEnabled ?: true
            val maxLimit = alarm?.snoozeLimit ?: unansweredSnoozeLimit
            val snoozeAllowed = snoozeEnabled && (maxLimit <= 0 || unansweredSnoozeCount < maxLimit)
            val awakeCheckEnabled = wasAwakeCheck || (alarm != null && alarm.effectiveAreYouAwake(globalAwakeEnabled))

            if (snoozeAllowed) {
                val configuredSnoozeMinutes = if (alarm != null && alarm.snoozeMinutes > 0) {
                    alarm.snoozeMinutes
                } else {
                    globalSnoozeMinutes
                }

                val nextSnoozeCount = unansweredSnoozeCount + 1
                AlarmScheduler(applicationContext).scheduleSnooze(
                    alarmId = unansweredAlarmId,
                    label = unansweredLabel,
                    hour = unansweredHour,
                    minute = unansweredMinute,
                    volumeLevel = unansweredVolumeLevel,
                    ringtoneUri = unansweredRingtoneUri,
                    vibrate = unansweredVibrate,
                    snoozeMinutes = configuredSnoozeMinutes,
                    vibrationPreset = unansweredVibePreset,
                    snoozeCount = nextSnoozeCount,
                    snoozeLimit = maxLimit
                )

                withContext(Dispatchers.Main) {
                    currentSnoozeCount = nextSnoozeCount
                    enterSnoozedState(configuredSnoozeMinutes)
                }
            } else if (awakeCheckEnabled) {
                AlarmScheduler(applicationContext).cancelAllAuxiliaryTimers(unansweredAlarmId)
                val checkDelayMinutes = if (alarm != null && alarm.hasCustomAreYouAwake && alarm.areYouAwakeMinutes > 0) {
                    alarm.areYouAwakeMinutes.coerceIn(1, 15)
                } else {
                    settings.awakeCheckMinutesFlow.first().coerceIn(1, 15)
                }
                scheduleSingleAwakeCheck(applicationContext, unansweredAlarmId, checkDelayMinutes)
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } else {
                if (alarm != null) {
                    if (alarm.daysOfWeek.isEmpty()) {
                        alarmDao.update(alarm.copy(isEnabled = false))
                    } else {
                        val lead = if (!alarm.effectiveUpcomingNotification(globalUpcoming)) {
                            0
                        } else if (alarm.upcomingNotificationLeadMinutes > 0) {
                            alarm.upcomingNotificationLeadMinutes
                        } else {
                            settings.rawUpcomingAlarmMinutesFlow.first()
                        }
                        AlarmScheduler(applicationContext).scheduleAlarm(alarm, lead)
                    }
                }
                postMissedAlarmNotification(
                    applicationContext,
                    unansweredAlarmId,
                    unansweredLabel,
                    unansweredHour,
                    unansweredMinute,
                    unansweredIs24Hour
                )
                withContext(Dispatchers.Main) {
                    stopAlarmAndSelf()
                }
            }
        }
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

        synchronized(playbackLock) {
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
        initSilentAlarmChannels(this)
        val title = if (label.isNotBlank()) label else "Alarm"
        val text = runCatching { formatNotificationTime(hour, minute, currentIs24Hour) }
            .getOrDefault("Alarm")
        return NotificationCompat.Builder(this, CHANNEL_RINGING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
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
        initSilentAlarmChannels(this)

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
            alarmId * 10 + AlarmScheduler.OFFSET_RING_SHOW,
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
            alarmId * 10 + 8,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId * 10 + 9,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = formatNotificationTime(hour, minute, currentIs24Hour)
        val title = if (label.isNotBlank()) label else "Alarm"

        val safeLargeIcon = runCatching {
            nowBar.largeIcon(NowBarNotification.Kind.ALARM, accentArgb, isDark)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, CHANNEL_RINGING)
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
            .setOnlyAlertOnce(true)
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
        initSilentAlarmChannels(this)
        val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val countdown = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val formattedSnoozeTargetTime = formatSnoozeTargetTime(snoozeTriggerAtMillis)
        val primary = countdown
        val secondary = "Rings at $formattedSnoozeTargetTime"

        val cancelIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = ACTION_CANCEL_SNOOZE
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            currentAlarmId * 10 + AlarmScheduler.OFFSET_SNOOZE_SHOW,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_SNOOZE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Snoozed • $countdown")
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
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        return formatNotificationTime(
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
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

    private fun cancelSnooze() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = null
        val alarmId = currentAlarmId
        if (alarmId != -1) {
            AlarmScheduler(this).cancelSnooze(alarmId)
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
        sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        isSnoozed = false
    }

    private suspend fun maybeStartAwakeCheckOnDismiss(
        alarmId: Int,
        wasAwakeCheck: Boolean = false
    ) {
        val storedAlarm = withContext(Dispatchers.IO) {
            runCatching {
                AppDatabase.getDatabase(applicationContext).alarmDao().getAlarmById(alarmId)
            }.getOrNull()
        }
        val settingsDataStore = SettingsDataStore(applicationContext)
        val globalEnabled = settingsDataStore.awakeCheckEnabledFlow.first()
        val shouldCheck = wasAwakeCheck || (storedAlarm != null && storedAlarm.effectiveAreYouAwake(globalEnabled))
        if (!shouldCheck) return

        val minutes = if (storedAlarm != null && storedAlarm.hasCustomAreYouAwake && storedAlarm.areYouAwakeMinutes > 0) {
            storedAlarm.areYouAwakeMinutes.coerceIn(1, 15)
        } else {
            settingsDataStore.awakeCheckMinutesFlow.first().coerceIn(1, 15)
        }

        scheduleSingleAwakeCheck(applicationContext, alarmId, minutes)
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
            putExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, isAwakeCheckActive)
        }
        try {
            startActivity(ringingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to bring ringing screen forward", e)
        }
    }

    private fun stopAlarmAndSelf() {
        stopCurrentPlaybackAndVibration()
        ringingState.value = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        sendBroadcast(Intent(ACTION_RINGING_DISMISSED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        snoozeTickerJob?.cancel()
        snoozeTickerJob = null
        stopCurrentPlaybackAndVibration()
        ringingState.value = null
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmAudioService"
        private const val MIN_VOLUME = 0.05f

        val ringingState = MutableStateFlow<RingingAlarmState?>(null)
        val isRinging: Boolean get() = ringingState.value != null

        val activeAwakeTimers = ConcurrentHashMap<Int, Long>()
        val pendingAwakeChecks = Collections.synchronizedList(mutableListOf<Int>())
        const val UNANSWERED_TIMEOUT_MS = 300_000L

        const val CHANNEL_RINGING = "lucid_alarm_ringing_v2"
        const val CHANNEL_SNOOZE = "lucid_alarm_snooze_v2"
        const val CHANNEL_AWAKE = "lucid_alarm_awake_v2"
        const val CHANNEL_UPCOMING = "lucid_alarm_upcoming_v2"

        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val MISSED_NOTIFICATION_BASE_ID = 40_000
        const val AWAKE_CHECK_NOTIFICATION_ID_BASE = 90_000

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

        fun initSilentAlarmChannels(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val oldChannels = listOf(
                "lucid_high_priority_alarm_channel",
                "lucid_high_priority_alarm_channel_v2",
                "lucid_alarm_channel",
                "lucid_snoozed_alarm_channel",
                "lucid_awake_check_channel",
                "lucid_upcoming_alarm_channel"
            )
            for (oldChannel in oldChannels) {
                runCatching { notificationManager.deleteNotificationChannel(oldChannel) }
            }

            if (notificationManager.getNotificationChannel(CHANNEL_RINGING) == null) {
                val ringingChannel = NotificationChannel(
                    CHANNEL_RINGING,
                    "Firing Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Silent channel for alarm screen and actions"
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(ringingChannel)
            }

            if (notificationManager.getNotificationChannel(CHANNEL_SNOOZE) == null) {
                val snoozeChannel = NotificationChannel(
                    CHANNEL_SNOOZE,
                    "Snoozed Alarms",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live countdown while an alarm is snoozed"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(snoozeChannel)
            }

            if (notificationManager.getNotificationChannel(CHANNEL_AWAKE) == null) {
                val awakeChannel = NotificationChannel(
                    CHANNEL_AWAKE,
                    "Awake Check",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Silent confirmation check after alarm stops"
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(awakeChannel)
            }

            if (notificationManager.getNotificationChannel(CHANNEL_UPCOMING) == null) {
                val upcomingChannel = NotificationChannel(
                    CHANNEL_UPCOMING,
                    "Upcoming Alarms",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Heads-up notification before an alarm fires"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(upcomingChannel)
            }
        }

        fun scheduleSingleAwakeCheck(context: Context, alarmId: Int, delayMinutes: Int) {
            cancelAllAwakeChecks(context)
            initSilentAlarmChannels(context)

            val confirmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_CONFIRM_AWAKE
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId * 10 + AlarmScheduler.OFFSET_AWAKE_TIMEOUT,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationText = if (delayMinutes == 1) {
                "Confirm within 1 minute or your alarm will ring again."
            } else {
                "Confirm within $delayMinutes minutes or your alarm will ring again."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_AWAKE)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Are you awake?")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(confirmPendingIntent)
                .addAction(
                    android.R.drawable.checkbox_on_background,
                    "I'm Awake",
                    confirmPendingIntent
                )
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(awakeCheckNotificationId(alarmId), notification)

            val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
            activeAwakeTimers[alarmId] = triggerAtMillis
            AlarmScheduler(context).scheduleAwakeCheckTimeout(alarmId, triggerAtMillis)
        }

        fun confirmAwake(context: Context, alarmId: Int) {
            activeAwakeTimers.remove(alarmId)
            pendingAwakeChecks.remove(alarmId)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(awakeCheckNotificationId(alarmId))

            AlarmScheduler(context).cancelAwakeTimeout(alarmId)
        }

        fun cancelAllAwakeChecks(context: Context) {
            val scheduler = AlarmScheduler(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val activeIds = activeAwakeTimers.keys.toList()
            for (alarmId in activeIds) {
                scheduler.cancelAwakeTimeout(alarmId)
                notificationManager.cancel(awakeCheckNotificationId(alarmId))
            }
            activeAwakeTimers.clear()
            pendingAwakeChecks.clear()
        }

        fun cancelAuxiliary(context: Context, alarmId: Int) {
            activeAwakeTimers.remove(alarmId)
            pendingAwakeChecks.remove(alarmId)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(awakeCheckNotificationId(alarmId))
        }

        fun removeAwakePromptNotification(context: Context, alarmId: Int) {
            activeAwakeTimers.remove(alarmId)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(awakeCheckNotificationId(alarmId))
        }

        fun postMissedAlarmNotification(
            context: Context,
            alarmId: Int,
            label: String,
            hour: Int,
            minute: Int,
            is24Hour: Boolean
        ) {
            initSilentAlarmChannels(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val formattedTime = formatNotificationTime(hour, minute, is24Hour)
            val contentText = if (label.isNotBlank()) "$label • $formattedTime" else "Missed alarm • $formattedTime"
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                MISSED_NOTIFICATION_BASE_ID + alarmId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, CHANNEL_UPCOMING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Missed alarm")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pi)
                .build()
            notificationManager.notify(MISSED_NOTIFICATION_BASE_ID + alarmId, notif)
        }

        fun awakeCheckNotificationId(alarmId: Int): Int =
            AWAKE_CHECK_NOTIFICATION_ID_BASE + alarmId
    }
}
