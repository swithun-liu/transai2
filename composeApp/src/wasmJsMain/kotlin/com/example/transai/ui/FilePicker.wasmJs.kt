@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.transai.platform.WebBridge
import com.example.transai.platform.saveTempFile
import kotlin.io.encoding.Base64
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
actual fun rememberFilePicker(onFilePicked: (String) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val payloadJson = WebBridge.pickEpubFile() as? String ?: return@launch
            val payload = Json.decodeFromString<PickedFilePayload>(payloadJson)
            val bytes = Base64.decode(payload.base64)
            onFilePicked(saveTempFile(payload.name, bytes))
        }
    }
}

@Serializable
private data class PickedFilePayload(
    val name: String,
    val base64: String
)
