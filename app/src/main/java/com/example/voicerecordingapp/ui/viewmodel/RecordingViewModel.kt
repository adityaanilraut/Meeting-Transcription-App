package com.example.voicerecordingapp.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecordingapp.data.repository.RecordingRepository
import com.example.voicerecordingapp.service.RecordingService
import com.example.voicerecordingapp.util.Constants
import com.example.voicerecordingapp.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository
) : ViewModel() {
    
    private val TAG = "RecordingViewModel"
    
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()
    
    private var recordingService: RecordingService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingBinder
            recordingService = binder.getService()
            serviceBound = true
            startStatePolling()
            Log.d(TAG, "Service connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            serviceBound = false
            Log.d(TAG, "Service disconnected")
        }
    }
    
    init {
        // Check for existing recording on startup
        viewModelScope.launch {
            val activeRecording = recordingRepository.getActiveRecording()
            if (activeRecording != null) {
                // Bind to service if there's an active recording
                bindToService()
            }
        }
    }
    
    fun startRecording() {
        if (!_uiState.value.hasRecordAudioPermission) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Microphone permission required"
            )
            return
        }
        
        if (!PermissionUtils.hasAllPermissions(context)) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "All required permissions not granted"
            )
            return
        }
        
        if (!recordingRepository.hasEnoughStorage()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Insufficient storage space"
            )
            return
        }
        
        val intent = Intent(context, RecordingService::class.java).apply {
            action = Constants.ACTION_START_RECORDING
        }
        context.startForegroundService(intent)
        
        bindToService()
        
        Log.d(TAG, "Recording start requested")
    }
    
    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = Constants.ACTION_STOP_RECORDING
        }
        context.startService(intent)
        
        unbindFromService()
        
        _uiState.value = RecordingUiState()
        
        Log.d(TAG, "Recording stop requested")
    }
    
    fun pauseRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = Constants.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
    }
    
    fun resumeRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = Constants.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
    }
    
    fun setPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasRecordAudioPermission = granted
        )
    }
    
    private fun bindToService() {
        if (!serviceBound) {
            val intent = Intent(context, RecordingService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    private fun unbindFromService() {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }
    
    private fun startStatePolling() {
        viewModelScope.launch {
            while (serviceBound) {
                val state = recordingService?.getRecordingState()
                if (state != null) {
                    _uiState.value = _uiState.value.copy(
                        isRecording = state.isRecording,
                        isPaused = state.isPaused,
                        elapsedTime = state.elapsedTime,
                        statusMessage = state.statusMessage,
                        meetingId = state.meetingId
                    )
                }
                delay(1000) // Update every second
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        unbindFromService()
    }
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Long = 0,
    val statusMessage: String = "Tap to start recording",
    val meetingId: Long = -1,
    val hasRecordAudioPermission: Boolean = false
)

