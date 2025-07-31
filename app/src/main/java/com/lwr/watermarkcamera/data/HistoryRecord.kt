package com.lwr.watermarkcamera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "title" 或 "imageName"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) 