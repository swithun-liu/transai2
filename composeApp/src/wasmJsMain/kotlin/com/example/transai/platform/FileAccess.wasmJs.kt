@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.platform

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

actual class ZipArchive actual constructor(private val filePath: String) {
    private val json = Json

    actual fun close() = Unit

    actual fun getEntry(name: String): ByteArray? {
    try {
        val encoded = WebBridge.zipEntryBase64(filePath, name)
        
        // 安全的 null 检查
        if (encoded == null) {
            println("zipEntryBase64 returned null for: $name")
            return null
        }
        
        // 使用安全的类型转换
        val encodedString = when (encoded) {
            is String -> encoded
            else -> encoded.toString()
        }
        
        println("Decoding base64 for $name, string length: ${encodedString.length}")
        
        return Base64.decode(encodedString)
    } catch (e: Exception) {
        println("Error in getEntry for $name: $e")
        return null
    }
}

actual fun entryNames(): List<String> {
    try {
        val namesJson = WebBridge.zipEntryNames(filePath)
        
        // 安全的 null 检查
        if (namesJson == null) {
            println("zipEntryNames returned null")
            return emptyList()
        }
        
        // 使用安全的类型转换
        val jsonString = when (namesJson) {
            is String -> namesJson
            else -> namesJson.toString()
        }
        
        println("Parsing zip entry names, JSON length: ${jsonString.length}")
        
        return json.decodeFromString(jsonString)
    } catch (e: Exception) {
        println("Error in entryNames: $e")
        return emptyList()
    }
}
}

actual fun saveTempFile(name: String, content: ByteArray): String {
    return BrowserFileStore.saveTempFile(name, content)
}

actual fun saveBookToSandbox(sourcePath: String): String {
    return BrowserFileStore.saveBook(sourcePath)
}

actual fun deleteFile(path: String): Boolean {
    return BrowserFileStore.delete(path)
}

actual fun openInExplorer(path: String) = Unit

actual fun supportsOpenInExplorer(): Boolean = false
