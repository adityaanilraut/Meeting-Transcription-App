package com.example.voicerecordingapp.util

object Constants {
    // API
    const val OPENAI_BASE_URL = "https://api.openai.com/"
    const val OPENAI_TIMEOUT_SECONDS = 60L
    
    // Recording
    const val CHUNK_DURATION_MS = 30_000L // 30 seconds
    const val OVERLAP_DURATION_MS = 2_000L // 2 seconds
    const val SAMPLE_RATE = 44100
    const val BIT_RATE = 128_000
    const val AUDIO_CHANNELS = 1 // Mono
    
    // Storage
    const val MIN_STORAGE_TO_START_MB = 100L
    const val MIN_STORAGE_TO_CONTINUE_MB = 50L
    const val RECORDINGS_DIR = "recordings"
    
    // Silent Audio Detection
    const val SILENT_AMPLITUDE_THRESHOLD = 500
    const val SILENT_DURATION_THRESHOLD_MS = 10_000L // 10 seconds
    
    // Retry
    const val MAX_RETRY_COUNT = 3
    const val INITIAL_RETRY_DELAY_MS = 1_000L
    const val MAX_RETRY_DELAY_MS = 30_000L
    
    // Notification
    const val RECORDING_NOTIFICATION_ID = 1001
    const val TRANSCRIPTION_NOTIFICATION_ID = 1002
    
    // Service Actions
    const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
    const val ACTION_PAUSE_RECORDING = "ACTION_PAUSE_RECORDING"
    const val ACTION_RESUME_RECORDING = "ACTION_RESUME_RECORDING"
    const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
    
    // WorkManager
    const val WORK_TAG_TRANSCRIPTION = "transcription_work"
    const val WORK_TAG_SUMMARY = "summary_work"
    const val KEY_MEETING_ID = "meeting_id"
    
    // Meeting Status
    const val STATUS_RECORDING = "Recording"
    const val STATUS_PROCESSING = "Processing"
    const val STATUS_COMPLETED = "Completed"
    const val STATUS_ERROR = "Error"
    
    // Summary Prompt
    val SUMMARY_SYSTEM_PROMPT = """You are a helpful assistant that creates structured summaries of meeting transcripts.
        |Given a transcript, provide a JSON response with the following structure:
        |{
        |  "title": "A brief title for the meeting (max 60 characters)",
        |  "summary": "A concise summary of the main discussion points (2-3 paragraphs)",
        |  "actionItems": ["Action item 1", "Action item 2", ...],
        |  "keyPoints": ["Key point 1", "Key point 2", ...]
        |}
        |Ensure the response is valid JSON.""".trimMargin()
}

