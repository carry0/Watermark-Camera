package com.lwr.watermarkcamera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_folders")
data class ProjectFolder(
    @PrimaryKey val name: String,
    val title: String?,
    val createdAt: Long = System.currentTimeMillis()
) 