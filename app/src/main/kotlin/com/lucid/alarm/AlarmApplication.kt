package com.lucid.alarm

import android.app.Application
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.repository.AlarmRepository
import com.lucid.alarm.repository.WorldClockRepository

class AlarmApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao()) }
    val worldClockRepository by lazy { WorldClockRepository(database.zoneDao()) }
}
