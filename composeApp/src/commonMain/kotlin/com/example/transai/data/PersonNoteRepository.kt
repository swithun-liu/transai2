package com.example.transai.data

import com.example.transai.domain.character.CharacterRecognitionPolicy
import com.example.transai.model.CharacterConsolidation
import com.example.transai.model.CharacterStoreDebugEntry
import com.example.transai.model.CURRENT_CHARACTER_STRATEGY_VERSION
import com.example.transai.model.CharacterRecognitionSettings
import com.example.transai.model.CharacterRevealStage
import com.example.transai.model.PersonMention
import com.example.transai.model.PersonNote
import com.example.transai.model.PersonResolution
import com.example.transai.model.TranslationConfig
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PersonNoteRepository {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }
    @OptIn(ExperimentalSerializationApi::class)
    private val prettyJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun getNotes(bookPath: String, config: TranslationConfig? = null): List<PersonNote> {
        return loadStore(bookPath, config = config?.characterRecognitionSettings).notes
    }

    fun getStrategyVersion(bookPath: String): Int {
        return loadStore(bookPath).strategyVersion
    }

    fun rebuildForLatestStrategy(bookPath: String, config: TranslationConfig? = null): List<PersonNote> {
        return loadStore(
            bookPath,
            forceRebuild = true,
            config = config?.characterRecognitionSettings
        ).notes
    }

    fun getRawStoreJson(bookPath: String): String {
        val raw = settings.getStringOrNull(cacheKey(bookPath))
        return if (raw.isNullOrBlank()) "{}" else raw
    }

    fun getFormattedStoreJson(bookPath: String, config: TranslationConfig? = null): String {
        val snapshot = loadStore(bookPath, config = config?.characterRecognitionSettings)
        return prettyJson.encodeToString(snapshot)
    }

    fun getCharacterDebugEntries(bookPath: String, config: TranslationConfig? = null): List<CharacterStoreDebugEntry> {
        return loadStore(bookPath, config = config?.characterRecognitionSettings).notes.map { note ->
            CharacterStoreDebugEntry(
                characterId = note.id,
                displayName = note.name,
                mentionCount = note.mentionParagraphIds.size,
                formattedJson = prettyJson.encodeToString(note)
            )
        }
    }

    fun applyResolutions(
        bookPath: String,
        paragraphId: Int,
        paragraphText: String,
        resolutions: List<PersonResolution>,
        config: TranslationConfig
    ): List<PersonNote> {
        if (resolutions.isEmpty()) return getNotes(bookPath, config)
        val settings = config.characterRecognitionSettings
        val currentNotes = loadStore(bookPath, config = settings).notes.toMutableList()
        resolutions.forEach { resolution ->
            val mention = PersonMention(
                paragraphId = paragraphId,
                surfaceForm = resolution.surfaceForm.trim(),
                contextSnippet = buildContextSnippet(paragraphText, resolution.surfaceForm)
            )
            val existingIndex = findExistingCharacterIndex(currentNotes, resolution)
            if (existingIndex >= 0) {
                currentNotes[existingIndex] = mergePersonNote(currentNotes[existingIndex], resolution, mention, settings)
            } else {
                createPersonNote(resolution, mention, settings)?.let { currentNotes += it }
            }
        }
        val rebuilt = rebuildNotes(currentNotes, settings)
        saveStore(
            bookPath = bookPath,
            snapshot = CharacterStoreSnapshot(
                strategyVersion = CURRENT_CHARACTER_STRATEGY_VERSION,
                notes = rebuilt
            )
        )
        return rebuilt
    }

    fun applyConsolidations(
        bookPath: String,
        consolidations: List<CharacterConsolidation>,
        config: TranslationConfig
    ): List<PersonNote> {
        if (consolidations.isEmpty()) return getNotes(bookPath, config)
        val settings = config.characterRecognitionSettings
        val currentNotes = loadStore(bookPath, config = settings).notes
        val rebuilt = rebuildNotes(
            consolidateNotes(currentNotes, consolidations, settings),
            settings
        )
        saveStore(
            bookPath = bookPath,
            snapshot = CharacterStoreSnapshot(
                strategyVersion = CURRENT_CHARACTER_STRATEGY_VERSION,
                notes = rebuilt
            )
        )
        return rebuilt
    }

    private fun loadStore(
        bookPath: String,
        forceRebuild: Boolean = false,
        config: CharacterRecognitionSettings? = null
    ): CharacterStoreSnapshot {
        val raw = settings.getStringOrNull(cacheKey(bookPath))
        val decoded = decodeStoredSnapshot(raw)
            ?: decodePlainNotes(raw)?.let { notes ->
                CharacterStoreSnapshot(
                    strategyVersion = 1,
                    notes = notes
                )
            }
            ?: decodeLegacyNotes(raw)?.let(::legacySnapshotFromNotes)
            ?: legacySnapshotFromNotes(decodeLegacyNotes(settings.getStringOrNull(legacyCacheKey(bookPath))))
            ?: CharacterStoreSnapshot(
                strategyVersion = CURRENT_CHARACTER_STRATEGY_VERSION,
                notes = emptyList()
            )

        val rebuiltNotes = rebuildNotes(decoded.notes, config ?: CharacterRecognitionSettings.default())
        val needsUpgrade = forceRebuild ||
            decoded.strategyVersion < CURRENT_CHARACTER_STRATEGY_VERSION ||
            rebuiltNotes != decoded.notes

        val latestSnapshot = CharacterStoreSnapshot(
            strategyVersion = CURRENT_CHARACTER_STRATEGY_VERSION,
            notes = rebuiltNotes
        )
        if (needsUpgrade || raw == null) {
            saveStore(bookPath, latestSnapshot)
            return latestSnapshot
        }
        return decoded.copy(notes = rebuiltNotes.sortedBy { it.name.lowercase() })
    }

    private fun saveStore(bookPath: String, snapshot: CharacterStoreSnapshot) {
        settings[cacheKey(bookPath)] = json.encodeToString(snapshot)
    }

    private fun decodeStoredSnapshot(raw: String?): CharacterStoreSnapshot? {
        raw ?: return null
        return runCatching { json.decodeFromString<CharacterStoreSnapshot>(raw) }.getOrNull()
    }

    private fun decodePlainNotes(raw: String?): List<PersonNote>? {
        raw ?: return null
        return runCatching { json.decodeFromString<List<PersonNote>>(raw) }.getOrNull()
    }

    private fun decodeLegacyNotes(raw: String?): List<LegacyCachedPersonNote>? {
        raw ?: return null
        return runCatching { json.decodeFromString<List<LegacyCachedPersonNote>>(raw) }.getOrNull()
    }

    private fun legacySnapshotFromNotes(legacyNotes: List<LegacyCachedPersonNote>?): CharacterStoreSnapshot? {
        if (legacyNotes.isNullOrEmpty()) return null
        val notes = legacyNotes.map { legacy ->
            val mentions = if (legacy.paragraphId >= 0) {
                listOf(
                    PersonMention(
                        paragraphId = legacy.paragraphId,
                        surfaceForm = legacy.displayName,
                        contextSnippet = legacy.displayName
                    )
                )
            } else {
                emptyList()
            }
            PersonNote(
                id = legacy.nameKey.ifBlank { generatePersonId(legacy.displayName, legacy.paragraphId) },
                name = legacy.displayName,
                role = legacy.role,
                aliases = emptyList(),
                mentions = mentions,
                paragraphId = legacy.paragraphId
            )
        }
        return CharacterStoreSnapshot(
            strategyVersion = 0,
            notes = notes
        )
    }
}

private fun cacheKey(bookPath: String): String = "person_notes_${bookPath.hashCode()}"

private fun legacyCacheKey(bookPath: String): String = "person_notes_${bookPath.hashCode()}"

private fun rebuildNotes(
    notes: List<PersonNote>,
    settings: CharacterRecognitionSettings
): List<PersonNote> {
    return deduplicateCharacters(notes.map { sanitizePersonNote(it, settings) }, settings)
        .map { finalizePersonNote(it, settings) }
        .sortedBy { it.name.lowercase() }
}

private fun sanitizePersonNote(
    note: PersonNote,
    settings: CharacterRecognitionSettings
): PersonNote {
    val normalizedMentions = note.mentions
        .map { mention ->
            mention.copy(
                surfaceForm = mention.surfaceForm.trim(),
                contextSnippet = mention.contextSnippet.ifBlank { mention.surfaceForm.trim() }
            )
        }
        .filter { it.surfaceForm.isNotBlank() }
        .distinctBy { "${it.paragraphId}_${normalizeCharacterToken(it.surfaceForm)}" }
        .sortedBy { it.paragraphId }
    val ensuredMentions = if (normalizedMentions.isEmpty() && note.paragraphId >= 0) {
        listOf(
            PersonMention(
                paragraphId = note.paragraphId,
                surfaceForm = note.name.trim(),
                contextSnippet = note.name.trim()
            )
        )
    } else {
        normalizedMentions
    }
    val canonicalName = pickPreferredCanonicalName(note.name, note.aliases, ensuredMentions, settings)
    val aliases = (note.aliases + ensuredMentions.map { it.surfaceForm } + note.name)
        .map { it.trim() }
        .filter { alias ->
            alias.isNotBlank() &&
                !alias.equals(canonicalName, ignoreCase = true) &&
                CharacterRecognitionPolicy.shouldIncludeInCharacterList(alias, settings)
        }
        .distinctBy { it.lowercase() }
    val paragraphId = ensuredMentions.firstOrNull()?.paragraphId ?: note.paragraphId
    val sanitized = note.copy(
        id = note.id.ifBlank { generatePersonId(canonicalName, paragraphId) },
        name = canonicalName,
        role = note.role.trim(),
        aliases = aliases,
        mentions = ensuredMentions,
        paragraphId = paragraphId,
        revealStage = inferRevealStage(
            currentStage = note.revealStage,
            canonicalName = canonicalName,
            aliases = aliases,
            mentions = ensuredMentions
        )
    )
    return if (shouldKeepCharacter(sanitized, settings)) sanitized else sanitized.copy(aliases = emptyList())
}

private fun finalizePersonNote(
    note: PersonNote,
    settings: CharacterRecognitionSettings
): PersonNote {
    val mentions = note.mentions
        .map { mention ->
            mention.copy(
                surfaceForm = mention.surfaceForm.trim(),
                contextSnippet = mention.contextSnippet.ifBlank { mention.surfaceForm.trim() }
            )
        }
        .filter { it.surfaceForm.isNotBlank() }
        .distinctBy { "${it.paragraphId}_${normalizeCharacterToken(it.surfaceForm)}" }
        .sortedBy { it.paragraphId }
    val canonicalName = pickPreferredCanonicalName(note.name, note.aliases, mentions, settings)
    val aliases = (note.aliases + mentions.map { it.surfaceForm } + note.name)
        .map { it.trim() }
        .filter { alias ->
            alias.isNotBlank() &&
                !alias.equals(canonicalName, ignoreCase = true) &&
                CharacterRecognitionPolicy.shouldIncludeInCharacterList(alias, settings)
        }
        .distinctBy { it.lowercase() }
    val paragraphId = mentions.firstOrNull()?.paragraphId ?: note.paragraphId
    val finalized = note.copy(
        id = note.id.ifBlank { generatePersonId(canonicalName, paragraphId) },
        name = canonicalName,
        role = note.role.trim(),
        aliases = aliases,
        mentions = mentions,
        paragraphId = paragraphId,
        revealStage = inferRevealStage(
            currentStage = note.revealStage,
            canonicalName = canonicalName,
            aliases = aliases,
            mentions = mentions
        )
    )
    return finalized
}

private fun pickBestCanonicalName(
    currentName: String,
    aliases: List<String>,
    mentions: List<PersonMention>
): String {
    val candidates = buildList {
        add(currentName)
        addAll(aliases)
        addAll(mentions.map { it.surfaceForm })
    }
    return candidates.fold("") { best, candidate ->
        chooseBetterDisplayName(best, candidate)
    }
}

private fun pickPreferredCanonicalName(
    currentName: String,
    aliases: List<String>,
    mentions: List<PersonMention>,
    settings: CharacterRecognitionSettings
): String {
    val preferredCandidates = buildList {
        add(currentName)
        addAll(aliases)
        addAll(mentions.map { it.surfaceForm })
    }.filter { candidate ->
        candidate.isNotBlank() && CharacterRecognitionPolicy.shouldIncludeInCharacterList(candidate, settings)
    }
    if (preferredCandidates.isNotEmpty()) {
        return preferredCandidates.fold("") { best, candidate ->
            chooseBetterDisplayName(best, candidate)
        }
    }
    return pickBestCanonicalName(currentName, aliases, mentions)
}

private fun findExistingCharacterIndex(
    currentNotes: List<PersonNote>,
    resolution: PersonResolution
): Int {
    resolution.matchedCharacterId?.let { targetId ->
        val idMatch = currentNotes.indexOfFirst { it.id == targetId }
        if (idMatch >= 0) return idMatch
    }
    val normalizedNames = (listOf(resolution.canonicalName) + resolution.aliases + resolution.surfaceForm)
        .map(::normalizeCharacterToken)
        .filter { it.isNotBlank() }
        .toSet()
    return currentNotes.indexOfFirst { note ->
        note.allNames().any { normalizeCharacterToken(it) in normalizedNames }
    }
}

private fun createPersonNote(
    resolution: PersonResolution,
    mention: PersonMention,
    settings: CharacterRecognitionSettings
): PersonNote? {
    val bestName = pickPreferredCanonicalName(
        currentName = resolution.canonicalName,
        aliases = resolution.aliases,
        mentions = listOf(mention),
        settings = settings
    )
    val eligibleNames = (listOf(bestName, resolution.surfaceForm, resolution.canonicalName) + resolution.aliases)
        .map { it.trim() }
        .filter { it.isNotBlank() && CharacterRecognitionPolicy.shouldIncludeInCharacterList(it, settings) }
    if (eligibleNames.isEmpty()) return null
    val aliases = (resolution.aliases + resolution.surfaceForm)
        .map { it.trim() }
        .filter { alias ->
            alias.isNotBlank() &&
                !alias.equals(bestName, ignoreCase = true) &&
                CharacterRecognitionPolicy.shouldIncludeInCharacterList(alias, settings)
        }
        .distinctBy { it.lowercase() }
    return PersonNote(
        id = resolution.matchedCharacterId ?: generatePersonId(bestName, mention.paragraphId),
        name = bestName,
        role = resolution.role,
        aliases = aliases,
        mentions = listOf(mention),
        paragraphId = mention.paragraphId,
        revealStage = inferRevealStage(
            currentStage = CharacterRevealStage.CLUE,
            canonicalName = bestName,
            aliases = aliases,
            mentions = listOf(mention)
        )
    )
}

private fun mergePersonNote(
    existing: PersonNote,
    resolution: PersonResolution,
    mention: PersonMention,
    settings: CharacterRecognitionSettings
): PersonNote {
    val upgradedName = pickPreferredCanonicalName(
        currentName = chooseBetterDisplayName(existing.name, resolution.canonicalName),
        aliases = existing.aliases + resolution.aliases,
        mentions = existing.mentions + mention,
        settings = settings
    )
    val mergedAliases = (existing.aliases + resolution.aliases + resolution.surfaceForm + existing.name + resolution.canonicalName)
        .map { it.trim() }
        .filter { alias ->
            alias.isNotBlank() && CharacterRecognitionPolicy.shouldIncludeInCharacterList(alias, settings)
        }
        .distinctBy { it.lowercase() }
        .filter { !it.equals(upgradedName, ignoreCase = true) }
    val mergedMentions = (existing.mentions + mention)
        .distinctBy { "${it.paragraphId}_${normalizeCharacterToken(it.surfaceForm)}" }
        .sortedBy { it.paragraphId }
    return existing.copy(
        name = upgradedName,
        role = chooseBetterRole(existing.role, resolution.role),
        aliases = mergedAliases,
        mentions = mergedMentions,
        paragraphId = mergedMentions.firstOrNull()?.paragraphId ?: existing.paragraphId,
        revealStage = inferRevealStage(
            currentStage = existing.revealStage,
            canonicalName = upgradedName,
            aliases = mergedAliases,
            mentions = mergedMentions
        )
    )
}

private fun consolidateNotes(
    currentNotes: List<PersonNote>,
    consolidations: List<CharacterConsolidation>,
    settings: CharacterRecognitionSettings
): List<PersonNote> {
    val notesById = currentNotes.associateBy { it.id }
    val usedIds = mutableSetOf<String>()
    val consolidatedNotes = mutableListOf<PersonNote>()

    consolidations.forEach { consolidation ->
        val sourceNotes = consolidation.sourceCharacterIds
            .mapNotNull { notesById[it] }
            .distinctBy { it.id }
        if (sourceNotes.isEmpty()) return@forEach
        usedIds += sourceNotes.map { it.id }

        val mergedMentions = sourceNotes
            .flatMap { it.mentions }
            .distinctBy { "${it.paragraphId}_${normalizeCharacterToken(it.surfaceForm)}" }
            .sortedBy { it.paragraphId }
        val mergedAliases = (
            sourceNotes.flatMap { it.aliases } +
                sourceNotes.map { it.name } +
                consolidation.aliases +
                mergedMentions.map { it.surfaceForm }
            )
            .map { it.trim() }
            .filter {
                it.isNotBlank() &&
                    !it.equals(consolidation.canonicalName, ignoreCase = true) &&
                    CharacterRecognitionPolicy.shouldIncludeInCharacterList(it, settings)
            }
            .distinctBy { it.lowercase() }
        val role = sourceNotes
            .map { it.role }
            .plus(consolidation.role)
            .filter { it.isNotBlank() }
            .maxByOrNull { it.length }
            .orEmpty()
        val paragraphId = mergedMentions.firstOrNull()?.paragraphId ?: sourceNotes.minOfOrNull { it.paragraphId } ?: -1
        consolidatedNotes += PersonNote(
            id = sourceNotes.first().id,
            name = consolidation.canonicalName.trim(),
            role = role,
            aliases = mergedAliases,
            mentions = mergedMentions,
            paragraphId = paragraphId,
            revealStage = consolidation.revealStage
        )
    }

    currentNotes
        .filterNot { it.id in usedIds }
        .forEach { consolidatedNotes += it }

    return consolidatedNotes
}

private fun deduplicateCharacters(
    notes: List<PersonNote>,
    settings: CharacterRecognitionSettings
): List<PersonNote> {
    val merged = mutableListOf<PersonNote>()
    notes.forEach { note ->
        if (!shouldKeepCharacter(note, settings)) {
            return@forEach
        }
        val existingIndex = merged.indexOfFirst { existing ->
            existing.id == note.id || existing.allNames().any { existingName ->
                note.allNames().any { incomingName ->
                    normalizeCharacterToken(existingName) == normalizeCharacterToken(incomingName)
                }
            }
        }
        if (existingIndex >= 0) {
            val combinedResolution = PersonResolution(
                matchedCharacterId = merged[existingIndex].id,
                canonicalName = note.name,
                aliases = note.aliases,
                role = note.role,
                surfaceForm = note.name,
                confidence = 1.0
            )
            val seedMention = note.mentions.firstOrNull()
                ?: PersonMention(
                    paragraphId = note.paragraphId,
                    surfaceForm = note.name,
                    contextSnippet = note.name
                )
            val mergedNote = mergePersonNote(merged[existingIndex], combinedResolution, seedMention, settings)
                .copy(mentions = (merged[existingIndex].mentions + note.mentions)
                    .distinctBy { "${it.paragraphId}_${normalizeCharacterToken(it.surfaceForm)}" }
                    .sortedBy { it.paragraphId })
            merged[existingIndex] = mergedNote
        } else {
            merged += note
        }
    }
    return merged
}

private fun shouldKeepCharacter(
    note: PersonNote,
    settings: CharacterRecognitionSettings
): Boolean {
    val candidates = buildList {
        add(note.name)
        addAll(note.aliases)
        addAll(note.mentions.map { it.surfaceForm })
    }
    return candidates.any { alias ->
        alias.isNotBlank() && CharacterRecognitionPolicy.shouldIncludeInCharacterList(alias, settings)
    }
}

private fun inferRevealStage(
    currentStage: CharacterRevealStage,
    canonicalName: String,
    aliases: List<String>,
    mentions: List<PersonMention>
): CharacterRevealStage {
    if (currentStage == CharacterRevealStage.RESOLVED) return currentStage
    val candidates = buildList {
        add(canonicalName)
        addAll(aliases)
        addAll(mentions.map { it.surfaceForm })
    }
    return if (candidates.any(::looksLikeResolvedIdentity)) {
        CharacterRevealStage.RESOLVED
    } else {
        CharacterRevealStage.CLUE
    }
}

private fun looksLikeResolvedIdentity(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.startsWith("the ", ignoreCase = true)) return false
    if (trimmed.startsWith("a ", ignoreCase = true) || trimmed.startsWith("an ", ignoreCase = true)) return false
    val normalized = normalizeCharacterToken(trimmed)
    if (normalized in GENERIC_CHARACTER_TITLES) return false
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size >= 2) {
        return words.count { token ->
            val cleaned = token.trim(',', '.', ';', ':')
            cleaned.firstOrNull()?.isUpperCase() == true || cleaned.lowercase() in COMMON_HONORIFICS
        } >= 2
    }
    return trimmed.firstOrNull()?.isUpperCase() == true && trimmed.length >= 3
}

private fun chooseBetterDisplayName(current: String, candidate: String): String {
    val currentTrimmed = current.trim()
    val candidateTrimmed = candidate.trim()
    if (currentTrimmed.isBlank()) return candidateTrimmed
    if (candidateTrimmed.isBlank()) return currentTrimmed
    return if (displayNameScore(candidateTrimmed) > displayNameScore(currentTrimmed)) {
        candidateTrimmed
    } else {
        currentTrimmed
    }
}

private fun displayNameScore(name: String): Int {
    val trimmed = name.trim()
    var score = trimmed.length
    if (' ' in trimmed) score += 12
    if (trimmed.any { it.isUpperCase() }) score += 4
    if (trimmed.startsWith("the ", ignoreCase = true)) score -= 10
    if (trimmed.startsWith("a ", ignoreCase = true) || trimmed.startsWith("an ", ignoreCase = true)) score -= 10
    if (trimmed.lowercase() in GENERIC_CHARACTER_TITLES) score -= 20
    return score
}

private fun chooseBetterRole(current: String, candidate: String): String {
    val currentTrimmed = current.trim()
    val candidateTrimmed = candidate.trim()
    if (candidateTrimmed.isBlank()) return currentTrimmed
    if (currentTrimmed.isBlank()) return candidateTrimmed
    return if (candidateTrimmed.length > currentTrimmed.length) candidateTrimmed else currentTrimmed
}

private fun buildContextSnippet(paragraphText: String, surfaceForm: String): String {
    val trimmedSurface = surfaceForm.trim()
    if (trimmedSurface.isBlank()) return paragraphText.take(80)
    val index = paragraphText.indexOf(trimmedSurface, ignoreCase = true)
    if (index < 0) return paragraphText.take(80)
    val start = (index - 32).coerceAtLeast(0)
    val end = (index + trimmedSurface.length + 32).coerceAtMost(paragraphText.length)
    return paragraphText.substring(start, end).trim()
}

private fun normalizeCharacterToken(value: String): String {
    return value
        .trim()
        .lowercase()
        .replace(Regex("""^[^a-z0-9\u4e00-\u9fa5]+|[^a-z0-9\u4e00-\u9fa5]+$"""), "")
}

private fun generatePersonId(name: String, paragraphId: Int): String {
    val normalized = normalizeCharacterToken(name).ifBlank { "character" }
    return "character_${normalized.hashCode()}_${paragraphId.coerceAtLeast(0)}"
}

private val GENERIC_CHARACTER_TITLES = setOf(
    "he", "she", "him", "her", "man", "woman", "boy", "girl",
    "captain", "doctor", "professor", "teacher", "mother", "father"
)

private val COMMON_HONORIFICS = setOf(
    "mr", "mr.", "mrs", "mrs.", "miss", "ms", "ms.", "sir", "lady", "lord"
)

@Serializable
private data class CharacterStoreSnapshot(
    val strategyVersion: Int = CURRENT_CHARACTER_STRATEGY_VERSION,
    val notes: List<PersonNote> = emptyList()
)

@Serializable
private data class LegacyCachedPersonNote(
    val nameKey: String,
    val displayName: String,
    val role: String,
    val paragraphId: Int
)
