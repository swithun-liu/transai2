package com.example.transai.viewmodel

import com.example.transai.model.Paragraph
import com.example.transai.model.PersonNote
import com.example.transai.model.TranslationConfig

data class ChapterInfo(
    val title: String,
    val startIndex: Int
)

data class ReaderUiState(
    val paragraphs: List<Paragraph> = emptyList(),
    val chapters: List<ChapterInfo> = emptyList(),
    val personNotes: List<PersonNote> = emptyList(),
    val config: TranslationConfig = TranslationConfig(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val initialScrollIndex: Int = 0,
    val wordPopup: WordPopupState? = null
)

data class WordPopupState(
    val word: String,
    val translation: String? = null,
    val pronunciation: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ReaderUiEvent {
    data class LoadFile(val path: String) : ReaderUiEvent
    data object LoadSample : ReaderUiEvent
    data class ToggleTranslation(val id: Int) : ReaderUiEvent
    data class UpdateConfig(val config: TranslationConfig) : ReaderUiEvent
    data class SaveProgress(val index: Int) : ReaderUiEvent
    data class SelectWord(val paragraphId: Int, val word: String, val context: String) : ReaderUiEvent
    data object DismissWordPopup : ReaderUiEvent
}

sealed interface ReaderUiEffect {
    data class ShowToast(val message: String) : ReaderUiEffect
}
