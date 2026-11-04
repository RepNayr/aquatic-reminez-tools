package com.repp.aquaticremineztools.data.local.compressor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CompressorItemEntity::class], version = 3, exportSchema = false)
abstract class CompressorDatabase : RoomDatabase() {

    abstract fun compressorDao(): CompressorDao

    companion object {
        @Volatile
        private var INSTANCE: CompressorDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE compressor_items ADD COLUMN detectedCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE compressor_items ADD COLUMN targetSizeAchieved INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE compressor_items ADD COLUMN processingTimeMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE compressor_items ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE compressor_items ADD COLUMN errorCategory TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): CompressorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CompressorDatabase::class.java,
                    "aquatic_compressor_db"
                ).addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
