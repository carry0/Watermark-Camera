package com.lwr.watermarkcamera.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryRecordDao {
    
    @Query("SELECT * FROM history_records WHERE type = :type ORDER BY timestamp DESC LIMIT 10")
    fun getHistoryByType(type: String): Flow<List<HistoryRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryRecord)
    
    @Delete
    suspend fun deleteHistory(history: HistoryRecord)
    
    @Query("DELETE FROM history_records WHERE type = :type AND content = :content")
    suspend fun deleteHistoryByContent(type: String, content: String)
    
    @Query("SELECT COUNT(*) FROM history_records WHERE type = :type AND content = :content")
    suspend fun getHistoryCount(type: String, content: String): Int
} 