package com.example.transai.platform

import platform.Foundation.*
import kotlinx.cinterop.*

actual class ZipArchive actual constructor(filePath: String) {
    actual fun close() {
        // TODO: Implement iOS zip handling
    }

    actual fun getEntry(name: String): ByteArray? {
        // TODO: Implement iOS zip handling
        return null
    }

    actual fun entryNames(): List<String> {
        return emptyList()
    }
}

actual fun saveTempFile(name: String, content: ByteArray): String {
    val tempDir = NSTemporaryDirectory()
    val path = "${tempDir}${name}"
    
    val data = content.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), content.size.toULong())
    }
    
    data.writeToFile(path, true)
    return path
}

actual fun saveBookToSandbox(sourcePath: String): String {
    // Basic implementation for iOS - assuming sourcePath is accessible
    // In a real app, we would copy to Documents directory
    val fileManager = NSFileManager.defaultManager
    val documentsUrl = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as? NSURL
    val documentsPath = documentsUrl?.path
    
    if (documentsPath != null) {
        val booksDir = "$documentsPath/books"
        if (!fileManager.fileExistsAtPath(booksDir)) {
             fileManager.createDirectoryAtPath(booksDir, true, null, null)
        }
        
        val fileName = sourcePath.split("/").last()
        val destPath = "$booksDir/$fileName"
        
        val error = null
        if (fileManager.fileExistsAtPath(destPath)) {
            fileManager.removeItemAtPath(destPath, null)
        }
        fileManager.copyItemAtPath(sourcePath, destPath, null)
        return destPath
    }
    
    return sourcePath
}

actual fun openInExplorer(path: String) {
    // Not supported on iOS
    println("Open in explorer requested for: $path")
}

