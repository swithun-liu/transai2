@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.transai.platform.WebBridge
import com.example.transai.platform.saveTempFile
import kotlin.io.encoding.Base64
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
actual fun rememberFilePicker(onFilePicked: (String) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            println("File picker triggered")
            val payloadJson = WebBridge.pickEpubFile().await() as? String
            
            if (payloadJson == null) {
                println("No file selected or file read failed")
                return@launch
            }
            
            println("File data received, parsing...")
            val payload = Json.decodeFromString<PickedFilePayload>(payloadJson)
            println("File name: ${payload.name}, size: ${payload.base64.length}")
            
            val bytes = Base64.decode(payload.base64)
            println("Decoded bytes size: ${bytes.size}")
            
            val savedPath = saveTempFile(payload.name, bytes)
            println("File saved to: $savedPath")
            
            onFilePicked(savedPath)
            println("File picked callback executed")
        }
    }
}

@Serializable
private data class PickedFilePayload(
    val name: String,
    val base64: String
)
