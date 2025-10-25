package com.example.voicerecordingapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId")]
)
data class AudioChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val chunkIndex: Int,
    val filePath: String,
    val duration: Long, // in milliseconds
    val uploadStatus: String, // Pending, Uploading, Uploaded, Failed
    val transcriptionStatus: String, // Pending, Transcribing, Completed, Failed
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

