@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.example.transai.platform

import kotlin.js.JsAny
import kotlin.js.Promise

@JsModule("./transai_bridge.mjs")
external object WebBridge {
    fun createTempPath(name: String): String
    fun createBookPath(name: String): String
    fun saveFile(path: String, base64: String)
    fun readFile(path: String): String?
    fun deleteStoredFile(path: String): Boolean
    fun zipEntryNames(path: String): Promise<String>
    fun zipEntryBase64(path: String, name: String): Promise<String?>
    fun pickEpubFile(): Promise<JsAny?>
}
