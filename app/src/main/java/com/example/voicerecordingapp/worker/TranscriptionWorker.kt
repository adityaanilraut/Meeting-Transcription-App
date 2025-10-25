package com.example.voicerecordingapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicerecordingapp.data.repository.TranscriptionRepository
import com.example.voicerecordingapp.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    private val TAG = "TranscriptionWorker"
    
    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong(Constants.KEY_MEETING_ID, -1L)
        
        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting ID")
            return Result.failure()
        }
        
        Log.d(TAG, "Starting transcription for meeting $meetingId")
        
        return try {
            // Get TranscriptionRepository through EntryPoint
            val transcriptionRepository = EntryPointAccessors.fromApplication(
                applicationContext,
                TranscriptionWorkerEntryPoint::class.java
            ).transcriptionRepository()
            
            val result = transcriptionRepository.transcribeAllPendingChunks(meetingId)
            
            if (result.isSuccess) {
                Log.d(TAG, "Transcription completed successfully for meeting $meetingId")
                Result.success()
            } else {
                Log.e(TAG, "Transcription failed for meeting $meetingId: ${result.exceptionOrNull()?.message}")
                
                // Retry if not max attempts
                if (runAttemptCount < Constants.MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription worker error", e)
            
            if (runAttemptCount < Constants.MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TranscriptionWorkerEntryPoint {
    fun transcriptionRepository(): TranscriptionRepository
}

