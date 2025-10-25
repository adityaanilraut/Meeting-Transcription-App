package com.example.voicerecordingapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicerecordingapp.data.repository.SummaryRepository
import com.example.voicerecordingapp.data.repository.SummaryState
import com.example.voicerecordingapp.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.last

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    private val TAG = "SummaryWorker"
    
    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong(Constants.KEY_MEETING_ID, -1L)
        
        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting ID")
            return Result.failure()
        }
        
        Log.d(TAG, "Starting summary generation for meeting $meetingId")
        
        return try {
            // Get SummaryRepository through EntryPoint
            val summaryRepository = EntryPointAccessors.fromApplication(
                applicationContext,
                SummaryWorkerEntryPoint::class.java
            ).summaryRepository()
            
            // Collect the flow until completion
            val lastState = summaryRepository.generateSummary(meetingId).last()
            
            when (lastState) {
                is SummaryState.Success -> {
                    Log.d(TAG, "Summary generated successfully for meeting $meetingId")
                    Result.success()
                }
                is SummaryState.Error -> {
                    Log.e(TAG, "Summary generation failed: ${lastState.message}")
                    
                    if (runAttemptCount < Constants.MAX_RETRY_COUNT) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                else -> {
                    Log.e(TAG, "Unexpected state: $lastState")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summary worker error", e)
            
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
interface SummaryWorkerEntryPoint {
    fun summaryRepository(): SummaryRepository
}

