package com.example.transai.domain.character

import com.example.transai.model.PersonNote
import com.example.transai.model.PersonResolution

object CharacterConsolidationTriggerPolicy {
    fun shouldTrigger(
        recentResolutions: List<PersonResolution>,
        currentNotes: List<PersonNote>
    ): Boolean {
        if (recentResolutions.isEmpty() || currentNotes.size < 2) return false
        return recentResolutions.any { resolution ->
            hasStrongEvidence(resolution, currentNotes) || existsPossibleConflictInCurrentCharacters(resolution, currentNotes)
        }
    }

    private fun hasStrongEvidence(
        resolution: PersonResolution,
        currentNotes: List<PersonNote>
    ): Boolean {
        val evidenceTexts = buildList {
            add(resolution.canonicalName)
            add(resolution.surfaceForm)
            addAll(resolution.aliases)
        }.filter { it.isNotBlank() }
        val normalizedEvidence = evidenceTexts.map(::normalize).filter { it.isNotBlank() }.toSet()

        return evidenceTexts.any { text ->
            (isFullName(text) || looksLikeNicknamePlusKnownFamilyName(text, currentNotes)) &&
                currentNotes.any { note ->
                    note.id != resolution.matchedCharacterId &&
                        note.allNames().map(::normalize).none { it in normalizedEvidence } &&
                        note.isClue &&
                        hasMeaningfulTokenOverlap(text, note.allNames())
                }
        }
    }

    private fun existsPossibleConflictInCurrentCharacters(
        resolution: PersonResolution,
        currentNotes: List<PersonNote>
    ): Boolean {
        val evidenceTexts = buildList {
            add(resolution.canonicalName)
            add(resolution.surfaceForm)
            addAll(resolution.aliases)
        }
            .map(::normalize)
            .filter { it.isNotBlank() }

        return currentNotes.any { note ->
            note.id != resolution.matchedCharacterId &&
                note.allNames().map(::normalize).none { it in evidenceTexts } &&
                note.allNames().any { existing ->
                    val normalizedExisting = normalize(existing)
                    evidenceTexts.any { candidate ->
                        hasMeaningfulTokenOverlap(candidate, listOf(normalizedExisting)) ||
                            candidate.contains(normalizedExisting) ||
                            normalizedExisting.contains(candidate)
                    }
                }
        }
    }

    private fun hasMeaningfulTokenOverlap(
        candidate: String,
        names: List<String>
    ): Boolean {
        val candidateTokens = tokenize(candidate)
        if (candidateTokens.isEmpty()) return false
        return names.any { name ->
            tokenize(name).intersect(candidateTokens).isNotEmpty()
        }
    }

    private fun isFullName(text: String): Boolean {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2) return false
        val honorifics = setOf("mr", "mr.", "mrs", "mrs.", "miss", "ms", "ms.", "sir", "lady", "lord")
        return words.count { word ->
            val trimmed = word.trim(',', '.', ';', ':')
            trimmed.firstOrNull()?.isUpperCase() == true || trimmed.lowercase() in honorifics
        } >= 2
    }

    private fun looksLikeNicknamePlusKnownFamilyName(
        mention: String,
        currentCharacters: List<PersonNote>
    ): Boolean {
        val words = mention.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2) return false
        val familyToken = words.last().trim(',', '.', ';', ':').lowercase()
        val nicknameHead = words.first().trim(',', '.', ';', ':')
        if (nicknameHead.firstOrNull()?.isUpperCase() != true || nicknameHead.length < 3) return false

        val knownFamilyTokens = currentCharacters
            .flatMap { it.allNames() }
            .flatMap { tokenize(it) }
            .toSet()

        return familyToken in knownFamilyTokens
    }

    private fun normalize(text: String): String {
        return tokenize(text).joinToString(" ")
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "young", "old", "big", "little",
            "girl", "boy", "man", "woman", "of", "and"
        )
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
            .toSet()
    }
}
