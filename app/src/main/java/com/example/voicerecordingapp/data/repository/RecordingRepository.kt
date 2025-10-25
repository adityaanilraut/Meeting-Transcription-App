package com.example.voicerecordingapp.data.repository

import android.content.Context
import com.example.voicerecordingapp.data.local.dao.AudioChunkDao
import com.example.voicerecordingapp.data.local.dao.MeetingDao
import com.example.voicerecordingapp.data.local.entity.AudioChunkEntity
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import com.example.voicerecordingapp.util.AudioUtils
import com.example.voicerecordingapp.util.Constants
import com.example.voicerecordingapp.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingDao: MeetingDao,
    private val audioChunkDao: AudioChunkDao
) {
    
    /**
     * Create a new meeting session
     */
    suspend fun createMeeting(title: String = "Meeting ${TimeUtils.formatDateTime(TimeUtils.currentTimeMillis())}"): Long {
        val meeting = MeetingEntity(
            title = title,
            startTime = TimeUtils.currentTimeMillis(),
            status = Constants.STATUS_RECORDING
        )
        return meetingDao.insert(meeting)
    }
    
    /**
     * Get meeting by ID
     */
    suspend fun getMeeting(id: Long): MeetingEntity? {
        return meetingDao.getMeetingById(id)
    }
    
    /**
     * Get meeting as Flow
     */
    fun getMeetingFlow(id: Long): Flow<MeetingEntity?> {
        return meetingDao.getMeetingByIdFlow(id)
    }
    
    /**
     * Update meeting status
     */
    suspend fun updateMeetingStatus(id: Long, status: String) {
        meetingDao.updateStatus(id, status)
    }
    
    /**
     * Complete meeting
     */
    suspend fun completeMeeting(id: Long) {
        val meeting = meetingDao.getMeetingById(id) ?: return
        val endTime = TimeUtils.currentTimeMillis()
        val duration = endTime - meeting.startTime
        meetingDao.updateEndTime(id, endTime, duration)
        meetingDao.updateStatus(id, Constants.STATUS_PROCESSING)
    }
    
    /**
     * Save audio chunk
     */
    suspend fun saveAudioChunk(
        meetingId: Long,
        chunkIndex: Int,
        filePath: String,
        duration: Long
    ): Long {
        val chunk = AudioChunkEntity(
            meetingId = meetingId,
            chunkIndex = chunkIndex,
            filePath = filePath,
            duration = duration,
            uploadStatus = "Pending",
            transcriptionStatus = "Pending"
        )
        return audioChunkDao.insert(chunk)
    }
    
    /**
     * Get all chunks for a meeting
     */
    suspend fun getChunks(meetingId: Long): List<AudioChunkEntity> {
        return audioChunkDao.getChunksByMeeting(meetingId)
    }
    
    /**
     * Get chunks for a meeting as Flow
     */
    fun getChunksFlow(meetingId: Long): Flow<List<AudioChunkEntity>> {
        return audioChunkDao.getChunksByMeetingFlow(meetingId)
    }
    
    /**
     * Get a specific chunk by ID
     */
    suspend fun getChunkById(chunkId: Long): AudioChunkEntity? {
        return audioChunkDao.getChunkById(chunkId)
    }
    
    /**
     * Check if there's enough storage
     */
    fun hasEnoughStorage(): Boolean {
        return AudioUtils.hasEnoughStorageToStart(context)
    }
    
    /**
     * Check if there's enough storage to continue
     */
    fun hasEnoughStorageToContinue(): Boolean {
        return AudioUtils.hasEnoughStorageToContinue(context)
    }
    
    /**
     * Get active recording session
     */
    suspend fun getActiveRecording(): MeetingEntity? {
        return meetingDao.getActiveRecording()
    }
    
    /**
     * Delete meeting
     */
    suspend fun deleteMeeting(id: Long) {
        // Get chunks to delete audio files
        val chunks = audioChunkDao.getChunksByMeeting(id)
        chunks.forEach { chunk ->
            AudioUtils.deleteAudioFile(java.io.File(chunk.filePath))
        }
        
        // Delete from database (cascades to chunks, transcripts, summary)
        meetingDao.deleteById(id)
    }
}

