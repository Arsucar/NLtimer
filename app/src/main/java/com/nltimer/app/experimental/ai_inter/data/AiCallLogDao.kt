package com.nltimer.app.experimental.ai_inter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCallLogDao {
    @Query("SELECT * FROM ai_call_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AiCallLogEntity>>

    @Insert
    suspend fun insertLog(log: AiCallLogEntity)

    @Query("DELETE FROM ai_call_logs")
    suspend fun clearLogs()
}
