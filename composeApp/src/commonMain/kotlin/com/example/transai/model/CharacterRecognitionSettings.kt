package com.example.transai.model

import kotlinx.serialization.Serializable

@Serializable
enum class CharacterMentionType(val displayName: String) {
    NAME("正式姓名"),
    TITLE("头衔/爵位"),
    ROLE_LABEL("官职/身份称呼"),
    NICKNAME("昵称/简称"),
    DESCRIPTION("描述性称呼"),
    PRONOUN("代词")
}

@Serializable
data class CharacterMentionTypeSetting(
    val includeInCharacterList: Boolean,
    val highlightInText: Boolean
)

@Serializable
data class CharacterRecognitionSettings(
    val typeSettings: Map<CharacterMentionType, CharacterMentionTypeSetting> = defaultTypeSettings()
) {
    fun settingFor(type: CharacterMentionType): CharacterMentionTypeSetting {
        return typeSettings[type] ?: defaultSettingFor(type)
    }

    fun update(
        type: CharacterMentionType,
        includeInCharacterList: Boolean? = null,
        highlightInText: Boolean? = null
    ): CharacterRecognitionSettings {
        val current = settingFor(type)
        val next = current.copy(
            includeInCharacterList = includeInCharacterList ?: current.includeInCharacterList,
            highlightInText = highlightInText ?: current.highlightInText
        )
        return copy(typeSettings = typeSettings + (type to next))
    }

    companion object {
        fun default(): CharacterRecognitionSettings = CharacterRecognitionSettings()
    }
}

fun defaultSettingFor(type: CharacterMentionType): CharacterMentionTypeSetting {
    return when (type) {
        CharacterMentionType.NAME -> CharacterMentionTypeSetting(includeInCharacterList = true, highlightInText = true)
        CharacterMentionType.TITLE -> CharacterMentionTypeSetting(includeInCharacterList = true, highlightInText = true)
        CharacterMentionType.ROLE_LABEL -> CharacterMentionTypeSetting(includeInCharacterList = true, highlightInText = true)
        CharacterMentionType.NICKNAME -> CharacterMentionTypeSetting(includeInCharacterList = true, highlightInText = true)
        CharacterMentionType.DESCRIPTION -> CharacterMentionTypeSetting(includeInCharacterList = true, highlightInText = false)
        CharacterMentionType.PRONOUN -> CharacterMentionTypeSetting(includeInCharacterList = false, highlightInText = false)
    }
}

fun defaultTypeSettings(): Map<CharacterMentionType, CharacterMentionTypeSetting> {
    return CharacterMentionType.entries.associateWith(::defaultSettingFor)
}
