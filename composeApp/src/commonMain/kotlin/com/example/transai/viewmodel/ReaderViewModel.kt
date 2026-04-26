package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.data.BookRepository
import com.example.transai.data.PersonNoteRepository
import com.example.transai.domain.usecase.GetSettingsUseCase
import com.example.transai.domain.usecase.ParseBookUseCase
import com.example.transai.domain.usecase.TranslateParagraphUseCase
import com.example.transai.domain.usecase.TranslateWordUseCase
import com.example.transai.domain.usecase.UpdateSettingsUseCase
import com.example.transai.domain.usecase.GetBookMetadataUseCase
import com.example.transai.domain.usecase.UpdateBookProgressUseCase
import com.example.transai.domain.usecase.ConsolidateCharacterNotesUseCase
import com.example.transai.domain.usecase.ExtractPersonNotesUseCase
import com.example.transai.domain.character.CharacterConsolidationTriggerPolicy
import com.example.transai.data.TranslationRepository
import com.example.transai.data.WordTranslationRepository
import com.example.transai.model.Paragraph
import com.example.transai.model.PersonNote
import com.example.transai.model.TranslationConfig
import com.example.transai.platform.fileExists
import com.example.transai.platform.saveBookToSandbox
import com.example.transai.platform.saveTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import transai.composeapp.generated.resources.Res

class ReaderViewModel(
    private val getSettingsUseCase: GetSettingsUseCase = GetSettingsUseCase(),
    private val updateSettingsUseCase: UpdateSettingsUseCase = UpdateSettingsUseCase(),
    private val translateParagraphUseCase: TranslateParagraphUseCase = TranslateParagraphUseCase(),
    private val translateWordUseCase: TranslateWordUseCase = TranslateWordUseCase(),
    private val extractPersonNotesUseCase: ExtractPersonNotesUseCase = ExtractPersonNotesUseCase(),
    private val consolidateCharacterNotesUseCase: ConsolidateCharacterNotesUseCase = ConsolidateCharacterNotesUseCase(),
    private val parseBookUseCase: ParseBookUseCase = ParseBookUseCase(),
    private val getBookMetadataUseCase: GetBookMetadataUseCase = GetBookMetadataUseCase(),
    private val updateBookProgressUseCase: UpdateBookProgressUseCase = UpdateBookProgressUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    private var currentFilePath: String? = null
    private val paragraphTranslationMutex = Mutex()
    private var batchTranslationJob: Job? = null
    private var batchTargetParagraphId: Int = -1
    private val batchInitialCachedParagraphIds = mutableSetOf<Int>()
    private val batchRequestedParagraphIds = mutableSetOf<Int>()
    private val batchFailedParagraphIds = mutableSetOf<Int>()

    init {
        observeSettings()
    }

    fun onEvent(event: ReaderUiEvent) {
        when (event) {
            is ReaderUiEvent.LoadFile -> loadFile(event.path)
            ReaderUiEvent.LoadSample -> loadSampleContent()
            is ReaderUiEvent.ToggleTranslation -> toggleTranslation(event.id)
            is ReaderUiEvent.TranslateToParagraph -> translateToParagraph(event.id)
            is ReaderUiEvent.RevealParagraph -> revealParagraph(event.id)
            ReaderUiEvent.RefreshCharacterStoreDebug -> refreshCharacterStoreDebug()
            ReaderUiEvent.ConsolidateCharacters -> rebuildCharactersForCurrentBook(_uiState.value.config)
            is ReaderUiEvent.UpdateConfig -> updateConfig(event)
            is ReaderUiEvent.SaveProgress -> saveProgress(event.index)
            is ReaderUiEvent.SelectWord -> selectWord(event)
            ReaderUiEvent.DismissWordPopup -> dismissWordPopup()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    private fun loadFile(path: String) {
        viewModelScope.launch(Dispatchers.Default) {
            batchTranslationJob?.cancel()
            batchTranslationJob = null
            batchTargetParagraphId = -1
            batchInitialCachedParagraphIds.clear()
            batchRequestedParagraphIds.clear()
            batchFailedParagraphIds.clear()
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null,
                    paragraphs = emptyList(),
                    chapters = emptyList(),
                    personNotes = emptyList(),
                    currentBookPath = null,
                    characterStoreStrategyVersion = 0,
                    characterStoreRawJson = "{}",
                    characterStoreFormattedJson = "{}",
                    characterStoreEntries = emptyList(),
                    initialScrollIndex = 0,
                    batchTranslation = BatchTranslationState()
                ) 
            }
            try {
                val originalMetadata = getBookMetadataUseCase(path)
                val resolvedPath = resolveReadablePath(path, originalMetadata)
                currentFilePath = resolvedPath
                val metadata = getBookMetadataUseCase(resolvedPath) ?: originalMetadata
                val initialIndex = metadata?.lastReadPosition ?: 0

                val book = parseBookUseCase(resolvedPath)
                
                val chaptersInfo = mutableListOf<ChapterInfo>()
                var currentIndex = 0
                val allParagraphs = mutableListOf<String>()

                book.chapters.forEach { chapter ->
                    if (chapter.paragraphs.isNotEmpty()) {
                        chaptersInfo.add(ChapterInfo(chapter.title, currentIndex))
                        allParagraphs.addAll(chapter.paragraphs)
                        currentIndex += chapter.paragraphs.size
                    }
                }
                
                val mappedParagraphs = allParagraphs.mapIndexed { index, text ->
                    val translationState = TranslationRepository.getTranslationState(path, index)
                    Paragraph(
                        id = index, 
                        originalText = text.trim(),
                        translatedText = translationState?.translatedText,
                        isExpanded = translationState?.isExpanded == true
                    )
                }
                val currentConfig = _uiState.value.config
                val notes = PersonNoteRepository.getNotes(path, currentConfig)
                val strategyVersion = PersonNoteRepository.getStrategyVersion(path)
                val rawJson = PersonNoteRepository.getRawStoreJson(path)
                val formattedJson = PersonNoteRepository.getFormattedStoreJson(path, currentConfig)
                val debugEntries = PersonNoteRepository.getCharacterDebugEntries(path, currentConfig)
                
                _uiState.update { 
                    it.copy(
                        paragraphs = mappedParagraphs,
                        chapters = chaptersInfo,
                        personNotes = notes,
                        currentBookPath = resolvedPath,
                        characterStoreStrategyVersion = strategyVersion,
                        characterStoreRawJson = rawJson,
                        characterStoreFormattedJson = formattedJson,
                        characterStoreEntries = debugEntries,
                        isLoading = false,
                        initialScrollIndex = initialIndex
                    )
                }
            } catch (e: Exception) {
                println("ReaderViewModel: loadFile error=${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        currentBookPath = null,
                        error = "Error loading book: ${e.message}"
                    )
                }
                e.printStackTrace()
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun resolveReadablePath(
        requestedPath: String,
        metadata: com.example.transai.data.model.BookMetadata?
    ): String {
        if (fileExists(requestedPath)) {
            return requestedPath
        }

        val fileName = requestedPath.substringAfterLast('/')
        if (fileName == "sample.epub") {
            val bytes = Res.readBytes("files/sample.epub")
            val tempPath = saveTempFile("sample.epub", bytes)
            val savedPath = saveBookToSandbox(tempPath)
            BookRepository.replaceBookPath(
                oldPath = requestedPath,
                newPath = savedPath,
                title = metadata?.title ?: "Alice's Adventures in Wonderland",
                lastReadPosition = metadata?.lastReadPosition ?: 0,
                totalParagraphs = metadata?.totalParagraphs ?: 0
            )
            return savedPath
        }

        throw Exception("Book file is missing. Please re-import it.")
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadSampleContent() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bytes = Res.readBytes("files/sample.epub")
                val path = saveTempFile("sample.epub", bytes)
                loadFile(path)
            } catch (e: Exception) {
                println("Error loading sample content: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun toggleTranslation(id: Int) {
        val path = currentFilePath ?: return
        val currentList = _uiState.value.paragraphs.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        
        if (index != -1) {
            val p = currentList[index]
            val newExpandedState = !p.isExpanded
            
            if (p.isExpanded) {
                // Collapse
                currentList[index] = p.copy(isExpanded = false)
                _uiState.update { it.copy(paragraphs = currentList) }
                // Persist collapse state
                viewModelScope.launch(Dispatchers.Default) {
                    TranslationRepository.updateExpansionState(path, id, false)
                }
            } else {
                // Expand
                if (p.translatedText != null) {
                    // Already translated, just show
                    currentList[index] = p.copy(isExpanded = true)
                    _uiState.update { it.copy(paragraphs = currentList) }
                    // Persist expand state
                    viewModelScope.launch(Dispatchers.Default) {
                        TranslationRepository.updateExpansionState(path, id, true)
                    }
                } else {
                    // Need translation (or retry if previous attempt failed)
                    currentList[index] = p.copy(isExpanded = true, isLoading = true, error = null)
                    _uiState.update { it.copy(paragraphs = currentList) }
                    viewModelScope.launch(Dispatchers.Default) {
                        translateParagraphSerial(path, id, p.originalText, expandAfter = true)
                    }
                }
            }
        }
    }

    private fun translateToParagraph(id: Int) {
        val path = currentFilePath ?: return
        val targetId = id.coerceIn(0, _uiState.value.paragraphs.lastIndex)
        registerBatchCachedParagraphs(
            startIdInclusive = if (batchTranslationJob?.isActive == true) batchTargetParagraphId + 1 else 0,
            endIdInclusive = targetId
        )
        batchTargetParagraphId = maxOf(batchTargetParagraphId, targetId)
        expandTranslatedParagraphsUpTo(path, batchTargetParagraphId)
        updateBatchTranslationState(isRunning = true)

        if (batchTranslationJob?.isActive == true) {
            return
        }

        batchTranslationJob = viewModelScope.launch(Dispatchers.Default) {
            processBatchTranslationQueue(path)
        }
    }

    private fun revealParagraph(id: Int) {
        val path = currentFilePath ?: return
        val paragraph = _uiState.value.paragraphs.firstOrNull { it.id == id } ?: return
        if (paragraph.translatedText != null && !paragraph.isExpanded) {
            updateParagraphExpansion(id, true)
            viewModelScope.launch(Dispatchers.Default) {
                TranslationRepository.updateExpansionState(path, id, true)
            }
        }
    }

    private fun refreshCharacterStoreDebug() {
        val path = currentFilePath ?: return
        _uiState.update { state ->
            state.copy(
                characterStoreStrategyVersion = PersonNoteRepository.getStrategyVersion(path),
                characterStoreRawJson = PersonNoteRepository.getRawStoreJson(path),
                characterStoreFormattedJson = PersonNoteRepository.getFormattedStoreJson(path, state.config),
                characterStoreEntries = PersonNoteRepository.getCharacterDebugEntries(path, state.config)
            )
        }
    }

    private suspend fun processBatchTranslationQueue(path: String) {
        while (true) {
            val targetId = batchTargetParagraphId
            if (targetId < 0) break

            val nextParagraph = _uiState.value.paragraphs
                .firstOrNull { paragraph ->
                    paragraph.id <= targetId &&
                        paragraph.translatedText == null &&
                        paragraph.id !in batchRequestedParagraphIds &&
                        paragraph.id !in batchFailedParagraphIds
                }

            if (nextParagraph == null) {
                break
            }

            batchRequestedParagraphIds += nextParagraph.id
            updateBatchTranslationState(
                isRunning = true,
                currentParagraphId = nextParagraph.id,
                currentParagraphPreview = nextParagraph.originalText.take(48)
            )

            val result = translateParagraphSerial(
                path = path,
                id = nextParagraph.id,
                text = nextParagraph.originalText,
                expandAfter = true
            )
            if (result.isFailure) {
                batchFailedParagraphIds += nextParagraph.id
            }
            updateBatchTranslationState(isRunning = true)
            delay(150)
        }

        updateBatchTranslationState(
            isRunning = false,
            currentParagraphId = null,
            currentParagraphPreview = null
        )
        batchTargetParagraphId = -1
        batchInitialCachedParagraphIds.clear()
        batchRequestedParagraphIds.clear()
        batchFailedParagraphIds.clear()
    }

    private suspend fun translateParagraphSerial(
        path: String,
        id: Int,
        text: String,
        expandAfter: Boolean
    ): Result<String> {
        return paragraphTranslationMutex.withLock {
            val existing = _uiState.value.paragraphs.firstOrNull { it.id == id }
                ?: return@withLock Result.failure(Exception("Paragraph not found"))

            if (existing.translatedText != null) {
                if (expandAfter && !existing.isExpanded) {
                    updateParagraphExpansion(id, true)
                    TranslationRepository.updateExpansionState(path, id, true)
                }
                return@withLock Result.success(existing.translatedText)
            }

            updateParagraphLoading(id, expandAfter, true, null)
            val config = _uiState.value.config
            val result = translateParagraphUseCase(text, config)
            if (result.isSuccess) {
                val translated = result.getOrNull()
                if (translated != null) {
                    TranslationRepository.saveTranslation(path, id, translated)
                    updateParagraphTranslated(id, translated, expandAfter)
                    extractAndSavePersonNotes(path, id, text, config)
                    Result.success(translated)
                } else {
                    val error = "Translation failed: Empty response."
                    updateParagraphLoading(id, expandAfter, false, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                updateParagraphLoading(id, expandAfter, false, error)
                Result.failure(Exception(error))
            }
        }
    }

    private fun updateConfig(event: ReaderUiEvent.UpdateConfig) {
        updateSettingsUseCase(event.config)
        _uiState.update { it.copy(config = event.config) }
        if (event.rebuildCharacters) {
            rebuildCharactersForCurrentBook(event.config)
        }
    }

    private fun rebuildCharactersForCurrentBook(config: TranslationConfig) {
        val path = currentFilePath ?: return
        viewModelScope.launch(Dispatchers.Default) {
            var rebuiltCharacters = PersonNoteRepository.rebuildForLatestStrategy(path, config)
            val consolidation = consolidateCharacterNotesUseCase(rebuiltCharacters, config).getOrNull().orEmpty()
            if (consolidation.isNotEmpty()) {
                rebuiltCharacters = PersonNoteRepository.applyConsolidations(path, consolidation, config)
            }
            publishCharacterState(path, config, rebuiltCharacters)
        }
    }

    private fun saveProgress(index: Int) {
        val path = currentFilePath ?: return
        val total = _uiState.value.paragraphs.size
        if (total > 0) {
            viewModelScope.launch(Dispatchers.Default) {
                updateBookProgressUseCase(path, index, total)
            }
        }
    }

    private fun selectWord(event: ReaderUiEvent.SelectWord) {
        val path = currentFilePath ?: return
        val normalizedWord = event.word.trim()
        if (normalizedWord.isBlank()) return
        _uiState.update {
            it.copy(
                wordPopup = WordPopupState(
                    word = normalizedWord,
                    isLoading = true
                )
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            val cacheKey = normalizedWord.lowercase()
            val cached = WordTranslationRepository.getTranslation(path, event.paragraphId, cacheKey)
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        wordPopup = WordPopupState(
                            word = normalizedWord,
                            translation = cached.translation,
                            pronunciation = cached.pronunciation,
                            isLoading = false
                        )
                    )
                }
                return@launch
            }
            val config = _uiState.value.config
            val result = translateWordUseCase(normalizedWord, event.context, config)
            _uiState.update {
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        WordTranslationRepository.saveTranslation(
                            bookPath = path,
                            paragraphId = event.paragraphId,
                            word = cacheKey,
                            context = event.context,
                            translation = data.translation,
                            pronunciation = data.pronunciation
                        )
                    }
                    it.copy(
                        wordPopup = WordPopupState(
                            word = normalizedWord,
                            translation = data?.translation,
                            pronunciation = data?.pronunciation,
                            isLoading = false
                        )
                    )
                } else {
                    it.copy(
                        wordPopup = WordPopupState(
                            word = normalizedWord,
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

    private fun dismissWordPopup() {
        _uiState.update { it.copy(wordPopup = null) }
    }

    private fun updateParagraphExpansion(id: Int, isExpanded: Boolean) {
        _uiState.update { state ->
            val updated = state.paragraphs.map { paragraph ->
                if (paragraph.id == id) paragraph.copy(isExpanded = isExpanded) else paragraph
            }
            state.copy(paragraphs = updated)
        }
    }

    private fun updateParagraphLoading(id: Int, isExpanded: Boolean, isLoading: Boolean, error: String?) {
        _uiState.update { state ->
            val updated = state.paragraphs.map { paragraph ->
                if (paragraph.id == id) {
                    paragraph.copy(
                        isExpanded = isExpanded,
                        isLoading = isLoading,
                        error = error
                    )
                } else {
                    paragraph
                }
            }
            state.copy(paragraphs = updated)
        }
    }

    private fun updateParagraphTranslated(id: Int, translatedText: String, isExpanded: Boolean) {
        _uiState.update { state ->
            val updated = state.paragraphs.map { paragraph ->
                if (paragraph.id == id) {
                    paragraph.copy(
                        translatedText = translatedText,
                        isExpanded = isExpanded,
                        isLoading = false,
                        error = null
                    )
                } else {
                    paragraph
                }
            }
            state.copy(paragraphs = updated)
        }
    }

    private fun expandTranslatedParagraphsUpTo(path: String, targetId: Int) {
        val paragraphIdsToExpand = _uiState.value.paragraphs
            .filter { it.id <= targetId && it.translatedText != null && !it.isExpanded }
            .map { it.id }

        if (paragraphIdsToExpand.isEmpty()) return

        _uiState.update { state ->
            val updated = state.paragraphs.map { paragraph ->
                if (paragraph.id in paragraphIdsToExpand) {
                    paragraph.copy(isExpanded = true)
                } else {
                    paragraph
                }
            }
            state.copy(paragraphs = updated)
        }

        viewModelScope.launch(Dispatchers.Default) {
            paragraphIdsToExpand.forEach { paragraphId ->
                TranslationRepository.updateExpansionState(path, paragraphId, true)
            }
        }
    }

    private fun registerBatchCachedParagraphs(startIdInclusive: Int, endIdInclusive: Int) {
        if (startIdInclusive > endIdInclusive) return
        _uiState.value.paragraphs
            .filter { paragraph ->
                paragraph.id in startIdInclusive..endIdInclusive && paragraph.translatedText != null
            }
            .forEach { paragraph ->
                batchInitialCachedParagraphIds += paragraph.id
            }
    }

    private fun updateBatchTranslationState(
        isRunning: Boolean,
        currentParagraphId: Int? = _uiState.value.batchTranslation.currentParagraphId,
        currentParagraphPreview: String? = _uiState.value.batchTranslation.currentParagraphPreview
    ) {
        val targetId = batchTargetParagraphId
        if (targetId < 0) {
            _uiState.update { it.copy(batchTranslation = BatchTranslationState()) }
            return
        }

        val paragraphsInRange = _uiState.value.paragraphs.filter { it.id <= targetId }
        val successfulRequestedCount = batchRequestedParagraphIds.count { requestedId ->
            paragraphsInRange.any { paragraph ->
                paragraph.id == requestedId && paragraph.translatedText != null
            }
        }
        val processedCount = (
            batchInitialCachedParagraphIds.count { cachedId ->
                paragraphsInRange.any { paragraph -> paragraph.id == cachedId && paragraph.translatedText != null }
            } + successfulRequestedCount + batchFailedParagraphIds.size
            ).coerceAtMost(targetId + 1)

        _uiState.update { state ->
            state.copy(
                batchTranslation = BatchTranslationState(
                    isRunning = isRunning,
                    targetParagraphId = targetId,
                    totalCount = targetId + 1,
                    processedCount = processedCount,
                    cachedCount = batchInitialCachedParagraphIds.size.coerceAtMost(targetId + 1),
                    requestedCount = batchRequestedParagraphIds.size,
                    successCount = successfulRequestedCount,
                    failedCount = batchFailedParagraphIds.size,
                    currentParagraphId = currentParagraphId,
                    currentParagraphPreview = currentParagraphPreview
                )
            )
        }
    }

    private suspend fun extractAndSavePersonNotes(path: String, paragraphId: Int, text: String, config: TranslationConfig) {
        val currentCharacters = _uiState.value.personNotes
        val result = extractPersonNotesUseCase(text, currentCharacters, config)
        val resolutions = result.getOrNull().orEmpty()
        if (resolutions.isEmpty()) return
        var mergedCharacters = PersonNoteRepository.applyResolutions(
            bookPath = path,
            paragraphId = paragraphId,
            paragraphText = text,
            resolutions = resolutions,
            config = config
        )
        if (CharacterConsolidationTriggerPolicy.shouldTrigger(
                recentResolutions = resolutions,
                currentNotes = mergedCharacters,
                settings = config.characterConsolidationSettings
            )
        ) {
            val consolidations = consolidateCharacterNotesUseCase(mergedCharacters, config).getOrNull().orEmpty()
            if (consolidations.isNotEmpty()) {
                mergedCharacters = PersonNoteRepository.applyConsolidations(path, consolidations, config)
            }
        }
        publishCharacterState(path, config, mergedCharacters)
    }

    private fun publishCharacterState(path: String, config: TranslationConfig, personNotes: List<PersonNote>) {
        _uiState.update { state ->
            state.copy(
                personNotes = personNotes,
                characterStoreStrategyVersion = PersonNoteRepository.getStrategyVersion(path),
                characterStoreRawJson = PersonNoteRepository.getRawStoreJson(path),
                characterStoreFormattedJson = PersonNoteRepository.getFormattedStoreJson(path, config),
                characterStoreEntries = PersonNoteRepository.getCharacterDebugEntries(path, config)
            )
        }
    }

    fun getApiKeyForProvider(provider: String): String {
        return getSettingsUseCase.getApiKeyForProvider(provider)
    }

    fun saveApiKeyForProvider(provider: String, apiKey: String) {
        updateSettingsUseCase.saveApiKeyForProvider(provider, apiKey)
    }
}
