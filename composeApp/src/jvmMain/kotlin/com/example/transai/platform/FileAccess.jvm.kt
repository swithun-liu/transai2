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

actual fun saveTempFile(name: String, content: ByteArray): String {
    val tempDir = File(System.getProperty("java.io.tmpdir") ?: ".")
    val file = File(tempDir, name)
    file.writeBytes(content)
    return file.absolutePath
}

actual fun saveBookToSandbox(sourcePath: String): String {
    val userHome = System.getProperty("user.home")
    val booksDir = File(userHome, ".transai/books")
    if (!booksDir.exists()) booksDir.mkdirs()
    
    val sourceFile = File(sourcePath)
    val destFile = File(booksDir, sourceFile.name)
    
    sourceFile.copyTo(destFile, overwrite = true)
    
    return destFile.absolutePath
}

actual fun deleteFile(path: String): Boolean {
    val file = File(path)
    return if (file.exists()) {
        file.delete()
    } else {
        false
    }
}

actual fun openInExplorer(path: String) {
    try {
        val file = File(path)
        val parent = file.parentFile
        if (parent != null && parent.exists()) {
            java.awt.Desktop.getDesktop().open(parent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

