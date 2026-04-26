package com.example.transai.viewmodel

import com.example.transai.model.Paragraph
import com.example.transai.model.CharacterStoreDebugEntry
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
    val currentBookPath: String? = null,
    val characterStoreStrategyVersion: Int = 0,
    val characterStoreRawJson: String = "{}",
    val characterStoreFormattedJson: String = "{}",
    val characterStoreEntries: List<CharacterStoreDebugEntry> = emptyList(),
    val config: TranslationConfig = TranslationConfig(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val initialScrollIndex: Int = 0,
    val wordPopup: WordPopupState? = null,
    val batchTranslation: BatchTranslationState = BatchTranslationState()
)

data class BatchTranslationState(
    val isRunning: Boolean = false,
    val targetParagraphId: Int = -1,
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val cachedCount: Int = 0,
    val requestedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val currentParagraphId: Int? = null,
    val currentParagraphPreview: String? = null
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
    data class TranslateToParagraph(val id: Int) : ReaderUiEvent
    data class RevealParagraph(val id: Int) : ReaderUiEvent
    data object RefreshCharacterStoreDebug : ReaderUiEvent
    data class UpdateConfig(
        val config: TranslationConfig,
        val rebuildCharacters: Boolean = false
    ) : ReaderUiEvent
    data class SaveProgress(val index: Int) : ReaderUiEvent
    data class SelectWord(val paragraphId: Int, val word: String, val context: String) : ReaderUiEvent
    data object DismissWordPopup : ReaderUiEvent
}

sealed interface ReaderUiEffect {
    data class ShowToast(val message: String) : ReaderUiEffect
}
