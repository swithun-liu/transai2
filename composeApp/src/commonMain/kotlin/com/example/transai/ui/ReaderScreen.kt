package com.example.transai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.transai.model.Paragraph
import com.example.transai.viewmodel.ReaderUiEvent
import com.example.transai.viewmodel.ReaderViewModel
import com.example.transai.viewmodel.WordPopupState
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
                    item {
                        Text(
                            "Characters",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    item {
                        HorizontalDivider()
                    }
                    if (uiState.personNotes.isEmpty()) {
                        item {
                            Text(
                                "暂无人物笔记",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.personNotes) { note ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(note.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    note.role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                                onToggle = { viewModel.onEvent(ReaderUiEvent.ToggleTranslation(paragraph.id)) },
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
    onToggle: () -> Unit,
    onWordTap: (String, Offset) -> Unit,
    onCoordinatesChanged: (LayoutCoordinates?) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    DisposableEffect(Unit) {
        onDispose { onCoordinatesChanged(null) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = paragraph.originalText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .pointerInput(paragraph.originalText) {
                            detectTapGestures { position ->
                                val result = layoutResult ?: return@detectTapGestures
                                val offset = result.getOffsetForPosition(position)
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
