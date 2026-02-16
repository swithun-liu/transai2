package com.example.transai.domain.usecase

import com.example.transai.data.TranslationService
import com.example.transai.model.TranslationConfig
import com.example.transai.model.WordTranslation

class TranslateWordUseCase(
    private val service: TranslationService = TranslationService()
) {
    suspend operator fun invoke(word: String, context: String, config: TranslationConfig): Result<WordTranslation> {
        return service.translateWord(word, context, config)
    }
}
