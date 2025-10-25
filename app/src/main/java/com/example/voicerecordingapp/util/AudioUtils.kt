package com.example.voicerecordingapp.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.StatFs
import java.io.File

object AudioUtils {
    
    /**
     * Check available storage in MB
     */
    fun getAvailableStorageMB(context: Context): Long {
        val path = context.filesDir
        val stat = StatFs(path.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes / (1024 * 1024) // Convert to MB
    }
    
    /**
     * Check if there's enough storage to start recording
     */
    fun hasEnoughStorageToStart(context: Context): Boolean {
        return getAvailableStorageMB(context) >= Constants.MIN_STORAGE_TO_START_MB
    }
    
    /**
     * Check if there's enough storage to continue recording
     */
    fun hasEnoughStorageToContinue(context: Context): Boolean {
        return getAvailableStorageMB(context) >= Constants.MIN_STORAGE_TO_CONTINUE_MB
    }
    
    /**
     * Get amplitude from MediaRecorder
     */
    fun getAmplitude(recorder: MediaRecorder?): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Check if audio is silent based on amplitude
     */
    fun isSilent(amplitude: Int): Boolean {
        return amplitude < Constants.SILENT_AMPLITUDE_THRESHOLD
    }
    
    /**
     * Create recordings directory
     */
    fun createRecordingsDirectory(context: Context): File {
        val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR)
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }
    
    /**
     * Generate file name for audio chunk
     */
    fun generateChunkFileName(meetingId: Long, chunkIndex: Int): String {
        return "meeting_${meetingId}_chunk_${chunkIndex}.mp3"
    }
    
    /**
     * Get audio file for chunk
     */
    fun getChunkFile(context: Context, meetingId: Long, chunkIndex: Int): File {
        val recordingsDir = createRecordingsDirectory(context)
        return File(recordingsDir, generateChunkFileName(meetingId, chunkIndex))
    }
    
    /**
     * Delete audio file
     */
    fun deleteAudioFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create MediaRecorder for API 31+
     */
    fun createMediaRecorder(context: Context): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}

