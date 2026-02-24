package com.example.transai.data

import com.example.transai.db.CharacterNote
import com.example.transai.db.TransAIDatabase
import com.example.transai.model.PersonNote

object PersonNoteRepository {
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = TransAIDatabase(driver)
    private val queries = database.characterNoteQueries

    fun getNotes(bookPath: String): List<PersonNote> {
        ensureTable()
        return queries.selectByBook(bookPath).executeAsList().map { it.toModel() }
    }

    fun exists(bookPath: String, nameKey: String): Boolean {
        ensureTable()
        return queries.selectOne(bookPath, nameKey).executeAsOneOrNull() != null
    }

    fun insertIfAbsent(bookPath: String, nameKey: String, displayName: String, role: String, paragraphId: Int): Boolean {
        ensureTable()
        if (exists(bookPath, nameKey)) return false
        queries.insertOrIgnore(bookPath, nameKey, displayName, role, paragraphId.toLong())
        return true
    }

    private fun ensureTable() {
        try {
            driver.execute(
                null,
                """
                    |CREATE TABLE IF NOT EXISTS characterNote (
                    |    bookPath TEXT NOT NULL,
                    |    nameKey TEXT NOT NULL,
                    |    displayName TEXT NOT NULL,
                    |    role TEXT NOT NULL,
                    |    paragraphId INTEGER NOT NULL,
                    |    PRIMARY KEY (bookPath, nameKey)
                    |)
                """.trimMargin(),
                0
            )
        } catch (e: Exception) {
        }
    }
}

private fun CharacterNote.toModel(): PersonNote {
    return PersonNote(
        name = displayName,
        role = role,
        paragraphId = paragraphId.toInt()
    )
}
