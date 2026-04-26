package com.example.transai.domain.character

import com.example.transai.model.CharacterMentionType
import com.example.transai.model.CharacterRecognitionSettings
import com.example.transai.model.PersonNote

object CharacterRecognitionPolicy {
    fun classify(text: String): CharacterMentionType {
        val trimmed = text.trim()
        val normalized = normalize(text)
        if (trimmed.isBlank()) return CharacterMentionType.DESCRIPTION
        if (normalized in PRONOUNS) return CharacterMentionType.PRONOUN
        if (looksLikeTitle(trimmed, normalized)) return CharacterMentionType.TITLE
        if (looksLikeName(trimmed)) return CharacterMentionType.NAME
        if (looksLikeNickname(trimmed)) return CharacterMentionType.NICKNAME
        if (looksLikeRoleLabel(normalized)) return CharacterMentionType.ROLE_LABEL
        return CharacterMentionType.DESCRIPTION
    }

    fun shouldIncludeInCharacterList(text: String, settings: CharacterRecognitionSettings): Boolean {
        val type = classify(text)
        return settings.settingFor(type).includeInCharacterList
    }

    fun shouldHighlight(text: String, settings: CharacterRecognitionSettings): Boolean {
        val type = classify(text)
        return settings.settingFor(type).highlightInText
    }

    fun highlightAliases(note: PersonNote, settings: CharacterRecognitionSettings): List<String> {
        return note.allNames()
            .filter { alias ->
                alias.isNotBlank() &&
                    shouldHighlight(alias, settings) &&
                    !isTooShortToHighlight(alias)
            }
            .distinctBy { normalize(it) }
    }

    private fun looksLikeTitle(trimmed: String, normalized: String): Boolean {
        if (normalized in TITLE_KEYWORDS) return true
        if (normalized.startsWith("the ")) {
            val remainder = normalized.removePrefix("the ").trim()
            if (remainder in TITLE_KEYWORDS || remainder in ROLE_LABEL_KEYWORDS) return true
        }
        val words = normalized.split(Regex("\\s+"))
        return words.any { it in TITLE_KEYWORDS }
    }

    private fun looksLikeName(trimmed: String): Boolean {
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size >= 2 && words.all(::startsWithUpperOrHonorific)) {
            return true
        }
        return false
    }

    private fun looksLikeNickname(trimmed: String): Boolean {
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size != 1) return false
        val word = words.single()
        if (word.length <= 2) return false
        val normalized = normalize(word)
        if (normalized in PRONOUNS || normalized in TITLE_KEYWORDS || normalized in ROLE_LABEL_KEYWORDS) return false
        return word.firstOrNull()?.isUpperCase() == true
    }

    private fun looksLikeRoleLabel(normalized: String): Boolean {
        if (normalized in ROLE_LABEL_KEYWORDS) return true
        if (normalized.startsWith("the ")) {
            val remainder = normalized.removePrefix("the ").trim()
            if (remainder in ROLE_LABEL_KEYWORDS) return true
        }
        return false
    }

    private fun startsWithUpperOrHonorific(word: String): Boolean {
        val normalized = normalize(word)
        return word.firstOrNull()?.isUpperCase() == true || normalized in TITLE_KEYWORDS
    }

    private fun isTooShortToHighlight(text: String): Boolean {
        return normalize(text).length <= 2
    }

    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace(Regex("""^[^a-z0-9\u4e00-\u9fa5]+|[^a-z0-9\u4e00-\u9fa5]+$"""), "")
    }

    private val PRONOUNS = setOf(
        "he", "she", "him", "her", "his", "hers",
        "they", "them", "their", "theirs",
        "one"
    )

    private val TITLE_KEYWORDS = setOf(
        "mr", "mr.", "mrs", "mrs.", "miss", "ms", "ms.", "sir", "lady", "lord",
        "count", "countess", "baron", "baroness", "duke", "duchess", "earl", "viscount",
        "marquis", "marquise", "prince", "princess", "king", "queen",
        "monsieur", "madame", "mademoiselle", "abbe", "abbé", "father", "sister", "brother",
        "captain", "major", "colonel", "general", "doctor", "dr", "dr.", "professor", "judge",
        "bishop", "priest", "procurator", "inspector"
    )

    private val ROLE_LABEL_KEYWORDS = setOf(
        "doctor", "judge", "procurator", "inspector", "captain", "major", "colonel",
        "general", "bishop", "priest", "sailor", "gentleman", "nurse", "lawyer", "magistrate"
    )
}
