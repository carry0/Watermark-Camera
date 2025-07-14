package com.lwr.watermarkcamera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectFolder::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectFolderDao(): ProjectFolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watermark_camera_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 