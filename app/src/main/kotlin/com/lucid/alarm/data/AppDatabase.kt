package com.lucid.alarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class, TrashedZone::class],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    abstract fun zoneDao(): ZoneDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lucid_alarm_database"
                )
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
