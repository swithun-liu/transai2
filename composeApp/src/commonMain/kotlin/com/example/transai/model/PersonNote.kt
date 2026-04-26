package com.example.transai.model

import kotlinx.serialization.Serializable

const val CURRENT_CHARACTER_STRATEGY_VERSION = 4

@Serializable
enum class CharacterRevealStage(val displayName: String) {
    CLUE("线索"),
    RESOLVED("已整理")
}

@Serializable
data class PersonMention(
    val paragraphId: Int,
    val surfaceForm: String,
    val contextSnippet: String
)

@Serializable
data class PersonResolution(
    val matchedCharacterId: String? = null,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val role: String,
    val surfaceForm: String,
    val confidence: Double = 0.0
)

@Serializable
data class CharacterConsolidation(
    val sourceCharacterIds: List<String>,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val role: String,
    val revealStage: CharacterRevealStage = CharacterRevealStage.RESOLVED
)

@Serializable
data class PersonNote(
    val id: String = "",
    val name: String,
    val role: String,
    val aliases: List<String> = emptyList(),
    val mentions: List<PersonMention> = emptyList(),
    val paragraphId: Int = mentions.firstOrNull()?.paragraphId ?: -1,
    val revealStage: CharacterRevealStage = CharacterRevealStage.RESOLVED
) {
    val mentionParagraphIds: List<Int>
        get() = mentions.map { it.paragraphId }.distinct().sorted()

    val firstMentionParagraphId: Int
        get() = mentionParagraphIds.firstOrNull() ?: paragraphId

    val latestMentionParagraphId: Int
        get() = mentionParagraphIds.lastOrNull() ?: paragraphId

    val mentionCount: Int
        get() = mentions.size

    val isClue: Boolean
        get() = revealStage == CharacterRevealStage.CLUE

    fun allNames(): List<String> {
        return (listOf(name) + aliases)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
}
