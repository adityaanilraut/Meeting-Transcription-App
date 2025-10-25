package com.example.voicerecordingapp.data.remote.model

import com.google.gson.annotations.SerializedName

data class TranscriptionResponse(
    @SerializedName("text")
    val text: String
)

