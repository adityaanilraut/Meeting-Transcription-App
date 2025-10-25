package com.example.voicerecordingapp.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.voicerecordingapp.MainActivity
import com.example.voicerecordingapp.R
import com.example.voicerecordingapp.VoiceRecordingApplication
import com.example.voicerecordingapp.data.repository.RecordingRepository
import com.example.voicerecordingapp.util.AudioUtils
import com.example.voicerecordingapp.util.Constants
import com.example.voicerecordingapp.util.TimeUtils
import com.example.voicerecordingapp.worker.TranscriptionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    
    @Inject
    lateinit var recordingRepository: RecordingRepository
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    @Inject
    lateinit var audioManager: AudioManager
    
    @Inject
    lateinit var telephonyManager: TelephonyManager
    
    private val binder = RecordingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "RecordingService"
    
    // Recording state
    private var mediaRecorder: MediaRecorder? = null
    private var currentMeetingId: Long = -1
    private var currentChunkIndex = 0
    private var isRecording = false
    private var isPaused = false
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var lastPauseTime = 0L
    
    // Overlap recording
    private var nextMediaRecorder: MediaRecorder? = null
    private var isOverlapping = false
    
    // Status tracking
    private var statusMessage = "Idle"
    private var isSilent = false
    private var silentStartTime = 0L
    
    // Handlers
    private val timerHandler = Handler(Looper.getMainLooper())
    private val chunkHandler = Handler(Looper.getMainLooper())
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    
    // Audio focus
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    
    // Phone state
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null
    
    // Headset receiver
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    val name = intent.getStringExtra("name") ?: "headset"
                    if (state == 1) {
                        Log.d(TAG, "Headset plugged: $name")
                    } else if (state == 0) {
                        Log.d(TAG, "Headset unplugged: $name")
                    }
                    // Continue recording regardless
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        registerHeadsetReceiver()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_RECORDING -> startRecording()
            Constants.ACTION_PAUSE_RECORDING -> pauseRecording("Paused by user")
            Constants.ACTION_RESUME_RECORDING -> resumeRecording()
            Constants.ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_STICKY
    }
    
    private fun startRecording() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Check storage
                if (!recordingRepository.hasEnoughStorage()) {
                    statusMessage = "Error: Insufficient storage"
                    updateNotification()
                    stopSelf()
                    return@launch
                }
                
                // Check for existing recording
                val existingRecording = recordingRepository.getActiveRecording()
                if (existingRecording != null) {
                    // Resume existing recording
                    currentMeetingId = existingRecording.id
                    val chunks = recordingRepository.getChunks(currentMeetingId)
                    currentChunkIndex = chunks.size
                } else {
                    // Create new meeting
                    currentMeetingId = recordingRepository.createMeeting()
                    currentChunkIndex = 0
                }
                
                launch(Dispatchers.Main) {
                    // Start foreground service
                    startForeground(
                        Constants.RECORDING_NOTIFICATION_ID,
                        createNotification("Recording...", showPauseAction = true)
                    )
                    
                    // Request audio focus
                    requestAudioFocus()
                    
                    // Register phone state listener
                    registerPhoneStateListener()
                    
                    // Start recording
                    isRecording = true
                    isPaused = false
                    recordingStartTime = TimeUtils.currentTimeMillis() - pausedDuration
                    statusMessage = "Recording..."
                    
                    startMediaRecorder()
                    startTimerUpdates()
                    startChunkRecording()
                    startAmplitudeMonitoring()
                    
                    Log.d(TAG, "Recording started for meeting $currentMeetingId")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                statusMessage = "Error: ${e.message}"
                stopSelf()
            }
        }
    }
    
    private fun startMediaRecorder() {
        try {
            val file = AudioUtils.getChunkFile(this, currentMeetingId, currentChunkIndex)
            
            mediaRecorder = AudioUtils.createMediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(Constants.SAMPLE_RATE)
                setAudioEncodingBitRate(Constants.BIT_RATE)
                setAudioChannels(Constants.AUDIO_CHANNELS)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            Log.d(TAG, "MediaRecorder started for chunk $currentChunkIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder", e)
            throw e
        }
    }
    
    private fun startChunkRecording() {
        val chunkRunnable = object : Runnable {
            override fun run() {
                if (isRecording && !isPaused) {
                    serviceScope.launch(Dispatchers.IO) {
                        // Check storage before next chunk
                        if (!recordingRepository.hasEnoughStorageToContinue()) {
                            launch(Dispatchers.Main) {
                                statusMessage = "Recording stopped - Low storage"
                                updateNotification()
                                stopRecording()
                            }
                            return@launch
                        }
                        
                        // Start overlap recording
                        startOverlapRecording()
                        
                        // Schedule the end of current chunk after overlap period
                        chunkHandler.postDelayed({
                            if (isRecording && !isPaused) {
                                serviceScope.launch(Dispatchers.IO) {
                                    finalizeCurrentChunk()
                                    switchToNextChunk()
                                }
                            }
                        }, Constants.OVERLAP_DURATION_MS)
                    }
                    // Schedule next chunk - This needs to be outside the serviceScope.launch block
                    chunkHandler.postDelayed(this, Constants.CHUNK_DURATION_MS)
                } else {
                    chunkHandler.postDelayed(this, 1000)
                }
            }
        }
        chunkHandler.postDelayed(chunkRunnable, Constants.CHUNK_DURATION_MS)
    }
    
    private fun startOverlapRecording() {
        if (isOverlapping) return
        
        try {
            currentChunkIndex++
            val file = AudioUtils.getChunkFile(this, currentMeetingId, currentChunkIndex)
            
            nextMediaRecorder = AudioUtils.createMediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(Constants.SAMPLE_RATE)
                setAudioEncodingBitRate(Constants.BIT_RATE)
                setAudioChannels(Constants.AUDIO_CHANNELS)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            isOverlapping = true
            Log.d(TAG, "Overlap recording started for chunk $currentChunkIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlap recording", e)
            isOverlapping = false
        }
    }
    
    private fun switchToNextChunk() {
        try {
            // Stop current recorder
            mediaRecorder?.stop()
            mediaRecorder?.release()
            
            // Switch to next recorder
            mediaRecorder = nextMediaRecorder
            nextMediaRecorder = null
            isOverlapping = false
            
            Log.d(TAG, "Switched to next chunk $currentChunkIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch to next chunk", e)
        }
    }
    
    private suspend fun finalizeCurrentChunk() {
        try {
            val file = AudioUtils.getChunkFile(this, currentMeetingId, currentChunkIndex)
            
            // Save chunk to database
            recordingRepository.saveAudioChunk(
                meetingId = currentMeetingId,
                chunkIndex = currentChunkIndex,
                filePath = file.absolutePath,
                duration = Constants.CHUNK_DURATION_MS
            )
            
            // Enqueue transcription work
            enqueueTranscriptionWork(currentMeetingId)
            
            Log.d(TAG, "Chunk $currentChunkIndex finalized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize chunk", e)
        }
    }
    
    private fun startTimerUpdates() {
        timerHandler.post(object : Runnable {
            override fun run() {
                if (isRecording) {
                    updateNotification()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        })
    }
    
    private fun startAmplitudeMonitoring() {
        amplitudeHandler.post(object : Runnable {
            override fun run() {
                if (isRecording && !isPaused) {
                    val amplitude = AudioUtils.getAmplitude(mediaRecorder)
                    
                    if (AudioUtils.isSilent(amplitude)) {
                        if (silentStartTime == 0L) {
                            silentStartTime = TimeUtils.currentTimeMillis()
                        } else {
                            val silentDuration = TimeUtils.currentTimeMillis() - silentStartTime
                            if (silentDuration >= Constants.SILENT_DURATION_THRESHOLD_MS && !isSilent) {
                                isSilent = true
                                statusMessage = "No audio detected - Check microphone"
                                updateNotification()
                            }
                        }
                    } else {
                        silentStartTime = 0L
                        if (isSilent) {
                            isSilent = false
                            statusMessage = "Recording..."
                            updateNotification()
                        }
                    }
                }
                
                if (isRecording) {
                    amplitudeHandler.postDelayed(this, 1000)
                }
            }
        })
    }
    
    private fun pauseRecording(reason: String) {
        if (isRecording && !isPaused) {
            isPaused = true
            lastPauseTime = TimeUtils.currentTimeMillis()
            statusMessage = reason
            
            // Stop media recorders
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                
                nextMediaRecorder?.stop()
                nextMediaRecorder?.release()
                nextMediaRecorder = null
                isOverlapping = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media recorder", e)
            }
            
            // Save current chunk
            serviceScope.launch(Dispatchers.IO) {
                finalizeCurrentChunk()
            }
            
            updateNotification(showResumeAction = true)
            Log.d(TAG, "Recording paused: $reason")
        }
    }
    
    private fun resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false
            pausedDuration += TimeUtils.currentTimeMillis() - lastPauseTime
            statusMessage = "Recording..."
            
            // Start next chunk
            currentChunkIndex++
            startMediaRecorder()
            
            updateNotification(showPauseAction = true)
            Log.d(TAG, "Recording resumed")
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        isPaused = false
        
        // Stop handlers
        timerHandler.removeCallbacksAndMessages(null)
        chunkHandler.removeCallbacksAndMessages(null)
        amplitudeHandler.removeCallbacksAndMessages(null)
        
        // Stop media recorders
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            
            nextMediaRecorder?.stop()
            nextMediaRecorder?.release()
            nextMediaRecorder = null
            isOverlapping = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        }
        
        // Finalize last chunk and complete meeting
        serviceScope.launch(Dispatchers.IO) {
            finalizeCurrentChunk()
            recordingRepository.completeMeeting(currentMeetingId)
            
            // Enqueue transcription work for all pending chunks
            enqueueTranscriptionWork(currentMeetingId)
        }
        
        // Release audio focus
        releaseAudioFocus()
        
        // Unregister phone state listener
        unregisterPhoneStateListener()
        
        // Stop foreground
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Log.d(TAG, "Recording stopped")
    }
    
    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        
        return hasAudioFocus
    }
    
    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (!isPaused) {
                    pauseRecording("Paused - Audio focus lost")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPaused && statusMessage == "Paused - Audio focus lost") {
                    resumeRecording()
                }
            }
        }
    }
    
    private fun registerPhoneStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerPhoneStateCallback()
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallStateChange(state)
                }
            }.also {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerPhoneStateCallback() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state)
            }
        }
        telephonyCallback = callback
        telephonyManager.registerTelephonyCallback(mainExecutor, callback)
    }
    
    private fun handleCallStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!isPaused) {
                    pauseRecording("Paused - Phone call")
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isPaused && statusMessage == "Paused - Phone call") {
                    // Wait a bit before resuming
                    Handler(Looper.getMainLooper()).postDelayed({
                        resumeRecording()
                    }, 1000)
                }
            }
        }
    }
    
    private fun unregisterPhoneStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
    
    private fun registerHeadsetReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        registerReceiver(headsetReceiver, filter)
    }
    
    private fun enqueueTranscriptionWork(meetingId: Long) {
        val workData = Data.Builder()
            .putLong(Constants.KEY_MEETING_ID, meetingId)
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val transcriptionWork = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workData)
            .setConstraints(constraints)
            .addTag(Constants.WORK_TAG_TRANSCRIPTION)
            .build()
        
        WorkManager.getInstance(this).enqueue(transcriptionWork)
    }
    
    private fun createNotification(
        status: String,
        showPauseAction: Boolean = false,
        showResumeAction: Boolean = false
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(this, VoiceRecordingApplication.RECORDING_CHANNEL_ID)
            .setContentTitle("Voice Recording")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
        
        // Add timer if recording
        if (isRecording && !isPaused) {
            val elapsedTime = TimeUtils.currentTimeMillis() - recordingStartTime
            builder.setSubText(TimeUtils.formatDuration(elapsedTime))
        }
        
        // Add actions
        if (showPauseAction) {
            val pauseIntent = Intent(this, RecordingService::class.java).apply {
                action = Constants.ACTION_PAUSE_RECORDING
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Pause", pausePendingIntent)
        }
        
        if (showResumeAction) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply {
                action = Constants.ACTION_RESUME_RECORDING
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumePendingIntent)
        }
        
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = Constants.ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPendingIntent)
        
        return builder.build()
    }
    
    private fun updateNotification(showPauseAction: Boolean = false, showResumeAction: Boolean = false) {
        val notification = createNotification(statusMessage, showPauseAction, showResumeAction)
        notificationManager.notify(Constants.RECORDING_NOTIFICATION_ID, notification)
    }
    
    fun getRecordingState(): RecordingState {
        val elapsedTime = if (isRecording && !isPaused) {
            TimeUtils.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
        
        return RecordingState(
            isRecording = isRecording,
            isPaused = isPaused,
            meetingId = currentMeetingId,
            elapsedTime = elapsedTime,
            statusMessage = statusMessage
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up
        timerHandler.removeCallbacksAndMessages(null)
        chunkHandler.removeCallbacksAndMessages(null)
        amplitudeHandler.removeCallbacksAndMessages(null)
        
        try {
            unregisterReceiver(headsetReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering headset receiver", e)
        }
        
        unregisterPhoneStateListener()
        releaseAudioFocus()
        
        serviceScope.cancel()
        
        Log.d(TAG, "Service destroyed")
    }
    
    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
}

data class RecordingState(
    val isRecording: Boolean,
    val isPaused: Boolean,
    val meetingId: Long,
    val elapsedTime: Long,
    val statusMessage: String
)