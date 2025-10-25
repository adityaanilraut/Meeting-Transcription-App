package com.example.voicerecordingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.voicerecordingapp.data.local.entity.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: TranscriptEntity): Long
    
    @Update
    suspend fun update(transcript: TranscriptEntity)
    
    @Delete
    suspend fun delete(transcript: TranscriptEntity)
    
    @Query("SELECT * FROM transcripts WHERE id = :id")
    suspend fun getTranscriptById(id: Long): TranscriptEntity?
    
    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY orderIndex ASC")
    suspend fun getTranscriptsByMeeting(meetingId: Long): List<TranscriptEntity>
    
    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY orderIndex ASC")
    fun getTranscriptsByMeetingFlow(meetingId: Long): Flow<List<TranscriptEntity>>
    
    @Query("SELECT * FROM transcripts WHERE chunkId = :chunkId")
    suspend fun getTranscriptByChunk(chunkId: Long): TranscriptEntity?
    
    @Query("SELECT text FROM transcripts WHERE meetingId = :meetingId ORDER BY orderIndex ASC")
    suspend fun getFullTranscript(meetingId: Long): List<String>
    
    @Query("DELETE FROM transcripts WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptsByMeeting(meetingId: Long)
    
    @Query("SELECT COUNT(*) FROM transcripts WHERE meetingId = :meetingId")
    suspend fun getTranscriptCount(meetingId: Long): Int
}

