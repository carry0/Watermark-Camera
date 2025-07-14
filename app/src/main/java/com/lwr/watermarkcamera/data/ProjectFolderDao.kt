package com.lwr.watermarkcamera.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectFolderDao {
    @Query("SELECT * FROM project_folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<ProjectFolder>>

    @Query("SELECT * FROM project_folders WHERE name LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFolders(query: String): Flow<List<ProjectFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ProjectFolder)

    @Delete
    suspend fun deleteFolder(folder: ProjectFolder)

    @Query("SELECT * FROM project_folders WHERE name = :name")
    suspend fun getFolderByName(name: String): ProjectFolder?
} 