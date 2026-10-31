package com.lucid.alarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class, TrashedZone::class, TimerEntity::class],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    abstract fun zoneDao(): ZoneDao

    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN hasCustomAreYouAwake INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN hasCustomUpcomingNotification INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN customBackgroundInteractiveColors TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN customClockBoxEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN customClockBoxColor INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN customClockBoxOpacity REAL NOT NULL DEFAULT 0.35"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN weekInterval INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN anchorWeekStartEpochDay INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN skipUntilEpochMillis INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, `remainingMillis` INTEGER NOT NULL, `endTimestamp` INTEGER NOT NULL DEFAULT 0, `isRunning` INTEGER NOT NULL DEFAULT 0, `isPaused` INTEGER NOT NULL DEFAULT 0, `isDeleted` INTEGER NOT NULL DEFAULT 0, `deletedAt` INTEGER)"
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN volume REAL NOT NULL DEFAULT 1.0"
                )
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN vibrate INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE timers ADD COLUMN vibrationPattern TEXT NOT NULL DEFAULT 'TICK_TICK'"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lucid_alarm_database"
                )
                    .addMigrations(
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
