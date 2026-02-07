package com.example.transai.data

import com.example.transai.db.TransAIDatabase

object TranslationRepository {
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = TransAIDatabase(driver)
    private val queries = database.translationQueries

    fun getTranslation(bookPath: String, paragraphId: Int): String? {
        return queries.selectOne(bookPath, paragraphId.toLong()).executeAsOneOrNull()
    }

    fun saveTranslation(bookPath: String, paragraphId: Int, translation: String) {
        queries.insertOrReplace(bookPath, paragraphId.toLong(), translation)
    }
}
