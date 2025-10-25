package com.example.voicerecordingapp.data.repository

import android.util.Log
import com.example.voicerecordingapp.data.local.dao.SummaryDao
import com.example.voicerecordingapp.data.local.dao.TranscriptDao
import com.example.voicerecordingapp.data.local.entity.SummaryEntity
import com.example.voicerecordingapp.data.remote.api.OpenAIApi
import com.example.voicerecordingapp.data.remote.model.ChatMessage
import com.example.voicerecordingapp.data.remote.model.ChatRequest
import com.example.voicerecordingapp.data.remote.model.ChatResponse
import com.example.voicerecordingapp.data.remote.model.SummaryData
import com.example.voicerecordingapp.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val transcriptDao: TranscriptDao,
    private val summaryDao: SummaryDao,
    private val gson: Gson = Gson()
) {
    
    private val TAG = "SummaryRepository"
    
    /**
     * Generate summary from transcript with streaming
     */
    fun generateSummary(meetingId: Long): Flow<SummaryState> = flow {
        try {
            emit(SummaryState.Loading)
            
            // Get full transcript
            val transcriptTexts = transcriptDao.getFullTranscript(meetingId)
            if (transcriptTexts.isEmpty()) {
                throw Exception("No transcript available for meeting $meetingId")
            }
            
            val fullTranscript = transcriptTexts.joinToString("\n")
            
            // Create or update summary entity with generating status
            val existingSummary = summaryDao.getSummaryByMeeting(meetingId)
            if (existingSummary == null) {
                summaryDao.insert(
                    SummaryEntity(
                        meetingId = meetingId,
                        title = "",
                        summary = "",
                        actionItems = "[]",
                        keyPoints = "[]",
                        status = "Generating"
                    )
                )
            } else {
                summaryDao.updateStatus(meetingId, "Generating")
            }
            
            // Prepare chat request
            val messages = listOf(
                ChatMessage(role = "system", content = Constants.SUMMARY_SYSTEM_PROMPT),
                ChatMessage(role = "user", content = "Please summarize the following meeting transcript:\n\n$fullTranscript")
            )
            
            val request = ChatRequest(
                model = "gpt-4o-mini",
                messages = messages,
                stream = true,
                temperature = 0.7
            )
            
            // Make API call
            val response = openAIApi.createChatCompletion(request)
            
            if (!response.isSuccessful) {
                throw Exception("API error: ${response.code()} - ${response.message()}")
            }
            
            val responseBody = response.body() ?: throw Exception("Empty response body")
            
            // Parse streaming response
            val fullContent = StringBuilder()
            
            responseBody.byteStream().bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()
                        
                        if (data == "[DONE]") {
                            return@forEach
                        }
                        
                        try {
                            val chunk = gson.fromJson(data, ChatResponse::class.java)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            
                            if (content != null) {
                                fullContent.append(content)
                                emit(SummaryState.Streaming(fullContent.toString()))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chunk: $data", e)
                        }
                    }
                }
            }
            
            // Parse final JSON response
            val finalContent = fullContent.toString()
            val summaryData = try {
                gson.fromJson(finalContent, SummaryData::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse summary JSON, using fallback", e)
                SummaryData(
                    title = "Meeting Summary",
                    summary = finalContent,
                    actionItems = emptyList(),
                    keyPoints = emptyList()
                )
            }
            
            // Save to database
            summaryDao.updateSummary(
                meetingId = meetingId,
                title = summaryData.title,
                summary = summaryData.summary,
                actionItems = gson.toJson(summaryData.actionItems),
                keyPoints = gson.toJson(summaryData.keyPoints),
                status = "Completed"
            )
            
            emit(SummaryState.Success(summaryData))
            
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed", e)
            summaryDao.updateStatus(meetingId, "Failed")
            emit(SummaryState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get summary by meeting
     */
    suspend fun getSummary(meetingId: Long): SummaryEntity? {
        return summaryDao.getSummaryByMeeting(meetingId)
    }
    
    /**
     * Get summary as Flow
     */
    fun getSummaryFlow(meetingId: Long): Flow<SummaryEntity?> {
        return summaryDao.getSummaryByMeetingFlow(meetingId)
    }
    
    /**
     * Retry summary generation
     */
    suspend fun retrySummary(meetingId: Long) {
        summaryDao.updateStatus(meetingId, "Generating")
    }
}

sealed class SummaryState {
    object Loading : SummaryState()
    data class Streaming(val partialContent: String) : SummaryState()
    data class Success(val summary: SummaryData) : SummaryState()
    data class Error(val message: String) : SummaryState()
}

