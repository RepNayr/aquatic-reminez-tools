package com.repp.aquaticremineztools.data.local.manga

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MangaDownloadEntity::class], version = 2, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {

    abstract fun mangaDownloadDao(): MangaDownloadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_manga_downloads_chapterUrl ON manga_downloads(chapterUrl)")
            }
        }

        @Volatile
        private var INSTANCE: MangaDatabase? = null

        fun getInstance(context: Context): MangaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MangaDatabase::class.java,
                    "aquatic_manga_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
