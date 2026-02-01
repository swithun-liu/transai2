package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.domain.usecase.GetSettingsUseCase
import com.example.transai.domain.usecase.ParseBookUseCase
import com.example.transai.domain.usecase.TranslateParagraphUseCase
import com.example.transai.domain.usecase.UpdateSettingsUseCase
import com.example.transai.model.Paragraph
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
    private val parseBookUseCase: ParseBookUseCase = ParseBookUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        onEvent(ReaderUiEvent.LoadSample)
        observeSettings()
    }

    fun onEvent(event: ReaderUiEvent) {
        when (event) {
            is ReaderUiEvent.LoadFile -> loadFile(event.path)
            ReaderUiEvent.LoadSample -> loadSampleContent()
            is ReaderUiEvent.ToggleTranslation -> toggleTranslation(event.id)
            is ReaderUiEvent.UpdateConfig -> updateConfig(event)
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val book = parseBookUseCase(path)
                val allParagraphs = book.chapters.flatMap { chapter ->
                    chapter.paragraphs
                }
                
                val mappedParagraphs = allParagraphs.mapIndexed { index, text ->
                    Paragraph(id = index, originalText = text.trim())
                }
                
                _uiState.update { 
                    it.copy(
                        paragraphs = mappedParagraphs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error loading book: ${e.message}"
                    )
                }
                println("Error loading book: ${e.message}")
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
        val currentList = _uiState.value.paragraphs.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        
        if (index != -1) {
            val p = currentList[index]
            if (p.isExpanded) {
                // Collapse
                currentList[index] = p.copy(isExpanded = false)
                _uiState.update { it.copy(paragraphs = currentList) }
            } else {
                // Expand
                if (p.translatedText != null) {
                    // Already translated, just show
                    currentList[index] = p.copy(isExpanded = true)
                    _uiState.update { it.copy(paragraphs = currentList) }
                } else {
                    // Need translation (or retry if previous attempt failed)
                    currentList[index] = p.copy(isExpanded = true, isLoading = true, error = null)
                    _uiState.update { it.copy(paragraphs = currentList) }
                    translateParagraph(id, p.originalText)
                }
            }
        }
    }

    private fun translateParagraph(id: Int, text: String) {
        viewModelScope.launch {
            val config = _uiState.value.config
            val result = translateParagraphUseCase(text, config)
            
            _uiState.update { state ->
                val currentList = state.paragraphs.toMutableList()
                val index = currentList.indexOfFirst { it.id == id }
                if (index != -1) {
                    val updatedParagraph = if (result.isSuccess) {
                        currentList[index].copy(
                            translatedText = result.getOrNull(),
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
        }
    }

    private fun updateConfig(event: ReaderUiEvent.UpdateConfig) {
        updateSettingsUseCase(event.config)
    }
}
