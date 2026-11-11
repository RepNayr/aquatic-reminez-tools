package com.repp.aquaticremineztools.data.local.video

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoDownloadEntity::class, VideoResultEntity::class], version = 1, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {

    abstract fun videoDownloadDao(): VideoDownloadDao
    abstract fun videoResultDao(): VideoResultDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getInstance(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "aquatic_video_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
