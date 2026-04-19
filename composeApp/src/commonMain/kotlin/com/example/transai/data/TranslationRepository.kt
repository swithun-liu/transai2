package com.example.transai.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object TranslationRepository {
    private val settings = Settings()

    fun getTranslationState(bookPath: String, paragraphId: Int): TranslationState? {
        val translatedText = settings.getStringOrNull(translationKey(bookPath, paragraphId))
        val isExpanded = settings.getBoolean(expansionKey(bookPath, paragraphId), false)
        return if (translatedText == null && !isExpanded) {
            null
        } else {
            TranslationState(
                translatedText = translatedText,
                isExpanded = isExpanded
            )
        }
    }

    fun saveTranslation(bookPath: String, paragraphId: Int, translation: String) {
        settings[translationKey(bookPath, paragraphId)] = translation
        settings[expansionKey(bookPath, paragraphId)] = true
    }

    fun updateExpansionState(bookPath: String, paragraphId: Int, isExpanded: Boolean) {
        settings[expansionKey(bookPath, paragraphId)] = isExpanded
    }
}

data class TranslationState(
    val translatedText: String?,
    val isExpanded: Boolean
)

private fun translationKey(bookPath: String, paragraphId: Int): String {
    return "translation_value_${bookPath.cacheKeyPart()}_$paragraphId"
}

private fun expansionKey(bookPath: String, paragraphId: Int): String {
    return "translation_expanded_${bookPath.cacheKeyPart()}_$paragraphId"
}

private fun String.cacheKeyPart(): Int = hashCode()
