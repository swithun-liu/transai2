package com.example.transai.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterConsolidationSettings(
    val enableAutomaticConsolidation: Boolean = true,
    val triggerOnStrongEvidence: Boolean = true,
    val triggerOnConflict: Boolean = true
) {
    companion object {
        fun default(): CharacterConsolidationSettings = CharacterConsolidationSettings()
    }
}
