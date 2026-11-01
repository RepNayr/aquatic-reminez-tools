package com.repp.aquaticremineztools.data.local.compressor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CompressorItemEntity::class], version = 2, exportSchema = false)
abstract class CompressorDatabase : RoomDatabase() {

    abstract fun compressorDao(): CompressorDao

    companion object {
        @Volatile
        private var INSTANCE: CompressorDatabase? = null

        fun getInstance(context: Context): CompressorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CompressorDatabase::class.java,
                    "aquatic_compressor_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
