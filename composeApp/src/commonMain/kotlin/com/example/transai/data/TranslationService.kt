package com.example.transai.data

import com.example.transai.model.PersonNote
import com.example.transai.model.TranslationConfig
import com.example.transai.model.WordTranslation
import com.example.transai.platform.shouldUseAiProxy
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
import kotlinx.serialization.builtins.ListSerializer
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
            val endpoint = resolveChatCompletionEndpoint(config)

            val response: ChatCompletionResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (shouldUseAiProxy()) {
                    setBody(
                        ProxyChatCompletionRequest(
                            baseUrl = config.baseUrl.trimEnd('/'),
                            apiKey = config.apiKey,
                            model = config.model,
                            messages = requestBody.messages
                        )
                    )
                } else {
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(requestBody)
                }
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
            val endpoint = resolveChatCompletionEndpoint(config)

            val response: ChatCompletionResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (shouldUseAiProxy()) {
                    setBody(
                        ProxyChatCompletionRequest(
                            baseUrl = config.baseUrl.trimEnd('/'),
                            apiKey = config.apiKey,
                            model = config.model,
                            messages = requestBody.messages
                        )
                    )
                } else {
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(requestBody)
                }
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

    suspend fun extractPersonNotes(text: String, config: TranslationConfig): Result<List<PersonNote>> {
        if (config.apiKey.isBlank()) {
            return Result.failure(Exception("Please set API Key in settings."))
        }
        try {
            val requestBody = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(
                        "system",
                        "You are a literary assistant. Extract only character/person names mentioned in the paragraph. Return a JSON array of objects with fields: name, role. name must keep the original form from the text. role must be a concise Chinese description based on this paragraph. If none, return an empty array."
                    ),
                    Message("user", text)
                )
            )
            val endpoint = resolveChatCompletionEndpoint(config)
            val response: ChatCompletionResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (shouldUseAiProxy()) {
                    setBody(
                        ProxyChatCompletionRequest(
                            baseUrl = config.baseUrl.trimEnd('/'),
                            apiKey = config.apiKey,
                            model = config.model,
                            messages = requestBody.messages
                        )
                    )
                } else {
                    header("Authorization", "Bearer ${config.apiKey}")
                    setBody(requestBody)
                }
            }.body()
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(Exception("Extraction failed: Empty response."))
            }
            val jsonText = extractJsonArray(content)
            val payloads = json.decodeFromString(ListSerializer(PersonNotePayload.serializer()), jsonText)
            val notes = payloads.mapNotNull { payload ->
                val name = payload.name.trim()
                val role = payload.role.trim()
                if (name.isBlank() || role.isBlank()) {
                    null
                } else {
                    PersonNote(name = name, role = role, paragraphId = -1)
                }
            }
            return Result.success(notes)
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

    private fun extractJsonArray(text: String): String {
        val cleaned = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        return if (start != -1 && end != -1 && end > start) {
            cleaned.substring(start, end + 1)
        } else {
            cleaned
        }
    }

    private fun resolveChatCompletionEndpoint(config: TranslationConfig): String {
        return if (shouldUseAiProxy()) {
            "/api/chat/completions"
        } else {
            "${config.baseUrl.trimEnd('/')}/chat/completions"
        }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class ProxyChatCompletionRequest(
    val baseUrl: String,
    val apiKey: String,
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

@Serializable
data class PersonNotePayload(
    val name: String,
    val role: String
)
