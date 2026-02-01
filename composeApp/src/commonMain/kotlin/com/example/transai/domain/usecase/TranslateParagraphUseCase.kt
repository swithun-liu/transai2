package com.example.transai.domain.usecase

import com.example.transai.data.TranslationService
import com.example.transai.model.TranslationConfig

class TranslateParagraphUseCase(
    private val service: TranslationService = TranslationService()
) {
    suspend operator fun invoke(text: String, config: TranslationConfig): Result<String> {
        return service.translate(text, config)
    }
}
