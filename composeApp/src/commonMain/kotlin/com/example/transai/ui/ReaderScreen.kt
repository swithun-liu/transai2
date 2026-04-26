package com.example.transai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.transai.model.Paragraph
import com.example.transai.model.CharacterStoreDebugEntry
import com.example.transai.model.CharacterRecognitionSettings
import com.example.transai.model.PersonMention
import com.example.transai.model.PersonNote
import com.example.transai.viewmodel.BatchTranslationState
import com.example.transai.viewmodel.ChapterInfo
import com.example.transai.viewmodel.ReaderUiEvent
import com.example.transai.viewmodel.ReaderViewModel
import com.example.transai.viewmodel.WordPopupState
import com.example.transai.domain.character.CharacterRecognitionPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: ReaderViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val paragraphCoordinates = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    var wordPopupAnchor by remember { mutableStateOf<WordPopupAnchor?>(null) }
    var wordPopupOffset by remember { mutableStateOf<IntOffset?>(null) }
    var showCharactersDialog by remember { mutableStateOf(false) }
    var showCharacterStoreDebugDialog by remember { mutableStateOf(false) }
    var focusedCharacterId by remember { mutableStateOf<String?>(null) }
    var highlightedParagraphId by remember { mutableStateOf<Int?>(null) }
    var showBatchTranslationDialog by remember { mutableStateOf(false) }

    rememberPopupOffset(
        anchor = wordPopupAnchor,
        paragraphCoordinates = paragraphCoordinates,
        listState = listState,
        density = density
    ) { wordPopupOffset = it }

    // Restore scroll position
    LaunchedEffect(uiState.initialScrollIndex) {
        if (uiState.paragraphs.isNotEmpty() && uiState.initialScrollIndex > 0) {
            listState.scrollToItem(uiState.initialScrollIndex)
        }
    }

    // Save progress
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        if (uiState.paragraphs.isNotEmpty()) {
            delay(500)
            viewModel.onEvent(ReaderUiEvent.SaveProgress(firstVisibleIndex))
        }
    }

    LaunchedEffect(highlightedParagraphId) {
        if (highlightedParagraphId != null) {
            delay(2500)
            highlightedParagraphId = null
        }
    }

    // Save on exit
    DisposableEffect(Unit) {
        onDispose {
            val currentIndex = listState.firstVisibleItemIndex
            if (uiState.paragraphs.isNotEmpty() && currentIndex > 0) {
                 viewModel.onEvent(ReaderUiEvent.SaveProgress(currentIndex))
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn {
                    item {
                        Text(
                            "Table of Contents",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    item {
                        HorizontalDivider()
                    }
                    items(uiState.chapters) { chapter ->
                        NavigationDrawerItem(
                            label = { Text(chapter.title) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(chapter.startIndex)
                                    drawerState.close()
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reader") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showBatchTranslationDialog = true }) {
                            Text(
                                if (uiState.batchTranslation.isRunning && uiState.batchTranslation.totalCount > 0) {
                                    "${uiState.batchTranslation.processedCount}/${uiState.batchTranslation.totalCount}"
                                } else {
                                    "进度"
                                }
                            )
                        }
                        IconButton(
                            onClick = {
                                focusedCharacterId = null
                                showCharactersDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Characters")
                        }
                        TextButton(
                            onClick = {
                                viewModel.onEvent(ReaderUiEvent.RefreshCharacterStoreDebug)
                                showCharacterStoreDebugDialog = true
                            }
                        ) {
                            Text("Debug")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (uiState.isLoading && uiState.paragraphs.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null && uiState.paragraphs.isEmpty()) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(
                            items = uiState.paragraphs,
                            key = { it.id }
                        ) { paragraph ->
                            ParagraphItem(
                                paragraph = paragraph,
                                personNotes = uiState.personNotes,
                                characterRecognitionSettings = uiState.config.characterRecognitionSettings,
                                onToggle = { viewModel.onEvent(ReaderUiEvent.ToggleTranslation(paragraph.id)) },
                                onTranslateToHere = {
                                    showBatchTranslationDialog = true
                                    viewModel.onEvent(ReaderUiEvent.TranslateToParagraph(paragraph.id))
                                },
                                onWordTap = { word, localOffset ->
                                    wordPopupAnchor = WordPopupAnchor(paragraph.id, localOffset)
                                    viewModel.onEvent(
                                        ReaderUiEvent.SelectWord(
                                            paragraphId = paragraph.id,
                                            word = word,
                                            context = paragraph.originalText
                                        )
                                    )
                                },
                                onCharacterTap = { note ->
                                    focusedCharacterId = note.id
                                    showCharactersDialog = true
                                },
                                isHighlighted = highlightedParagraphId == paragraph.id,
                                onCoordinatesChanged = { coordinates ->
                                    if (coordinates == null) {
                                        paragraphCoordinates.remove(paragraph.id)
                                    } else {
                                        paragraphCoordinates[paragraph.id] = coordinates
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }

                    // FAB to open drawer
                    FloatingActionButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }

                    WordTranslationDialog(
                        state = uiState.wordPopup,
                        offset = wordPopupOffset,
                        onDismiss = {
                            wordPopupAnchor = null
                            wordPopupOffset = null
                            viewModel.onEvent(ReaderUiEvent.DismissWordPopup)
                        }
                    )

                    if (showCharactersDialog) {
                        CharactersDialog(
                            personNotes = uiState.personNotes,
                            chapters = uiState.chapters,
                            focusedCharacterId = focusedCharacterId,
                            onDismiss = { showCharactersDialog = false },
                            onSelectCharacter = { focusedCharacterId = it.id },
                            onOpenDebug = {
                                viewModel.onEvent(ReaderUiEvent.RefreshCharacterStoreDebug)
                                showCharacterStoreDebugDialog = true
                            },
                            onJumpToParagraph = { paragraphId ->
                                focusedCharacterId = uiState.personNotes.firstOrNull {
                                    it.mentionParagraphIds.contains(paragraphId)
                                }?.id ?: focusedCharacterId
                                viewModel.onEvent(ReaderUiEvent.RevealParagraph(paragraphId))
                                highlightedParagraphId = paragraphId
                                scope.launch {
                                    listState.animateScrollToItem(paragraphId)
                                }
                                showCharactersDialog = false
                            }
                        )
                    }

                    if (showCharacterStoreDebugDialog) {
                        CharacterStoreDebugDialog(
                            strategyVersion = uiState.characterStoreStrategyVersion,
                            formattedJson = uiState.characterStoreFormattedJson,
                            entries = uiState.characterStoreEntries,
                            onRefresh = { viewModel.onEvent(ReaderUiEvent.RefreshCharacterStoreDebug) },
                            onDismiss = { showCharacterStoreDebugDialog = false }
                        )
                    }

                    if (showBatchTranslationDialog) {
                        BatchTranslationDialog(
                            state = uiState.batchTranslation,
                            onDismiss = { showBatchTranslationDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPopupOffset(
    anchor: WordPopupAnchor?,
    paragraphCoordinates: Map<Int, LayoutCoordinates>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    density: androidx.compose.ui.unit.Density,
    update: (IntOffset?) -> Unit
) {
    LaunchedEffect(anchor) {
        if (anchor == null) {
            update(null)
            return@LaunchedEffect
        }
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                val coordinates = paragraphCoordinates[anchor.paragraphId]
                if (coordinates != null) {
                    val root = coordinates.localToRoot(anchor.localOffset)
                    val verticalOffset = with(density) { 48.dp.toPx() }
                    update(
                        IntOffset(
                            root.x.toInt(),
                            (root.y - verticalOffset).coerceAtLeast(0f).toInt()
                        )
                    )
                }
            }
    }
}

@Composable
fun ParagraphItem(
    paragraph: Paragraph,
    personNotes: List<PersonNote>,
    characterRecognitionSettings: CharacterRecognitionSettings,
    onToggle: () -> Unit,
    onTranslateToHere: () -> Unit,
    onWordTap: (String, Offset) -> Unit,
    onCharacterTap: (PersonNote) -> Unit,
    isHighlighted: Boolean,
    onCoordinatesChanged: (LayoutCoordinates?) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotatedText = remember(paragraph.originalText, personNotes, characterRecognitionSettings) {
        buildHighlightedParagraphText(paragraph.originalText, personNotes, characterRecognitionSettings)
    }
    DisposableEffect(Unit) {
        onDispose { onCoordinatesChanged(null) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.medium
            )
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .pointerInput(annotatedText) {
                            detectTapGestures { position ->
                                val result = layoutResult ?: return@detectTapGestures
                                val offset = result.getOffsetForPosition(position)
                                val characterNote = annotatedText
                                    .getStringAnnotations(
                                        tag = CHARACTER_ANNOTATION_TAG,
                                        start = offset,
                                        end = (offset + 1).coerceAtMost(annotatedText.length)
                                    )
                                    .firstOrNull()
                                    ?.let { annotation ->
                                        personNotes.firstOrNull {
                                            it.id == annotation.item
                                        }
                                    }
                                if (characterNote != null) {
                                    onCharacterTap(characterNote)
                                    return@detectTapGestures
                                }
                                val word = findWordAt(paragraph.originalText, offset)
                                if (word != null) {
                                    onWordTap(word, position)
                                }
                            }
                        }
                    .onGloballyPositioned { onCoordinatesChanged(it) },
                    onTextLayout = { layoutResult = it }
                )
            }
            
            // Show icon if translation is cached but not expanded
            if (paragraph.translatedText != null && !paragraph.isExpanded) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Cached translation available",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 8.dp, top = 4.dp)
                        .width(16.dp)
                        .height(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onTranslateToHere) {
                Text("翻译到此")
            }
            TextButton(onClick = onToggle) {
                Text(if (paragraph.isExpanded) "Hide" else "Translate")
            }
        }

        AnimatedVisibility(
            visible = paragraph.isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                if (paragraph.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Translating...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (paragraph.error != null) {
                    Text(
                        text = paragraph.error,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                } else {
                    Text(
                        text = paragraph.translatedText ?: "Translation not available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

data class WordPopupAnchor(
    val paragraphId: Int,
    val localOffset: Offset
)

@Composable
fun WordTranslationDialog(state: WordPopupState?, offset: IntOffset?, onDismiss: () -> Unit) {
    if (state == null || offset == null) return
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            Text(
                text = state.word,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中...")
                }
            } else if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error
                    )
                )
            } else {
                Text("释义：${state.translation ?: "-"}")
                Spacer(modifier = Modifier.height(6.dp))
                Text("读音：${state.pronunciation ?: "-"}")
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}

fun findWordAt(text: String, offset: Int): String? {
    if (text.isBlank()) return null
    val index = offset.coerceIn(0, text.length - 1)
    val initialChar = text[index]
    val startIndex = if (isWordChar(initialChar)) index else index - 1
    if (startIndex < 0 || !isWordChar(text[startIndex])) return null
    var start = startIndex
    var end = startIndex
    while (start > 0 && isWordChar(text[start - 1])) {
        start--
    }
    while (end < text.length - 1 && isWordChar(text[end + 1])) {
        end++
    }
    return text.substring(start, end + 1)
}

fun isWordChar(char: Char): Boolean {
    return char.isLetterOrDigit() || char == '\'' || char == '-'
}

@Composable
fun CharactersDialog(
    personNotes: List<PersonNote>,
    chapters: List<ChapterInfo>,
    focusedCharacterId: String?,
    onDismiss: () -> Unit,
    onSelectCharacter: (PersonNote) -> Unit,
    onOpenDebug: () -> Unit,
    onJumpToParagraph: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedCharacter = remember(personNotes, focusedCharacterId) {
        personNotes.firstOrNull { it.id == focusedCharacterId } ?: personNotes.firstOrNull()
    }
    val focusedIndex = remember(personNotes, selectedCharacter?.id) {
        val selectedId = selectedCharacter?.id ?: return@remember -1
        personNotes.indexOfFirst { it.id == selectedId }
    }
    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0) {
            listState.scrollToItem(focusedIndex)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Characters") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (personNotes.isEmpty()) {
                    Text(
                        "暂无人物笔记",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        itemsIndexed(personNotes) { index, note ->
                            val isFocused = index == focusedIndex
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCharacter(note) }
                                    .background(
                                        color = if (isFocused) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(note.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${note.mentionParagraphIds.size} 段",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    note.revealStage.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (note.isClue) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    note.role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                    selectedCharacter?.let { character ->
                        Spacer(modifier = Modifier.height(12.dp))
                        CharacterDetailContent(
                            character = character,
                            chapters = chapters,
                            onJumpToParagraph = onJumpToParagraph
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onOpenDebug) {
                        Text("Debug JSON")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun CharacterStoreDebugDialog(
    strategyVersion: Int,
    formattedJson: String,
    entries: List<CharacterStoreDebugEntry>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedCharacterId by remember(entries) { mutableStateOf(entries.firstOrNull()?.characterId) }
    val selectedEntry = remember(entries, selectedCharacterId) {
        entries.firstOrNull { it.characterId == selectedCharacterId } ?: entries.firstOrNull()
    }
    val copyTargetJson = selectedEntry?.formattedJson ?: formattedJson
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Character Store Debug") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "策略版本：$strategyVersion",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "按人物查看",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (entries.isEmpty()) {
                    Text(
                        "当前没有人物存储数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                    ) {
                        items(entries) { entry ->
                            val isSelected = entry.characterId == selectedEntry?.characterId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCharacterId = entry.characterId }
                                    .background(
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${entry.mentionCount} 段",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        selectedEntry?.let { "人物 JSON: ${it.displayName}" } ?: "人物 JSON",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            item {
                                Text(
                                    selectedEntry?.formattedJson ?: "{}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "整份快照 JSON",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                    ) {
                        item {
                            Text(
                                formattedJson,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(copyTargetJson))
                        }
                    ) {
                        Text(if (selectedEntry != null) "复制当前人物 JSON" else "复制当前 JSON")
                    }
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(formattedJson))
                        }
                    ) {
                        Text("复制完整 JSON")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRefresh) {
                        Text("刷新")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    )
}

@Composable
fun BatchTranslationDialog(
    state: BatchTranslationState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("翻译进度") },
        text = {
            if (state.totalCount <= 0) {
                Text(
                    "暂无后台翻译任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val progress =
                    if (state.totalCount == 0) 0f else state.processedCount.toFloat() / state.totalCount.toFloat()
                Column {
                    Text(
                        if (state.isRunning) "后台翻译进行中，可关闭本窗口继续阅读" else "本轮翻译已结束"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("目标范围：第 1 段 到 第 ${state.targetParagraphId + 1} 段")
                    Text("整体进度：${state.processedCount}/${state.totalCount}")
                    Text("缓存命中：${state.cachedCount}")
                    Text("已发请求：${state.requestedCount}")
                    Text("翻译成功：${state.successCount}")
                    if (state.failedCount > 0) {
                        Text(
                            "翻译失败：${state.failedCount}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.currentParagraphId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("当前段落：第 ${state.currentParagraphId + 1} 段")
                        if (!state.currentParagraphPreview.isNullOrBlank()) {
                            Text(
                                text = state.currentParagraphPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private const val CHARACTER_ANNOTATION_TAG = "character"

private fun buildHighlightedParagraphText(
    text: String,
    personNotes: List<PersonNote>,
    characterRecognitionSettings: CharacterRecognitionSettings
): AnnotatedString {
    val candidates = personNotes
        .flatMap { note ->
            CharacterRecognitionPolicy.highlightAliases(note, characterRecognitionSettings).mapNotNull { alias ->
                val trimmedName = alias.trim()
                val normalizedName = normalizeCharacterName(trimmedName)
                if (trimmedName.isBlank()) {
                    null
                } else {
                    HighlightCandidate(
                        characterId = note.id,
                        normalizedName = normalizedName,
                        displayName = trimmedName
                    )
                }
            }
        }
        .distinctBy { "${it.characterId}_${it.normalizedName}" }
        .sortedByDescending { it.displayName.length }

    if (candidates.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < text.length) {
            val match = candidates.firstOrNull { candidate ->
                text.regionMatches(
                    thisOffset = currentIndex,
                    other = candidate.displayName,
                    otherOffset = 0,
                    length = candidate.displayName.length,
                    ignoreCase = true
                ) && isValidHighlightBoundary(text, currentIndex, candidate.displayName.length)
            }
            if (match == null) {
                append(text[currentIndex])
                currentIndex++
            } else {
                val nextIndex = currentIndex + match.displayName.length
                pushStringAnnotation(tag = CHARACTER_ANNOTATION_TAG, annotation = match.characterId)
                pushStyle(
                    SpanStyle(
                        background = androidx.compose.ui.graphics.Color(0x66FFD54F),
                        color = androidx.compose.ui.graphics.Color.Unspecified,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append(text.substring(currentIndex, nextIndex))
                pop()
                pop()
                currentIndex = nextIndex
            }
        }
    }
}

@Composable
private fun CharacterDetailContent(
    character: PersonNote,
    chapters: List<ChapterInfo>,
    onJumpToParagraph: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp)
    ) {
        Text(character.name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (character.isClue) "当前状态：线索人物" else "当前状态：已整理人物",
            style = MaterialTheme.typography.labelMedium,
            color = if (character.isClue) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            character.role,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "出现 ${character.mentionParagraphIds.size} 个段落",
            style = MaterialTheme.typography.labelLarge
        )
        if (character.aliases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "别名：${character.aliases.joinToString(" / ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            character.mentionParagraphIds.firstOrNull()?.let { firstParagraphId ->
                AssistChip(
                    onClick = { onJumpToParagraph(firstParagraphId) },
                    label = { Text("首次出现") }
                )
            }
            character.mentionParagraphIds.lastOrNull()?.let { latestParagraphId ->
                AssistChip(
                    onClick = { onJumpToParagraph(latestParagraphId) },
                    label = { Text("最近出现") }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("出现记录", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
            items(character.mentions.sortedBy { it.paragraphId }) { mention ->
                CharacterMentionRow(
                    mention = mention,
                    chapterLabel = chapterLabelForParagraph(mention.paragraphId, chapters),
                    onJumpToParagraph = onJumpToParagraph
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CharacterMentionRow(
    mention: PersonMention,
    chapterLabel: String,
    onJumpToParagraph: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJumpToParagraph(mention.paragraphId) }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                chapterLabel,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "第 ${mention.paragraphId + 1} 段",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "称呼：${mention.surfaceForm}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            mention.contextSnippet,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun chapterLabelForParagraph(paragraphId: Int, chapters: List<ChapterInfo>): String {
    return chapters
        .lastOrNull { it.startIndex <= paragraphId }
        ?.title
        ?: "正文"
}

private data class HighlightCandidate(
    val characterId: String,
    val normalizedName: String,
    val displayName: String
)

private fun isValidHighlightBoundary(text: String, startIndex: Int, matchLength: Int): Boolean {
    val beforeChar = text.getOrNull(startIndex - 1)
    val afterChar = text.getOrNull(startIndex + matchLength)
    return isBoundaryChar(beforeChar) && isBoundaryChar(afterChar)
}

private fun isBoundaryChar(char: Char?): Boolean {
    if (char == null) return true
    return !char.isLetterOrDigit()
}

private fun normalizeCharacterName(name: String): String {
    return name.trim().lowercase()
}
