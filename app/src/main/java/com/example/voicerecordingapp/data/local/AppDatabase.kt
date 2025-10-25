package com.example.voicerecordingapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voicerecordingapp.data.local.dao.AudioChunkDao
import com.example.voicerecordingapp.data.local.dao.MeetingDao
import com.example.voicerecordingapp.data.local.dao.SummaryDao
import com.example.voicerecordingapp.data.local.dao.TranscriptDao
import com.example.voicerecordingapp.data.local.entity.AudioChunkEntity
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import com.example.voicerecordingapp.data.local.entity.SummaryEntity
import com.example.voicerecordingapp.data.local.entity.TranscriptEntity

@Database(
    entities = [
        MeetingEntity::class,
        AudioChunkEntity::class,
        TranscriptEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
}

