package com.example.voicerecordingapp.data.remote.api

import com.example.voicerecordingapp.data.remote.model.ChatRequest
import com.example.voicerecordingapp.data.remote.model.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

interface OpenAIApi {
    
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): Response<TranscriptionResponse>
    
    @Streaming
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatRequest
    ): Response<ResponseBody>
}

