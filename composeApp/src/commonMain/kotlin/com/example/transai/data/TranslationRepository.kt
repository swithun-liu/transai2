package com.example.transai.data

import com.example.transai.db.TransAIDatabase

import com.example.transai.db.Translation

object TranslationRepository {
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = TransAIDatabase(driver)
    private val queries = database.translationQueries

    fun getTranslationState(bookPath: String, paragraphId: Int): Translation? {
        return queries.selectOne(bookPath, paragraphId.toLong()).executeAsOneOrNull()
    }

    fun saveTranslation(bookPath: String, paragraphId: Int, translation: String) {
        // When saving new translation, we assume user wants it expanded (default behavior)
        // or we preserve existing expanded state?
        // Let's check if it exists first to preserve state, or just default to true.
        // Usually if we just translated it, we want to show it.
        queries.insertOrReplace(bookPath, paragraphId.toLong(), translation, 1) // 1 = true
    }

    fun updateExpansionState(bookPath: String, paragraphId: Int, isExpanded: Boolean) {
        queries.updateExpansion(if (isExpanded) 1 else 0, bookPath, paragraphId.toLong())
    }
}
