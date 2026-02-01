package com.example.transai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transai.ui.BookshelfScreen
import com.example.transai.ui.ReaderScreen
import com.example.transai.ui.SettingsScreen
import com.example.transai.viewmodel.BookshelfViewModel
import com.example.transai.viewmodel.ReaderUiEvent
import com.example.transai.viewmodel.ReaderViewModel

sealed class Screen {
    data object Bookshelf : Screen()
    data object Reader : Screen()
    data object Settings : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        val readerViewModel = viewModel { ReaderViewModel() }
        val bookshelfViewModel = viewModel { BookshelfViewModel() }
        
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Bookshelf) }

        when (val screen = currentScreen) {
            Screen.Bookshelf -> {
                BookshelfScreen(
                    viewModel = bookshelfViewModel,
                    onBookClick = { path ->
                        readerViewModel.onEvent(ReaderUiEvent.LoadFile(path))
                        currentScreen = Screen.Reader
                    },
                    onSettingsClick = {
                        currentScreen = Screen.Settings
                    }
                )
            }
            Screen.Reader -> {
                ReaderScreen(
                    viewModel = readerViewModel,
                    onBack = { currentScreen = Screen.Bookshelf }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    viewModel = readerViewModel,
                    onBack = { currentScreen = Screen.Bookshelf }
                )
            }
        }
    }
}
