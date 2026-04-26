package com.example.transai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transai.model.CharacterMentionType
import com.example.transai.model.CharacterConsolidationSettings
import com.example.transai.model.CharacterRecognitionSettings
import com.example.transai.model.ModelProvider
import com.example.transai.model.TranslationConfig
import com.example.transai.viewmodel.ReaderUiEvent
import com.example.transai.viewmodel.ReaderViewModel

@Composable
fun SettingsScreen(viewModel: ReaderViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.config
    
    // Determine initial provider
    val initialProvider = remember(config.model) { 
        ModelProvider.fromModel(config.model) ?: ModelProvider.OpenAI 
    }
    
    // Track selected provider to manage API keys
    var selectedProvider by remember(config) { mutableStateOf(initialProvider) }
    
    // Local state for editing
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var model by remember(config) { mutableStateOf(config.model) }
    var characterRecognitionSettings by remember(config) {
        mutableStateOf(config.characterRecognitionSettings)
    }
    var characterConsolidationSettings by remember(config) {
        mutableStateOf(config.characterConsolidationSettings)
    }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { isModelDropdownExpanded = true }) {
                        Text("▼")
                    }
                }
            )
            
            DropdownMenu(
                expanded = isModelDropdownExpanded,
                onDismissRequest = { isModelDropdownExpanded = false }
            ) {
                ModelProvider.entries.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = provider.defaultModel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            // Save current apiKey for the current provider before switching
                            viewModel.saveApiKeyForProvider(selectedProvider.name, apiKey)
                            
                            selectedProvider = provider
                            model = provider.defaultModel
                            baseUrl = provider.defaultBaseUrl
                            apiKey = viewModel.getApiKeyForProvider(provider.name)
                            isModelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "人物识别设置",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "控制哪些称呼类型进入人物表，以及哪些类型在正文里高亮。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        CharacterMentionType.entries.forEach { type ->
            val typeSetting = characterRecognitionSettings.settingFor(type)
            CharacterMentionTypeSettingRow(
                type = type,
                includeInCharacterList = typeSetting.includeInCharacterList,
                highlightInText = typeSetting.highlightInText,
                onIncludeChanged = { enabled ->
                    characterRecognitionSettings = characterRecognitionSettings.update(
                        type = type,
                        includeInCharacterList = enabled
                    )
                },
                onHighlightChanged = { enabled ->
                    characterRecognitionSettings = characterRecognitionSettings.update(
                        type = type,
                        highlightInText = enabled
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "人物整理触发",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "控制什么时候自动触发 AI 人物整理。手动“重整人物”不受这里影响。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        ConsolidationTriggerSettingRow(
            title = "自动整理",
            description = "关闭后，只保留线索人物和手动重整。",
            checked = characterConsolidationSettings.enableAutomaticConsolidation,
            onCheckedChange = { enabled ->
                characterConsolidationSettings = characterConsolidationSettings.copy(
                    enableAutomaticConsolidation = enabled
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ConsolidationTriggerSettingRow(
            title = "强证据触发",
            description = "出现全名、昵称加姓氏、头衔加姓名等更明确信息时自动整理。",
            checked = characterConsolidationSettings.triggerOnStrongEvidence,
            enabled = characterConsolidationSettings.enableAutomaticConsolidation,
            onCheckedChange = { enabled ->
                characterConsolidationSettings = characterConsolidationSettings.copy(
                    triggerOnStrongEvidence = enabled
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ConsolidationTriggerSettingRow(
            title = "冲突触发",
            description = "人物表里出现明显疑似重复的线索人物时自动整理。",
            checked = characterConsolidationSettings.triggerOnConflict,
            enabled = characterConsolidationSettings.enableAutomaticConsolidation,
            onCheckedChange = { enabled ->
                characterConsolidationSettings = characterConsolidationSettings.copy(
                    triggerOnConflict = enabled
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        val updatedConfig = TranslationConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            characterRecognitionSettings = characterRecognitionSettings,
            characterConsolidationSettings = characterConsolidationSettings
        )

        Button(
            onClick = {
                viewModel.saveApiKeyForProvider(selectedProvider.name, apiKey)
                viewModel.onEvent(
                    ReaderUiEvent.UpdateConfig(
                        config = updatedConfig
                    )
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Back")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.saveApiKeyForProvider(selectedProvider.name, apiKey)
                viewModel.onEvent(
                    ReaderUiEvent.UpdateConfig(
                        config = updatedConfig,
                        rebuildCharacters = true
                    )
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save, Rebuild Characters & Back")
        }
    }
}

@Composable
private fun ConsolidationTriggerSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun CharacterMentionTypeSettingRow(
    type: CharacterMentionType,
    includeInCharacterList: Boolean,
    highlightInText: Boolean,
    onIncludeChanged: (Boolean) -> Unit,
    onHighlightChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "整理到人物表",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = includeInCharacterList,
                onCheckedChange = onIncludeChanged
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "正文高亮",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = highlightInText,
                onCheckedChange = onHighlightChanged
            )
        }
    }
}
