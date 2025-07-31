package com.lwr.watermarkcamera.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectFolder::class, HistoryRecord::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectFolderDao(): ProjectFolderDao
    abstract fun historyRecordDao(): HistoryRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watermark_camera_db"
                )
                .fallbackToDestructiveMigration() // 简单处理版本升级
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 