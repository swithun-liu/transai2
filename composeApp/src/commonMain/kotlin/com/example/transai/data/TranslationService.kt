package com.example.transai.data

import com.example.transai.model.PersonNote
import com.example.transai.model.PersonResolution
import com.example.transai.model.TranslationConfig
import com.example.transai.model.WordTranslation
import com.example.transai.platform.aiProxyEndpoint
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

    suspend fun resolvePersonNotes(
        text: String,
        existingCharacters: List<PersonNote>,
        config: TranslationConfig
    ): Result<List<PersonResolution>> {
        if (config.apiKey.isBlank()) {
            return Result.failure(Exception("Please set API Key in settings."))
        }
        try {
            val existingCharactersJson = json.encodeToString(
                ListSerializer(ExistingCharacterPayload.serializer()),
                existingCharacters.map { note ->
                    ExistingCharacterPayload(
                        id = note.id,
                        canonicalName = note.name,
                        aliases = note.aliases,
                        role = note.role
                    )
                }
            )
            val requestBody = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    Message(
                        "system",
                        """
                        You maintain a novel character encyclopedia.
                        Extract every explicit person/character mention in the paragraph, including names, full names, titles, identity labels, and nicknames when they clearly point to a person.
                        Compare each mention against the provided existing character list and merge when they are the same person.
                        Return only a JSON array. Each item must have:
                        - matchedCharacterId: existing character id when the mention should merge into an existing character, otherwise null
                        - canonicalName: best current display name for that person
                        - aliases: array of alternate names or titles for the same person
                        - role: concise Chinese role summary based on the paragraph
                        - surfaceForm: exact mention text from the paragraph
                        - confidence: number between 0 and 1
                        Rules:
                        - Prefer reusing an existing character when the mention is likely the same person.
                        - If a better full name appears later, keep matchedCharacterId and upgrade canonicalName.
                        - Do not invent characters that are not supported by the paragraph.
                        - If there are no people, return [].
                        """.trimIndent()
                    ),
                    Message(
                        "user",
                        """
                        Existing characters:
                        $existingCharactersJson

                        Paragraph:
                        $text
                        """.trimIndent()
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
                return Result.failure(Exception("Extraction failed: Empty response."))
            }
            val jsonText = extractJsonArray(content)
            val payloads = json.decodeFromString(ListSerializer(PersonResolutionPayload.serializer()), jsonText)
            val notes = payloads.mapNotNull { payload ->
                val name = payload.canonicalName.trim()
                val role = payload.role.trim()
                val surfaceForm = payload.surfaceForm.trim()
                if (name.isBlank() || role.isBlank() || surfaceForm.isBlank()) {
                    null
                } else {
                    PersonResolution(
                        matchedCharacterId = payload.matchedCharacterId?.trim().orEmpty().ifBlank { null },
                        canonicalName = name,
                        aliases = payload.aliases.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() },
                        role = role,
                        surfaceForm = surfaceForm,
                        confidence = payload.confidence?.coerceIn(0.0, 1.0) ?: 0.0
                    )
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
            aiProxyEndpoint()
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
data class ExistingCharacterPayload(
    val id: String,
    val canonicalName: String,
    val aliases: List<String>,
    val role: String
)

@Serializable
data class PersonResolutionPayload(
    val matchedCharacterId: String? = null,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val role: String,
    val surfaceForm: String,
    val confidence: Double? = null
)
