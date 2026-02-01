package com.example.transai.platform

import java.util.zip.ZipFile
import java.io.File
import com.example.transai.TransAIApplication
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import androidx.core.content.FileProvider

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
    val context = TransAIApplication.appContext ?: throw IllegalStateException("Context not initialized")
    val sourceFile = File(sourcePath)
    val booksDir = File(context.filesDir, "books")
    if (!booksDir.exists()) booksDir.mkdirs()
    
    // Create destination file with same name
    val destFile = File(booksDir, sourceFile.name)
    
    // Copy content
    sourceFile.copyTo(destFile, overwrite = true)
    
    return destFile.absolutePath
}

actual fun openInExplorer(path: String) {
    val context = TransAIApplication.appContext ?: return
    try {
        val file = File(path)
        val parentFile = file.parentFile ?: return
        
        // Strategy 1: Try opening the folder directly using file:// URI with strict mode bypass
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        
        val folderUri = Uri.fromFile(parentFile)
        val folderIntent = Intent(Intent.ACTION_VIEW)
        folderIntent.setDataAndType(folderUri, "resource/folder")
        folderIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (folderIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(folderIntent)
            return
        }
        
        // Strategy 2: Try opening the folder with specific MIME type for file managers
        folderIntent.setDataAndType(folderUri, "vnd.android.cursor.dir/file")
        if (folderIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(folderIntent)
            return
        }

        // Strategy 3: Try opening the file itself using FileProvider
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val fileIntent = Intent(Intent.ACTION_VIEW)
        // Try generic type if epub specific fails, or stick to epub to be safe
        fileIntent.setDataAndType(fileUri, "application/epub+zip")
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        fileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (fileIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(fileIntent)
        } else {
            // Strategy 4: Last resort, try opening as generic file
            fileIntent.setDataAndType(fileUri, "*/*")
            context.startActivity(fileIntent)
        }
    } catch (e: Exception) {
        println("Error opening explorer: ${e.message}")
        e.printStackTrace()
    }
}



