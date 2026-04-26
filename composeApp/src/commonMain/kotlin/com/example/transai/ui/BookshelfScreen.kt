package com.example.transai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.transai.appFonts
import com.example.transai.data.model.BookMetadata
import com.example.transai.platform.supportsOpenInExplorer
import com.example.transai.viewmodel.BookshelfViewModel

@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel,
    onBookClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val books by viewModel.books.collectAsState()
    val pickFile = rememberFilePicker { path ->
        viewModel.addBook(path)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookMetadata?>(null) }

    if (showDeleteDialog && bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除书籍") },
            text = { Text("确认删除 \"${bookToDelete?.title}\"？这会同时移除本地阅读记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookToDelete?.let { viewModel.removeBook(it.filePath) }
                        showDeleteDialog = false
                        bookToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    AuroraBackground {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ShelfHeader(
                    bookCount = books.size,
                    onAddBook = { pickFile() },
                    onSettingsClick = onSettingsClick
                )
            }

            if (books.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyShelfCard(onAddBook = { pickFile() })
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ShelfOverviewCard(books = books)
                }
                itemsIndexed(books, key = { _, book -> book.filePath }) { index, book ->
                    BookItem(
                        book = book,
                        index = index,
                        onClick = { onBookClick(book.filePath) },
                        onDelete = {
                            bookToDelete = book
                            showDeleteDialog = true
                        },
                        onOpenFolder = { viewModel.openBookFolder(book.filePath) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { pickFile() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Book")
        }
    }
}

@Composable
private fun ShelfHeader(
    bookCount: Int,
    onAddBook: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiFont = appFonts().ui
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TransAI Reader",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = uiFont),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (bookCount == 0) {
                    "导入 EPUB，开始一场更现代的 AI 阅读体验。"
                } else {
                    "继续阅读、翻译到此、查看人物线索，一切都在这里。"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderAction(text = "导入 EPUB", onClick = onAddBook)
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun HeaderAction(
    text: String,
    onClick: () -> Unit
) {
    GlassCard {
        Text(
            text = text,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun EmptyShelfCard(onAddBook: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "你的书架还空着",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "先导入一本 EPUB。后面这里会展示最近阅读、批量翻译进度和人物线索。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HeaderAction(text = "现在导入", onClick = onAddBook)
        }
    }
}

@Composable
private fun ShelfOverviewCard(books: List<BookMetadata>) {
    val totalParagraphs = books.sumOf { it.totalParagraphs }
    val progressBook = books.maxByOrNull { it.lastReadPosition }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "当前书架",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${books.size} 本书，正在形成你的 AI 阅读工作台",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "支持点击翻译、翻译到此、人物识别和进度恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewStatCard(
                    title = "总段落数",
                    value = totalParagraphs.toString(),
                    accent = MaterialTheme.colorScheme.primary
                )
                OverviewStatCard(
                    title = "最近阅读",
                    value = progressBook?.title ?: "暂无",
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(accent.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BookItem(
    book: BookMetadata,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenFolder: () -> Unit
) {
    val uiFont = appFonts().ui
    var showMenu by remember { mutableStateOf(false) }
    val canOpenFolder = supportsOpenInExplorer()
    val progressInfo = remember(book.totalParagraphs, book.lastReadPosition) {
        val currentParagraph = if (book.totalParagraphs > 0) {
            (book.lastReadPosition + 1).coerceIn(1, book.totalParagraphs)
        } else {
            0
        }
        val progressFraction = if (book.totalParagraphs > 0) {
            currentParagraph.toFloat() / book.totalParagraphs.toFloat()
        } else {
            0f
        }
        currentParagraph to progressFraction
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(bookCoverBrush(index))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.32f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "EPUB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = uiFont),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (book.totalParagraphs > 0) "继续阅读" else "刚刚导入",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (book.totalParagraphs > 0) {
                                "${(progressInfo.second * 100).toInt()}%"
                            } else {
                                "NEW"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = progressInfo.second,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (book.totalParagraphs > 0) {
                                "第 ${progressInfo.first} / ${book.totalParagraphs} 段"
                            } else {
                                "等待开始阅读"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = shortPath(book.filePath),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (canOpenFolder) {
                        DropdownMenuItem(
                            text = { Text("打开目录") },
                            onClick = {
                                onOpenFolder()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun shortPath(path: String): String {
    val lastSlash = path.lastIndexOfAny(charArrayOf('/', '\\'))
    return if (lastSlash >= 0 && lastSlash < path.length - 1) {
        path.substring(lastSlash + 1)
    } else {
        path
    }
}
