package com.lucid.alarm

import android.app.Application
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.data.TimerRepository
import com.lucid.alarm.repository.AlarmRepository
import com.lucid.alarm.repository.WorldClockRepository
import com.lucid.alarm.service.AlarmAudioService

class AlarmApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao()) }
    val worldClockRepository by lazy { WorldClockRepository(database.zoneDao()) }
    val timerRepository by lazy { TimerRepository(database.timerDao()) }

    override fun onCreate() {
        super.onCreate()
        AlarmAudioService.initSilentAlarmChannels(this)
    }
}
