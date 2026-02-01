package com.example.transai.viewmodel

import com.example.transai.model.Paragraph
import com.example.transai.model.TranslationConfig

data class ChapterInfo(
    val title: String,
    val startIndex: Int
)

data class ReaderUiState(
    val paragraphs: List<Paragraph> = emptyList(),
    val chapters: List<ChapterInfo> = emptyList(),
    val config: TranslationConfig = TranslationConfig(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ReaderUiEvent {
    data class LoadFile(val path: String) : ReaderUiEvent
    data object LoadSample : ReaderUiEvent
    data class ToggleTranslation(val id: Int) : ReaderUiEvent
    data class UpdateConfig(val config: TranslationConfig) : ReaderUiEvent
}

sealed interface ReaderUiEffect {
    data class ShowToast(val message: String) : ReaderUiEffect
}
