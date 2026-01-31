package com.example.transai.platform

import java.util.zip.ZipFile
import java.io.File

actual class ZipArchive actual constructor(filePath: String) {
    private val zipFile: ZipFile?

    init {
        val file = File(filePath)
        zipFile = if (file.exists()) ZipFile(file) else null
    }

    actual fun close() {
        zipFile?.close()
    }

    actual fun getEntry(name: String): ByteArray? {
        val entry = zipFile?.getEntry(name) ?: return null
        return zipFile.getInputStream(entry).use { it.readBytes() }
    }

    actual fun entryNames(): List<String> {
        return zipFile?.entries()?.asSequence()?.map { it.name }?.toList() ?: emptyList()
    }
}
