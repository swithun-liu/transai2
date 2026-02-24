package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.data.PersonNoteRepository
import com.example.transai.domain.usecase.GetSettingsUseCase
import com.example.transai.domain.usecase.ParseBookUseCase
import com.example.transai.domain.usecase.TranslateParagraphUseCase
import com.example.transai.domain.usecase.TranslateWordUseCase
import com.example.transai.domain.usecase.UpdateSettingsUseCase
import com.example.transai.domain.usecase.GetBookMetadataUseCase
import com.example.transai.domain.usecase.UpdateBookProgressUseCase
import com.example.transai.domain.usecase.ExtractPersonNotesUseCase
import com.example.transai.data.TranslationRepository
import com.example.transai.data.WordTranslationRepository
import com.example.transai.model.Paragraph
import com.example.transai.model.PersonNote
import com.example.transai.model.TranslationConfig
import com.example.transai.platform.saveTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import transai.composeapp.generated.resources.Res

class ReaderViewModel(
    private val getSettingsUseCase: GetSettingsUseCase = GetSettingsUseCase(),
    private val updateSettingsUseCase: UpdateSettingsUseCase = UpdateSettingsUseCase(),
    private val translateParagraphUseCase: TranslateParagraphUseCase = TranslateParagraphUseCase(),
    private val translateWordUseCase: TranslateWordUseCase = TranslateWordUseCase(),
    private val extractPersonNotesUseCase: ExtractPersonNotesUseCase = ExtractPersonNotesUseCase(),
    private val parseBookUseCase: ParseBookUseCase = ParseBookUseCase(),
    private val getBookMetadataUseCase: GetBookMetadataUseCase = GetBookMetadataUseCase(),
    private val updateBookProgressUseCase: UpdateBookProgressUseCase = UpdateBookProgressUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    
    private var currentFilePath: String? = null

    init {
        observeSettings()
    }

    fun onEvent(event: ReaderUiEvent) {
        when (event) {
            is ReaderUiEvent.LoadFile -> loadFile(event.path)
            ReaderUiEvent.LoadSample -> loadSampleContent()
            is ReaderUiEvent.ToggleTranslation -> toggleTranslation(event.id)
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
        currentFilePath = path
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null,
                    paragraphs = emptyList(),
                    chapters = emptyList(),
                    personNotes = emptyList(),
                    initialScrollIndex = 0
                ) 
            }
            try {
                val metadata = getBookMetadataUseCase(path)
                val initialIndex = metadata?.lastReadPosition ?: 0

                val book = parseBookUseCase(path)
                
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
                        isExpanded = translationState?.isExpanded == 1L
                    )
                }
                val notes = PersonNoteRepository.getNotes(path)
                
                _uiState.update { 
                    it.copy(
                        paragraphs = mappedParagraphs,
                        chapters = chaptersInfo,
                        personNotes = notes,
                        isLoading = false,
                        initialScrollIndex = initialIndex
                    )
                }
            } catch (e: Exception) {
                println("ReaderViewModel: loadFile error=${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error loading book: ${e.message}"
                    )
                }
                e.printStackTrace()
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadSampleContent() {
        viewModelScope.launch(Dispatchers.IO) {
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
                viewModelScope.launch(Dispatchers.IO) {
                    TranslationRepository.updateExpansionState(path, id, false)
                }
            } else {
                // Expand
                if (p.translatedText != null) {
                    // Already translated, just show
                    currentList[index] = p.copy(isExpanded = true)
                    _uiState.update { it.copy(paragraphs = currentList) }
                    // Persist expand state
                    viewModelScope.launch(Dispatchers.IO) {
                        TranslationRepository.updateExpansionState(path, id, true)
                    }
                } else {
                    // Need translation (or retry if previous attempt failed)
                    currentList[index] = p.copy(isExpanded = true, isLoading = true, error = null)
                    _uiState.update { it.copy(paragraphs = currentList) }
                    translateParagraph(id, p.originalText)
                    // Note: translateParagraph will save the expansion state as true when it saves the translation
                }
            }
        }
    }

    private fun translateParagraph(id: Int, text: String) {
        val path = currentFilePath ?: return
        viewModelScope.launch {
            val config = _uiState.value.config
            val result = translateParagraphUseCase(text, config)
            
            _uiState.update { state ->
                val currentList = state.paragraphs.toMutableList()
                val index = currentList.indexOfFirst { it.id == id }
                if (index != -1) {
                    val updatedParagraph = if (result.isSuccess) {
                        val translated = result.getOrNull()
                        if (translated != null) {
                            TranslationRepository.saveTranslation(path, id, translated)
                        }
                        currentList[index].copy(
                            translatedText = translated,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        currentList[index].copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
                    currentList[index] = updatedParagraph
                }
                state.copy(paragraphs = currentList)
            }
            if (result.isSuccess) {
                viewModelScope.launch(Dispatchers.IO) {
                    extractAndSavePersonNotes(path, id, text, config)
                }
            }
        }
    }

    private fun updateConfig(event: ReaderUiEvent.UpdateConfig) {
        updateSettingsUseCase(event.config)
    }

    private fun saveProgress(index: Int) {
        val path = currentFilePath ?: return
        val total = _uiState.value.paragraphs.size
        if (total > 0) {
            viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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

    private suspend fun extractAndSavePersonNotes(path: String, paragraphId: Int, text: String, config: TranslationConfig) {
        val result = extractPersonNotesUseCase(text, config)
        val extracted = result.getOrNull().orEmpty()
        if (extracted.isEmpty()) return
        val added = mutableListOf<PersonNote>()
        extracted.forEach { note ->
            val normalized = normalizeName(note.name)
            if (normalized.isBlank()) return@forEach
            val inserted = PersonNoteRepository.insertIfAbsent(
                bookPath = path,
                nameKey = normalized,
                displayName = note.name.trim(),
                role = note.role.trim(),
                paragraphId = paragraphId
            )
            if (inserted) {
                added.add(note.copy(paragraphId = paragraphId))
            }
        }
        if (added.isNotEmpty()) {
            _uiState.update { state ->
                val merged = (state.personNotes + added)
                    .distinctBy { it.name.lowercase() }
                    .sortedBy { it.name.lowercase() }
                state.copy(personNotes = merged)
            }
        }
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    fun getApiKeyForProvider(provider: String): String {
        return getSettingsUseCase.getApiKeyForProvider(provider)
    }

    fun saveApiKeyForProvider(provider: String, apiKey: String) {
        updateSettingsUseCase.saveApiKeyForProvider(provider, apiKey)
    }
}
