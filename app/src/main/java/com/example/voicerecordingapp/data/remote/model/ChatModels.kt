package com.example.voicerecordingapp.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")
    val model: String = "gpt-4o-mini",
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    @SerializedName("stream")
    val stream: Boolean = true,
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat? = ResponseFormat(type = "json_object")
)

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"
    @SerializedName("content")
    val content: String
)

data class ResponseFormat(
    @SerializedName("type")
    val type: String // "json_object" or "text"
)

data class ChatResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("created")
    val created: Long,
    @SerializedName("model")
    val model: String,
    @SerializedName("choices")
    val choices: List<ChatChoice>
)

data class ChatChoice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: ChatMessage? = null,
    @SerializedName("delta")
    val delta: ChatMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class SummaryData(
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>
)

