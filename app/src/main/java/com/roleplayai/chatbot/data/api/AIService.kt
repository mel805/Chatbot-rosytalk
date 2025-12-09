package com.roleplayai.chatbot.data.api

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String = "gpt-3.5-turbo",
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    @SerializedName("temperature")
    val temperature: Float = 0.9f,
    @SerializedName("max_tokens")
    val maxTokens: Int = 500,
    @SerializedName("stream")
    val stream: Boolean = false
)

data class ChatMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("choices")
    val choices: List<Choice>,
    @SerializedName("created")
    val created: Long,
    @SerializedName("model")
    val model: String
)

data class Choice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: ChatMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

// For HuggingFace API
data class HFRequest(
    @SerializedName("inputs")
    val inputs: String,
    @SerializedName("parameters")
    val parameters: HFParameters = HFParameters()
)

data class HFParameters(
    @SerializedName("max_new_tokens")
    val maxNewTokens: Int = 500,
    @SerializedName("temperature")
    val temperature: Float = 0.9f,
    @SerializedName("top_p")
    val topP: Float = 0.95f,
    @SerializedName("return_full_text")
    val returnFullText: Boolean = false
)

data class HFResponse(
    @SerializedName("generated_text")
    val generatedText: String
)
