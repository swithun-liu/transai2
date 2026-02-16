package com.example.transai.data

import com.example.transai.model.TranslationConfig
import com.example.transai.model.WordTranslation
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
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    suspend fun translateWord(word: String, context: String, config: TranslationConfig): Result<WordTranslation> {
        if (config.apiKey.isBlank()) {
            return Result.failure(Exception("Please set API Key in settings."))
        }
        try {
            val requestBody = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(
                        "system",
                        "You are a bilingual dictionary assistant. Return a JSON object with fields: translation, pronunciation. Choose the most suitable meaning based on the given context. Keep it concise."
                    ),
                    Message(
                        "user",
                        "Word: $word\nContext: $context"
                    )
                )
            )

            val baseUrl = config.baseUrl.trimEnd('/')
            val endpoint = "$baseUrl/chat/completions"

            val response: ChatCompletionResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                setBody(requestBody)
            }.body()

            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(Exception("Translation failed: Empty response."))
            }
            val jsonText = extractJson(content)
            val payload = json.decodeFromString(WordTranslationPayload.serializer(), jsonText)
            return Result.success(
                WordTranslation(
                    word = word,
                    translation = payload.translation.trim(),
                    pronunciation = payload.pronunciation.trim()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    private fun extractJson(text: String): String {
        val cleaned = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            cleaned.substring(start, end + 1)
        } else {
            cleaned
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

@Serializable
data class WordTranslationPayload(
    val translation: String,
    val pronunciation: String
)
