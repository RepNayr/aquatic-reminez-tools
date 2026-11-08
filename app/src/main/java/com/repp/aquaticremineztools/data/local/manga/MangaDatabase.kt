package com.repp.aquaticremineztools.data.local.manga

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MangaDownloadEntity::class], version = 4, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {

    abstract fun mangaDownloadDao(): MangaDownloadDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN lastHeartbeatAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS index_manga_downloads_chapterUrl")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_downloads_chapterUrl ON manga_downloads(chapterUrl)")
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN lastHeartbeatAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS index_manga_downloads_chapterUrl")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_downloads_chapterUrl ON manga_downloads(chapterUrl)")
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN lastHeartbeatAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS index_manga_downloads_chapterUrl")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_downloads_chapterUrl ON manga_downloads(chapterUrl)")
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
                .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
