package com.example.transai.ui

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun rememberFilePicker(onFilePicked: (String) -> Unit): () -> Unit {
    return {
        // Use AWT FileDialog which is native on macOS/Windows
        val dialog = FileDialog(null as Frame?, "Open Book", FileDialog.LOAD)
        // Basic filtering (optional, might not work on all platforms consistently)
        dialog.setFilenameFilter { _, name -> 
            name.endsWith(".epub", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true) 
        }
        dialog.isVisible = true
        
        val file = dialog.file
        val dir = dialog.directory
        if (file != null && dir != null) {
            onFilePicked(dir + file)
        }
    }
}
