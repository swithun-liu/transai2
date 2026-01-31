package com.example.transai.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(onFilePicked: (String) -> Unit): () -> Unit {
    return {
        // Placeholder for iOS
        println("File picker not implemented for iOS yet")
    }
}
