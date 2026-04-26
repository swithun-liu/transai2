package com.example.transai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = CircleShape
                            )
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "调整模型、人物识别和自动整理策略。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            SettingsSection(
                title = "模型配置",
                description = "在不同服务商之间切换，并单独保存对应 API Key。"
            ) {
                ProviderChips(
                    selectedProvider = selectedProvider,
                    onSelect = { provider ->
                        viewModel.saveApiKeyForProvider(selectedProvider.name, apiKey)
                        selectedProvider = provider
                        model = provider.defaultModel
                        baseUrl = provider.defaultBaseUrl
                        apiKey = viewModel.getApiKeyForProvider(provider.name)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("模型名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isModelDropdownExpanded = true },
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = "切换",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            SettingsSection(
                title = "人物识别设置",
                description = "控制哪些称呼类型进入人物表，以及哪些类型在正文里高亮。"
            ) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            SettingsSection(
                title = "人物整理触发",
                description = "控制什么时候自动触发 AI 人物整理，手动整理不受这里影响。"
            ) {
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
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
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
            }

            val updatedConfig = TranslationConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                characterRecognitionSettings = characterRecognitionSettings,
                characterConsolidationSettings = characterConsolidationSettings
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                        Text("保存并返回")
                    }

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
                        Text("保存并重建人物")
                    }
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                shape = MaterialTheme.shapes.small
            )
            .padding(14.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                shape = MaterialTheme.shapes.small
            )
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
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
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ProviderChips(
    selectedProvider: ModelProvider,
    onSelect: (ModelProvider) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModelProvider.entries.chunked(2).forEach { rowProviders ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowProviders.forEach { provider ->
                    val selected = provider == selectedProvider
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onSelect(provider) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = provider.defaultModel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowProviders.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
