package com.example.transai.data

import com.example.transai.model.PersonNote
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PersonNoteRepository {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    fun getNotes(bookPath: String): List<PersonNote> {
        return loadNotes(bookPath).map { it.toModel() }
    }

    fun exists(bookPath: String, nameKey: String): Boolean {
        return loadNotes(bookPath).any { it.nameKey == nameKey }
    }

    fun insertIfAbsent(bookPath: String, nameKey: String, displayName: String, role: String, paragraphId: Int): Boolean {
        val currentNotes = loadNotes(bookPath).toMutableList()
        if (currentNotes.any { it.nameKey == nameKey }) return false
        currentNotes += CachedPersonNote(
            nameKey = nameKey,
            displayName = displayName,
            role = role,
            paragraphId = paragraphId
        )
        saveNotes(bookPath, currentNotes)
        return true
    }

    private fun loadNotes(bookPath: String): List<CachedPersonNote> {
        val raw = settings.getStringOrNull(cacheKey(bookPath)) ?: return emptyList()
        return runCatching { json.decodeFromString<List<CachedPersonNote>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveNotes(bookPath: String, notes: List<CachedPersonNote>) {
        settings[cacheKey(bookPath)] = json.encodeToString(notes)
    }
}

@Serializable
private data class CachedPersonNote(
    val nameKey: String,
    val displayName: String,
    val role: String,
    val paragraphId: Int
)

private fun CachedPersonNote.toModel(): PersonNote {
    return PersonNote(
        name = displayName,
        role = role,
        paragraphId = paragraphId
    )
}

private fun cacheKey(bookPath: String): String = "person_notes_${bookPath.hashCode()}"
