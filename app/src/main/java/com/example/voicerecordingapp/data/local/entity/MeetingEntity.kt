package com.example.voicerecordingapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String, // Recording, Processing, Completed, Error
    val totalDuration: Long = 0, // in milliseconds
    val audioFilePath: String? = null
)

