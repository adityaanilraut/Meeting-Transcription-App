package com.example.voicerecordingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.voicerecordingapp.data.local.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunkEntity): Long
    
    @Update
    suspend fun update(chunk: AudioChunkEntity)
    
    @Delete
    suspend fun delete(chunk: AudioChunkEntity)
    
    @Query("SELECT * FROM audio_chunks WHERE id = :id")
    suspend fun getChunkById(id: Long): AudioChunkEntity?
    
    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksByMeeting(meetingId: Long): List<AudioChunkEntity>
    
    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun getChunksByMeetingFlow(meetingId: Long): Flow<List<AudioChunkEntity>>
    
    @Query("SELECT * FROM audio_chunks WHERE transcriptionStatus = 'Pending' OR transcriptionStatus = 'Failed' ORDER BY meetingId, chunkIndex ASC")
    suspend fun getPendingChunks(): List<AudioChunkEntity>
    
    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId AND (transcriptionStatus = 'Pending' OR transcriptionStatus = 'Failed') ORDER BY chunkIndex ASC")
    suspend fun getPendingChunksByMeeting(meetingId: Long): List<AudioChunkEntity>
    
    @Query("UPDATE audio_chunks SET uploadStatus = :status WHERE id = :id")
    suspend fun updateUploadStatus(id: Long, status: String)
    
    @Query("UPDATE audio_chunks SET transcriptionStatus = :status WHERE id = :id")
    suspend fun updateTranscriptionStatus(id: Long, status: String)
    
    @Query("UPDATE audio_chunks SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)
    
    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId")
    suspend fun getChunkCount(meetingId: Long): Int
    
    @Query("SELECT COUNT(*) FROM audio_chunks WHERE meetingId = :meetingId AND transcriptionStatus = 'Completed'")
    suspend fun getCompletedChunkCount(meetingId: Long): Int
    
    @Query("DELETE FROM audio_chunks WHERE meetingId = :meetingId")
    suspend fun deleteChunksByMeeting(meetingId: Long)
}

