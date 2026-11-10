package com.repp.aquaticremineztools.data.local.manga

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MangaDownloadEntity::class, MangaResultEntity::class], version = 7, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {

    abstract fun mangaDownloadDao(): MangaDownloadDao
    abstract fun mangaResultDao(): MangaResultDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manga_results ADD COLUMN originalSizeBytes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS manga_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        seriesTitle TEXT NOT NULL,
                        chapterTitle TEXT NOT NULL,
                        chapterUrl TEXT NOT NULL,
                        pdfPath TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        originalSizeBytes INTEGER NOT NULL DEFAULT 0,
                        pageCount INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'NOT_COMPLETED',
                        timestamp INTEGER NOT NULL,
                        errorMessage TEXT
                    )
                """)
            }
        }

        val MIGRATION_1_7 = object : Migration(1, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN lastHeartbeatAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS index_manga_downloads_chapterUrl")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_downloads_chapterUrl ON manga_downloads(chapterUrl)")
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN stage TEXT NOT NULL DEFAULT 'DOWNLOADING_IMAGES'")
                db.execSQL("ALTER TABLE manga_downloads ADD COLUMN stageProgress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS manga_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        seriesTitle TEXT NOT NULL,
                        chapterTitle TEXT NOT NULL,
                        chapterUrl TEXT NOT NULL,
                        pdfPath TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        originalSizeBytes INTEGER NOT NULL DEFAULT 0,
                        pageCount INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'NOT_COMPLETED',
                        timestamp INTEGER NOT NULL,
                        errorMessage TEXT
                    )
                """)
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
                .addMigrations(MIGRATION_1_7, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
