package com.example.voicerecordingapp.data.repository

import android.util.Log
import com.example.voicerecordingapp.data.local.dao.AudioChunkDao
import com.example.voicerecordingapp.data.local.dao.TranscriptDao
import com.example.voicerecordingapp.data.local.entity.AudioChunkEntity
import com.example.voicerecordingapp.data.local.entity.TranscriptEntity
import com.example.voicerecordingapp.data.remote.api.OpenAIApi
import com.example.voicerecordingapp.util.Constants
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

@Singleton
class TranscriptionRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptDao: TranscriptDao
) {
    
    private val TAG = "TranscriptionRepository"
    
    /**
     * Transcribe a single audio chunk
     */
    suspend fun transcribeChunk(chunk: AudioChunkEntity): Result<String> {
        return try {
            // Update status to transcribing
            audioChunkDao.updateTranscriptionStatus(chunk.id, "Transcribing")
            
            // Prepare file
            val file = File(chunk.filePath)
            if (!file.exists()) {
                throw Exception("Audio file not found: ${chunk.filePath}")
            }
            
            // Create multipart request
            val requestFile = file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val modelPart = "whisper-1".toRequestBody("text/plain".toMediaTypeOrNull())
            
            // Make API call
            val response = openAIApi.transcribeAudio(filePart, modelPart)
            
            if (response.isSuccessful && response.body() != null) {
                val transcription = response.body()!!.text
                
                // Save transcript to database
                val transcriptEntity = TranscriptEntity(
                    meetingId = chunk.meetingId,
                    chunkId = chunk.id,
                    text = transcription,
                    orderIndex = chunk.chunkIndex
                )
                transcriptDao.insert(transcriptEntity)
                
                // Update chunk status
                audioChunkDao.updateTranscriptionStatus(chunk.id, "Completed")
                
                Log.d(TAG, "Transcription successful for chunk ${chunk.id}")
                Result.success(transcription)
            } else {
                val errorMsg = "API error: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                throw Exception(errorMsg)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed for chunk ${chunk.id}", e)
            
            // Increment retry count
            audioChunkDao.incrementRetryCount(chunk.id)
            
            // Mark as failed if max retries reached
            val updatedChunk = audioChunkDao.getChunkById(chunk.id)
            if (updatedChunk != null && updatedChunk.retryCount >= Constants.MAX_RETRY_COUNT) {
                audioChunkDao.updateTranscriptionStatus(chunk.id, "Failed")
            } else {
                audioChunkDao.updateTranscriptionStatus(chunk.id, "Pending")
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * Transcribe all pending chunks for a meeting with retry logic
     */
    suspend fun transcribeAllPendingChunks(meetingId: Long): Result<Unit> {
        return try {
            val pendingChunks = audioChunkDao.getPendingChunksByMeeting(meetingId)
            
            if (pendingChunks.isEmpty()) {
                return Result.success(Unit)
            }
            
            Log.d(TAG, "Starting transcription for ${pendingChunks.size} chunks")
            
            for (chunk in pendingChunks) {
                var retryCount = 0
                var success = false
                
                while (retryCount < Constants.MAX_RETRY_COUNT && !success) {
                    val result = transcribeChunk(chunk)
                    
                    if (result.isSuccess) {
                        success = true
                    } else {
                        retryCount++
                        if (retryCount < Constants.MAX_RETRY_COUNT) {
                            val delayMs = calculateBackoffDelay(retryCount)
                            Log.d(TAG, "Retrying chunk ${chunk.id} in ${delayMs}ms (attempt $retryCount)")
                            delay(delayMs)
                        }
                    }
                }
                
                if (!success) {
                    Log.e(TAG, "Failed to transcribe chunk ${chunk.id} after $retryCount attempts")
                }
            }
            
            // Check if all chunks are completed
            val remainingPending = audioChunkDao.getPendingChunksByMeeting(meetingId)
            if (remainingPending.isEmpty()) {
                Log.d(TAG, "All chunks transcribed successfully for meeting $meetingId")
                Result.success(Unit)
            } else {
                throw Exception("${remainingPending.size} chunks failed to transcribe")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Transcription batch failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get full transcript for a meeting
     */
    suspend fun getFullTranscript(meetingId: Long): String {
        val transcripts = transcriptDao.getFullTranscript(meetingId)
        return transcripts.joinToString("\n")
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        val delay = Constants.INITIAL_RETRY_DELAY_MS * (2.0.pow(retryCount.toDouble())).toLong()
        return min(delay, Constants.MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Get pending chunks count
     */
    suspend fun getPendingChunksCount(meetingId: Long): Int {
        return audioChunkDao.getPendingChunksByMeeting(meetingId).size
    }
}

