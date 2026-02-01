package com.example.transai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transai.ui.ReaderScreen
import com.example.transai.ui.SettingsScreen
import com.example.transai.ui.rememberFilePicker
import com.example.transai.viewmodel.ReaderUiEvent
import com.example.transai.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        val viewModel = viewModel { ReaderViewModel() }
        var showSettings by remember { mutableStateOf(false) }
        
        val pickFile = rememberFilePicker { path ->
            viewModel.onEvent(ReaderUiEvent.LoadFile(path))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TransAI Reader") },
                    actions = {
                        if (!showSettings) {
                            TextButton(onClick = { pickFile() }) {
                                Text("Open")
                            }
                            TextButton(onClick = { showSettings = true }) {
                                Text("Settings")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettings = false }
                    )
                } else {
                    ReaderScreen(viewModel = viewModel)
                }
            }
        }
    }
}
