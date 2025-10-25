package com.example.voicerecordingapp.util

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayer {
    
    private val TAG = "AudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    /**
     * Load audio file for playback
     */
    fun loadAudio(filePath: String, onError: (String) -> Unit = {}) {
        try {
            release()
            
            val file = File(filePath)
            if (!file.exists()) {
                onError("Audio file not found: $filePath")
                return
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    _isPrepared.value = true
                    _duration.value = mp.duration.toLong()
                    Log.d(TAG, "Audio prepared: duration=${mp.duration}ms")
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    positionUpdateJob?.cancel()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    onError("Playback error: $what")
                    _isPlaying.value = false
                    _isPrepared.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio", e)
            onError("Failed to load audio: ${e.message}")
        }
    }
    
    /**
     * Start or resume playback
     */
    fun play() {
        mediaPlayer?.let { mp ->
            if (_isPrepared.value) {
                try {
                    if (!mp.isPlaying) {
                        mp.start()
                        _isPlaying.value = true
                        startPositionUpdates()
                        Log.d(TAG, "Playback started")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start playback", e)
                }
            }
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                positionUpdateJob?.cancel()
                Log.d(TAG, "Playback paused")
            }
        }
    }
    
    /**
     * Stop playback and reset position
     */
    fun stop() {
        mediaPlayer?.let { mp ->
            mp.stop()
            _isPlaying.value = false
            _currentPosition.value = 0L
            positionUpdateJob?.cancel()
            Log.d(TAG, "Playback stopped")
        }
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            if (_isPrepared.value) {
                try {
                    mp.seekTo(positionMs.toInt())
                    _currentPosition.value = positionMs
                    Log.d(TAG, "Seeked to ${positionMs}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seek", e)
                }
            }
        }
    }
    
    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition?.toLong() ?: 0L
    }
    
    /**
     * Get total duration
     */
    fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0L
    }
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    /**
     * Start position updates
     */
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = playerScope.launch {
            while (_isPlaying.value && _isPrepared.value) {
                _currentPosition.value = getCurrentPosition()
                delay(100) // Update every 100ms
            }
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        positionUpdateJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
        _isPrepared.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }
    
    /**
     * Cleanup when done
     */
    fun cleanup() {
        release()
        playerScope.cancel()
    }
}
