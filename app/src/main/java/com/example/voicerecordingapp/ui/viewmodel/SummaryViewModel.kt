package com.example.voicerecordingapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.voicerecordingapp.data.local.dao.MeetingDao
import com.example.voicerecordingapp.data.local.dao.TranscriptDao
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import com.example.voicerecordingapp.data.local.entity.TranscriptEntity
import com.example.voicerecordingapp.data.remote.model.SummaryData
import com.example.voicerecordingapp.data.repository.SummaryRepository
import com.example.voicerecordingapp.data.repository.SummaryState
import com.example.voicerecordingapp.util.Constants
import com.example.voicerecordingapp.worker.SummaryWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meetingDao: MeetingDao,
    private val transcriptDao: TranscriptDao,
    private val summaryRepository: SummaryRepository,
    private val workManager: WorkManager,
    private val gson: Gson
) : ViewModel() {
    
    private val meetingId: Long = savedStateHandle["meetingId"] ?: -1L
    
    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
    
    init {
        loadMeeting()
    }
    
    private fun loadMeeting() {
        viewModelScope.launch {
            try {
                val meeting = meetingDao.getMeetingById(meetingId)
                if (meeting == null) {
                    _uiState.value = SummaryUiState.Error("Meeting not found")
                    return@launch
                }
                
                val transcripts = transcriptDao.getTranscriptsByMeeting(meetingId)
                val existingSummary = summaryRepository.getSummary(meetingId)
                
                if (existingSummary != null && existingSummary.status == "Completed") {
                    // Summary already exists
                    val summaryData = SummaryData(
                        title = existingSummary.title,
                        summary = existingSummary.summary,
                        actionItems = gson.fromJson(existingSummary.actionItems, Array<String>::class.java).toList(),
                        keyPoints = gson.fromJson(existingSummary.keyPoints, Array<String>::class.java).toList()
                    )
                    
                    _uiState.value = SummaryUiState.Success(
                        meeting = meeting,
                        transcripts = transcripts,
                        summary = summaryData
                    )
                } else if (transcripts.isNotEmpty()) {
                    // Start generating summary
                    _uiState.value = SummaryUiState.Success(
                        meeting = meeting,
                        transcripts = transcripts,
                        summary = null,
                        isGenerating = true
                    )
                    generateSummary()
                } else {
                    // Waiting for transcription
                    _uiState.value = SummaryUiState.Success(
                        meeting = meeting,
                        transcripts = emptyList(),
                        summary = null,
                        isGenerating = false,
                        message = "Transcribing audio..."
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = SummaryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun generateSummary() {
        viewModelScope.launch {
            summaryRepository.generateSummary(meetingId).collect { state ->
                when (state) {
                    is SummaryState.Loading -> {
                        updateGeneratingState(isGenerating = true)
                    }
                    is SummaryState.Streaming -> {
                        updateStreamingContent(state.partialContent)
                    }
                    is SummaryState.Success -> {
                        val currentState = _uiState.value
                        if (currentState is SummaryUiState.Success) {
                            _uiState.value = currentState.copy(
                                summary = state.summary,
                                isGenerating = false
                            )
                        }
                    }
                    is SummaryState.Error -> {
                        updateError(state.message)
                    }
                }
            }
        }
    }
    
    fun retrySummary() {
        viewModelScope.launch {
            summaryRepository.retrySummary(meetingId)
            
            val currentState = _uiState.value
            if (currentState is SummaryUiState.Success) {
                _uiState.value = currentState.copy(
                    isGenerating = true,
                    error = null
                )
            }
            
            generateSummary()
        }
    }
    
    fun updateMeetingTitle(newTitle: String) {
        viewModelScope.launch {
            meetingDao.updateTitle(meetingId, newTitle)
            val currentState = _uiState.value
            if (currentState is SummaryUiState.Success) {
                _uiState.value = currentState.copy(
                    meeting = currentState.meeting.copy(title = newTitle)
                )
            }
        }
    }
    
    private fun updateGeneratingState(isGenerating: Boolean) {
        val currentState = _uiState.value
        if (currentState is SummaryUiState.Success) {
            _uiState.value = currentState.copy(isGenerating = isGenerating)
        }
    }
    
    private fun updateStreamingContent(content: String) {
        val currentState = _uiState.value
        if (currentState is SummaryUiState.Success) {
            _uiState.value = currentState.copy(
                streamingContent = content
            )
        }
    }
    
    private fun updateError(message: String) {
        val currentState = _uiState.value
        if (currentState is SummaryUiState.Success) {
            _uiState.value = currentState.copy(
                isGenerating = false,
                error = message
            )
        }
    }
}

sealed class SummaryUiState {
    object Loading : SummaryUiState()
    data class Success(
        val meeting: MeetingEntity,
        val transcripts: List<TranscriptEntity>,
        val summary: SummaryData?,
        val isGenerating: Boolean = false,
        val streamingContent: String? = null,
        val error: String? = null,
        val message: String? = null
    ) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}

