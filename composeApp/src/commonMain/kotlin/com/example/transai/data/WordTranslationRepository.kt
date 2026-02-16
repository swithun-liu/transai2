package com.example.transai.data

import com.example.transai.db.TransAIDatabase
import com.example.transai.db.WordTranslation

object WordTranslationRepository {
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = TransAIDatabase(driver)
    private val queries = database.wordTranslationQueries

    fun getTranslation(bookPath: String, paragraphId: Int, word: String): WordTranslation? {
        return queries.selectOne(bookPath, paragraphId.toLong(), word).executeAsOneOrNull()
    }

    fun saveTranslation(
        bookPath: String,
        paragraphId: Int,
        word: String,
        context: String,
        translation: String,
        pronunciation: String
    ) {
        queries.insertOrReplace(bookPath, paragraphId.toLong(), word, context, translation, pronunciation)
    }
}
