package com.example.transai.data.parser

import com.example.transai.data.model.Book
import com.example.transai.data.model.Chapter
import com.example.transai.data.processor.SmartParagraphSplitter
import com.example.transai.platform.ZipArchive
import com.fleeksoft.ksoup.Ksoup

class EpubParser {
    // 300 chars is a good balance for mobile screens and translation context
    private val splitter = SmartParagraphSplitter(maxParagraphLength = 300)
    private val skippedChapterTitles = setOf(
        "title page",
        "dedication",
        "contents",
        "table of contents",
        "about the author",
        "other books by agatha christie",
        "copyright",
        "about the publisher"
    )

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
            
            val opfData = parseOpf(opfContent)
            val tocEntries = parseTocEntries(zip, opfPath, opfData)
            val tocTitleByPath = tocEntries.associateBy({ it.path }, { it.title })
            
            // 3. Extract chapters
            val chapters = opfData.spinePaths.mapIndexedNotNull { index, itemPath ->
                // OPF path relative resolution
                val fullPath = resolvePath(opfPath, itemPath)
                val htmlEntry = zip.getEntry(fullPath)
                
                if (htmlEntry != null) {
                    val htmlContent = htmlEntry.decodeToString()
                    val extractedTitle = tocTitleByPath[fullPath] ?: extractChapterTitle(htmlContent)
                    val title = if (extractedTitle.isNullOrBlank()) "Chapter ${index + 1}" else extractedTitle
                    
                    val text = parseHtmlToText(htmlContent)
                    val paragraphs = splitToParagraphs(text)
                    if (shouldSkipChapter(title, fullPath, paragraphs)) {
                        null
                    } else {
                        Chapter(title, text, paragraphs)
                    }
                } else {
                    println("Warning: Missing chapter file $fullPath")
                    null
                }
            }
            
            return Book(opfData.metadata.title, opfData.metadata.author, chapters)
        } finally {
            zip.close()
        }
    }

    private fun parseContainerXml(xml: String): String {
        val regex = Regex("""<rootfile[^>]*full-path=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1) ?: "OEBPS/content.opf"
    }

    data class EpubMetadata(val title: String, val author: String)
    data class TocEntry(val title: String, val path: String)
    data class OpfData(
        val metadata: EpubMetadata,
        val manifest: Map<String, String>,
        val spinePaths: List<String>,
        val tocPath: String?
    )

    private fun parseOpf(xml: String): OpfData {
        val title = extractTagText(xml, "title")
        val author = extractTagText(xml, "creator")

        val manifestTagRegex = Regex("""<item\b[^>]*>""", RegexOption.IGNORE_CASE)
        val manifest = manifestTagRegex.findAll(xml).mapNotNull { match ->
            val tag = match.value
            val id = extractAttribute(tag, "id")
            val href = extractAttribute(tag, "href")
            if (id.isNullOrBlank() || href.isNullOrBlank()) {
                null
            } else {
                id to href
            }
        }.toMap()

        val itemRefRegex = Regex("""<itemref\b[^>]*idref=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val spineIds = itemRefRegex.findAll(xml).map { it.groupValues[1] }.toList()
        val spineOpenTag = Regex("""<spine\b([^>]*)>""", RegexOption.IGNORE_CASE)
            .find(xml)
            ?.value
            .orEmpty()
        val tocId = Regex("""toc=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(spineOpenTag)
            ?.groupValues
            ?.get(1)
            .orEmpty()
        val ncxPath = manifest[tocId]
            ?: manifestTagRegex.findAll(xml).firstNotNullOfOrNull { match ->
                val tag = match.value
                val mediaType = extractAttribute(tag, "media-type")
                val href = extractAttribute(tag, "href")
                if (mediaType == "application/x-dtbncx+xml") href else null
            }

        val spinePaths = spineIds.mapNotNull { manifest[it] }
        return OpfData(
            metadata = EpubMetadata(title, author),
            manifest = manifest,
            spinePaths = spinePaths,
            tocPath = ncxPath
        )
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        // If basePath is "OEBPS/content.opf", parent is "OEBPS"
        // If relativePath is "chap1.html", result is "OEBPS/chap1.html"
        val parent = basePath.substringBeforeLast('/', "")
        if (parent.isEmpty()) return relativePath
        return "$parent/$relativePath"
    }

    private fun parseTocEntries(
        zip: ZipArchive,
        opfPath: String,
        opfData: OpfData
    ): List<TocEntry> {
        val tocRelativePath = opfData.tocPath ?: return emptyList()
        val tocFullPath = resolvePath(opfPath, tocRelativePath)
        val tocEntry = zip.getEntry(tocFullPath) ?: return emptyList()
        val tocXml = tocEntry.decodeToString()
        val navPointRegex = Regex(
            """<navPoint\b[\s\S]*?<navLabel>\s*<text>(.*?)</text>\s*</navLabel>[\s\S]*?<content\b[^>]*src=["']([^"']+)["']""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return navPointRegex.findAll(tocXml).mapNotNull { match ->
            val title = decodeXmlEntities(stripXmlTags(match.groupValues[1])).trim()
            val src = match.groupValues[2].trim()
            if (title.isBlank() || src.isBlank()) {
                null
            } else {
                TocEntry(
                    title = title,
                    path = resolvePath(tocFullPath, src.substringBefore('#'))
                )
            }
        }.toList()
    }

    private fun extractChapterTitle(html: String): String? {
        val doc = Ksoup.parse(html)
        val headers = doc.select("h1, h2, h3").eachText()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (headers.isNotEmpty()) {
            if (headers.size >= 2 && looksLikeOrdinalHeader(headers[0])) {
                return "${headers[0]} ${headers[1]}".trim()
            }
            return headers.first()
        }
        // Fallback to title tag
        val titleTag = doc.select("title").firstOrNull()
        return titleTag?.text()
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
        val rawParagraphs = text.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        return splitter.process(rawParagraphs)
    }

    private fun shouldSkipChapter(
        title: String,
        path: String,
        paragraphs: List<String>
    ): Boolean {
        if (paragraphs.isEmpty()) return true

        val normalizedTitle = normalizeTitle(title)
        val normalizedPath = path.lowercase()

        if (normalizedTitle in skippedChapterTitles) return true
        if (normalizedPath.contains("toc") || normalizedPath.contains("nav")) return true

        return false
    }

    private fun looksLikeOrdinalHeader(text: String): Boolean {
        val normalized = normalizeTitle(text)
        return normalized.matches(Regex("""(\d+|[ivxlcdm]+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)"""))
    }

    private fun normalizeTitle(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace('’', '\'')
    }

    private fun extractTagText(xml: String, localName: String): String {
        val regex = Regex(
            """<(?:[\w.-]+:)?$localName\b[^>]*>(.*?)</(?:[\w.-]+:)?$localName>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.find(xml)?.groupValues?.get(1)?.let(::stripXmlTags)?.let(::decodeXmlEntities)?.trim().orEmpty()
    }

    private fun stripXmlTags(text: String): String {
        return text.replace(Regex("""<[^>]+>"""), " ")
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#8217;", "’")
            .replace("&#x2019;", "’")
    }

    private fun extractAttribute(tag: String, name: String): String? {
        val regex = Regex("""\b$name=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(tag)?.groupValues?.get(1)
    }
}
