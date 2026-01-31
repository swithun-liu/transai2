package com.example.transai.data.parser

import com.example.transai.data.model.Book
import com.example.transai.data.model.Chapter
import com.example.transai.platform.ZipArchive
import com.fleeksoft.ksoup.Ksoup

class EpubParser {
    fun parse(filePath: String): Book {
        val zip = ZipArchive(filePath)
        try {
            // 1. Find OPF path from META-INF/container.xml
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: throw Exception("Invalid EPUB: Missing container.xml")
            val containerXml = containerEntry.decodeToString()
            
            val opfPath = parseContainerXml(containerXml)
            
            // 2. Parse OPF to get metadata and spine
            val opfEntry = zip.getEntry(opfPath)
                ?: throw Exception("Invalid EPUB: Missing OPF file at $opfPath")
            val opfContent = opfEntry.decodeToString()
            
            val (metadata, spine) = parseOpf(opfContent)
            
            // 3. Extract chapters
            val chapters = spine.mapNotNull { itemPath ->
                // OPF path relative resolution
                val fullPath = resolvePath(opfPath, itemPath)
                val htmlEntry = zip.getEntry(fullPath)
                
                if (htmlEntry != null) {
                    val htmlContent = htmlEntry.decodeToString()
                    val text = parseHtmlToText(htmlContent)
                    val paragraphs = splitToParagraphs(text)
                    // Use file name or header as title for now
                    Chapter("Chapter", text, paragraphs)
                } else {
                    println("Warning: Missing chapter file $fullPath")
                    null
                }
            }
            
            return Book(metadata.title, metadata.author, chapters)
        } finally {
            zip.close()
        }
    }

    private fun parseContainerXml(xml: String): String {
        val doc = Ksoup.parse(xml)
        // Standard OCF container.xml parsing
        val rootFile = doc.select("rootfile").firstOrNull()
        return rootFile?.attr("full-path") ?: "OEBPS/content.opf"
    }

    data class EpubMetadata(val title: String, val author: String)
    
    private fun parseOpf(xml: String): Pair<EpubMetadata, List<String>> {
        val doc = Ksoup.parse(xml)
        
        // Namespaces in Jsoup/Ksoup can be tricky, often just ignoring them works or using *|tag
        val title = doc.select("dc|title").text().ifEmpty { doc.select("title").text() }
        val author = doc.select("dc|creator").text().ifEmpty { doc.select("creator").text() }
        
        // Manifest: ID -> Href
        val manifestItems = doc.select("manifest > item")
        val manifest = manifestItems.associate { 
            it.attr("id") to it.attr("href") 
        }
        
        // Spine: List of IDs
        val spineItems = doc.select("spine > itemref")
        val spineIds = spineItems.map { it.attr("idref") }
        
        val spinePaths = spineIds.mapNotNull { manifest[it] }
        return Pair(EpubMetadata(title, author), spinePaths)
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        // If basePath is "OEBPS/content.opf", parent is "OEBPS"
        // If relativePath is "chap1.html", result is "OEBPS/chap1.html"
        val parent = basePath.substringBeforeLast('/', "")
        if (parent.isEmpty()) return relativePath
        return "$parent/$relativePath"
    }

    private fun parseHtmlToText(html: String): String {
        val doc = Ksoup.parse(html)
        // Extract paragraphs and headers.
        // We select <p> and <h1-6> tags to ensure we get proper structure.
        val elements = doc.select("h1, h2, h3, h4, h5, h6, p")
        val texts = elements.eachText()
        
        if (texts.isNotEmpty()) {
            return texts.joinToString("\n\n")
        }
        // Fallback if no specific tags (e.g. div soup)
        return doc.body().text()
    }

    private fun splitToParagraphs(text: String): List<String> {
        // Split by double newline which we inserted
        return text.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
