package com.example.transai.data

import com.example.transai.model.TranslationConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TranslationService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            })
        }
    }

    suspend fun translate(text: String, config: TranslationConfig): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(Exception("Please set API Key in settings."))
        }
        
        try {
            val requestBody = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message("system", "You are a professional translator. Translate the following text into Chinese. Only output the translation."),
                    Message("user", text)
                )
            )

            // Normalize base URL
            val baseUrl = config.baseUrl.trimEnd('/')
            val endpoint = "$baseUrl/chat/completions"

            val response: ChatCompletionResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                setBody(requestBody)
            }.body()

            val content = response.choices.firstOrNull()?.message?.content?.trim()
            return if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Translation failed: Empty response."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)
