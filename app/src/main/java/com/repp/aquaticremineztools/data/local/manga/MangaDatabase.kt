package com.repp.aquaticremineztools.data.local.manga

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MangaDownloadEntity::class], version = 1, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {

    abstract fun mangaDownloadDao(): MangaDownloadDao

    companion object {
        @Volatile
        private var INSTANCE: MangaDatabase? = null

        fun getInstance(context: Context): MangaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MangaDatabase::class.java,
                    "aquatic_manga_db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
