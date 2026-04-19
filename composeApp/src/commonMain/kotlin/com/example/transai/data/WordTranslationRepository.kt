package com.example.transai.data

import com.example.transai.model.WordTranslation
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WordTranslationRepository {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    fun getTranslation(bookPath: String, paragraphId: Int, word: String): WordTranslation? {
        val raw = settings.getStringOrNull(cacheKey(bookPath, paragraphId, word)) ?: return null
        return runCatching {
            val cached = json.decodeFromString<CachedWordTranslation>(raw)
            WordTranslation(
                word = cached.word,
                translation = cached.translation,
                pronunciation = cached.pronunciation
            )
        }.getOrNull()
    }

    fun saveTranslation(
        bookPath: String,
        paragraphId: Int,
        word: String,
        context: String,
        translation: String,
        pronunciation: String
    ) {
        settings[cacheKey(bookPath, paragraphId, word)] = json.encodeToString(
            CachedWordTranslation(
                word = word,
                context = context,
                translation = translation,
                pronunciation = pronunciation
            )
        )
    }
}

@Serializable
private data class CachedWordTranslation(
    val word: String,
    val context: String,
    val translation: String,
    val pronunciation: String
)

private fun cacheKey(bookPath: String, paragraphId: Int, word: String): String {
    return "word_translation_${bookPath.hashCode()}_${paragraphId}_${word.hashCode()}"
}
