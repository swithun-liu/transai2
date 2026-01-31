package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.data.SettingsRepository
import com.example.transai.data.TranslationService
import com.example.transai.data.parser.BookParserImpl
import com.example.transai.model.Paragraph
import com.example.transai.model.TranslationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {
    private val settingsRepository = SettingsRepository()
    private val translationService = TranslationService()
    private val bookParser = BookParserImpl()

    private val _paragraphs = MutableStateFlow<List<Paragraph>>(emptyList())
    val paragraphs: StateFlow<List<Paragraph>> = _paragraphs.asStateFlow()

    private val _config = MutableStateFlow(TranslationConfig())
    val config: StateFlow<TranslationConfig> = _config.asStateFlow()
    
    private val sampleText = """
        Call me Ishmael. Some years ago—never mind how long precisely—having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little and see the watery part of the world.
        
        It is a way I have of driving off the spleen and regulating the circulation. Whenever I find myself growing grim about the mouth; whenever it is a damp, drizzly November in my soul; whenever I find myself involuntarily pausing before coffin warehouses, and bringing up the rear of every funeral I meet; and especially whenever my hypos get such an upper hand of me, that it requires a strong moral principle to prevent me from deliberately stepping into the street, and methodically knocking people's hats off—then, I account it high time to get to sea as soon as I can.
        
        This is my substitute for pistol and ball. With a philosophical flourish Cato throws himself upon his sword; I quietly take to the ship. There is nothing surprising in this. If they but knew it, almost all men in their degree, some time or other, cherish very nearly the same feelings towards the ocean with me.
    """.trimIndent()

    init {
        loadSampleContent()
        viewModelScope.launch {
            settingsRepository.config.collectLatest {
                _config.value = it
            }
        }
    }

    fun loadFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val book = bookParser.parse(path)
                val allParagraphs = book.chapters.flatMap { chapter ->
                    chapter.paragraphs
                }
                
                val mappedParagraphs = allParagraphs.mapIndexed { index, text ->
                    Paragraph(id = index, originalText = text.trim())
                }
                
                _paragraphs.value = mappedParagraphs
            } catch (e: Exception) {
                // TODO: Handle error state in UI
                println("Error loading book: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadSampleContent() {
        val paras = sampleText.split("\n\n").mapIndexed { index, text ->
            Paragraph(id = index, originalText = text.trim())
        }
        _paragraphs.value = paras
    }

    fun toggleTranslation(id: Int) {
        val currentList = _paragraphs.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val p = currentList[index]
            if (p.isExpanded) {
                // Collapse
                currentList[index] = p.copy(isExpanded = false)
                _paragraphs.value = currentList
            } else {
                // Expand
                if (p.translatedText != null) {
                    // Already translated, just show
                    currentList[index] = p.copy(isExpanded = true)
                    _paragraphs.value = currentList
                } else {
                    // Need translation (or retry if previous attempt failed)
                    currentList[index] = p.copy(isExpanded = true, isLoading = true, error = null)
                    _paragraphs.value = currentList
                    translateParagraph(id, p.originalText)
                }
            }
        }
    }

    private fun translateParagraph(id: Int, text: String) {
        viewModelScope.launch {
            val result = translationService.translate(text, _config.value)
            
            val currentList = _paragraphs.value.toMutableList()
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
                _paragraphs.value = currentList
            }
        }
    }

    fun updateConfig(apiKey: String, baseUrl: String, model: String) {
        settingsRepository.saveConfig(TranslationConfig(apiKey, baseUrl, model))
    }
}
