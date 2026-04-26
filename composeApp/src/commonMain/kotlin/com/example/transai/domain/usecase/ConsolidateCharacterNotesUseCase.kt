package com.example.transai.domain.usecase

import com.example.transai.data.TranslationService
import com.example.transai.model.CharacterConsolidation
import com.example.transai.model.PersonNote
import com.example.transai.model.TranslationConfig

class ConsolidateCharacterNotesUseCase(
    private val service: TranslationService = TranslationService()
) {
    suspend operator fun invoke(
        existingCharacters: List<PersonNote>,
        config: TranslationConfig
    ): Result<List<CharacterConsolidation>> {
        return service.consolidateCharacters(existingCharacters, config)
    }
}
