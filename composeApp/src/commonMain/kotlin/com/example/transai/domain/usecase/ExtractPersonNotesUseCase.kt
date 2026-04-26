package com.example.transai.domain.usecase

import com.example.transai.data.TranslationService
import com.example.transai.model.PersonNote
import com.example.transai.model.PersonResolution
import com.example.transai.model.TranslationConfig

class ExtractPersonNotesUseCase(
    private val service: TranslationService = TranslationService()
) {
    suspend operator fun invoke(
        text: String,
        existingCharacters: List<PersonNote>,
        config: TranslationConfig
    ): Result<List<PersonResolution>> {
        return service.resolvePersonNotes(text, existingCharacters, config)
    }
}
