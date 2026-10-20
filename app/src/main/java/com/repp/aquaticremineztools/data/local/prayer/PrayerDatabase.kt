package com.repp.aquaticremineztools.data.local.prayer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PrayerEntity::class], version = 1, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {

    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        fun getInstance(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "aquatic_prayer_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
