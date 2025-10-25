package com.example.voicerecordingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.voicerecordingapp.data.local.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity): Long
    
    @Update
    suspend fun update(summary: SummaryEntity)
    
    @Delete
    suspend fun delete(summary: SummaryEntity)
    
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: Long): SummaryEntity?
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryByMeeting(meetingId: Long): SummaryEntity?
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryByMeetingFlow(meetingId: Long): Flow<SummaryEntity?>
    
    @Query("UPDATE summaries SET status = :status WHERE meetingId = :meetingId")
    suspend fun updateStatus(meetingId: Long, status: String)
    
    @Query("UPDATE summaries SET title = :title, summary = :summary, actionItems = :actionItems, keyPoints = :keyPoints, status = :status WHERE meetingId = :meetingId")
    suspend fun updateSummary(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        status: String
    )
    
    @Query("DELETE FROM summaries WHERE meetingId = :meetingId")
    suspend fun deleteSummaryByMeeting(meetingId: Long)
}

