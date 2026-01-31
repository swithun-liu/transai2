package com.example.transai.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(onFilePicked: (String) -> Unit): () -> Unit
