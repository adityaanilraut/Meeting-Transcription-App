package com.example.voicerecordingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecordingapp.data.local.entity.AudioChunkEntity
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import com.example.voicerecordingapp.data.repository.RecordingRepository
import com.example.voicerecordingapp.util.AudioPlayer
import com.example.voicerecordingapp.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackUiState(
    val meeting: MeetingEntity? = null,
    val chunks: List<AudioChunkEntity> = emptyList(),
    val currentChunkIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    
    private var meetingId: Long = -1
    
    init {
        // Observe audio player state
        viewModelScope.launch {
            combine(
                audioPlayer.isPlaying,
                audioPlayer.isPrepared,
                audioPlayer.currentPosition,
                audioPlayer.duration
            ) { isPlaying, isPrepared, currentPosition, duration ->
                _uiState.value.copy(
                    isPlaying = isPlaying,
                    isPrepared = isPrepared,
                    currentPosition = currentPosition,
                    duration = duration
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun loadMeeting(meetingId: Long) {
        this.meetingId = meetingId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // Load meeting details
                val meeting = recordingRepository.getMeeting(meetingId)
                if (meeting == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Meeting not found"
                    )
                    return@launch
                }
                
                // Load chunks
                recordingRepository.getChunksFlow(meetingId).collect { chunks ->
                    _uiState.value = _uiState.value.copy(
                        meeting = meeting,
                        chunks = chunks.sortedBy { it.chunkIndex },
                        isLoading = false,
                        errorMessage = null
                    )
                    
                    // Load first chunk if available
                    if (chunks.isNotEmpty()) {
                        loadChunk(0)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load meeting: ${e.message}"
                )
            }
        }
    }
    
    fun play() {
        if (_uiState.value.isPrepared) {
            audioPlayer.play()
        } else {
            // Load current chunk if not prepared
            loadCurrentChunk()
        }
    }
    
    fun pause() {
        audioPlayer.pause()
    }
    
    fun stop() {
        audioPlayer.stop()
    }
    
    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }
    
    fun loadChunk(chunkIndex: Int) {
        val chunks = _uiState.value.chunks
        if (chunkIndex >= 0 && chunkIndex < chunks.size) {
            val chunk = chunks[chunkIndex]
            _uiState.value = _uiState.value.copy(currentChunkIndex = chunkIndex)
            
            viewModelScope.launch {
                try {
                    audioPlayer.loadAudio(chunk.filePath) { error ->
                        _uiState.value = _uiState.value.copy(errorMessage = error)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load chunk: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun loadCurrentChunk() {
        loadChunk(_uiState.value.currentChunkIndex)
    }
    
    fun nextChunk() {
        val currentIndex = _uiState.value.currentChunkIndex
        val chunks = _uiState.value.chunks
        if (currentIndex < chunks.size - 1) {
            stop()
            loadChunk(currentIndex + 1)
        }
    }
    
    fun previousChunk() {
        val currentIndex = _uiState.value.currentChunkIndex
        if (currentIndex > 0) {
            stop()
            loadChunk(currentIndex - 1)
        }
    }
    
    fun formatDuration(milliseconds: Long): String {
        return TimeUtils.formatDuration(milliseconds)
    }
    
    fun getCurrentChunk(): AudioChunkEntity? {
        val chunks = _uiState.value.chunks
        val currentIndex = _uiState.value.currentChunkIndex
        return if (currentIndex >= 0 && currentIndex < chunks.size) {
            chunks[currentIndex]
        } else null
    }
    
    fun getTotalDuration(): Long {
        return _uiState.value.chunks.sumOf { it.duration }
    }
    
    fun getTotalDurationFormatted(): String {
        return formatDuration(getTotalDuration())
    }
    
    fun getCurrentChunkInfo(): String {
        val currentIndex = _uiState.value.currentChunkIndex
        val totalChunks = _uiState.value.chunks.size
        return "Chunk ${currentIndex + 1} of $totalChunks"
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayer.cleanup()
    }
}
