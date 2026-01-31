package com.example.transai.data.processor

/**
 * A processor that splits long paragraphs into smaller chunks intelligently.
 * It attempts to split at sentence boundaries (punctuation) to preserve meaning.
 * This is useful for:
 * 1. Displaying text in manageable chunks.
 * 2. Sending text to translation APIs that have character limits.
 * 3. Improving the granularity of "Click to Translate".
 */
class SmartParagraphSplitter(
    private val maxParagraphLength: Int = 1000
) {
    /**
     * Process a list of raw paragraphs and return a refined list.
     */
    fun process(paragraphs: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in paragraphs) {
            if (paragraph.length > maxParagraphLength) {
                result.addAll(splitLongParagraph(paragraph))
            } else {
                result.add(paragraph)
            }
        }
        return result
    }

    private fun splitLongParagraph(text: String): List<String> {
        val chunks = mutableListOf<String>()
        val sentenceDelimiters = charArrayOf('.', '?', '!', '。', '？', '！')
        val clauseDelimiters = charArrayOf(',', ';', ':', '，', '；', '：')
        
        var currentIndex = 0
        val length = text.length
        
        while (currentIndex < length) {
            // If the remaining text fits, add it and finish
            if (length - currentIndex <= maxParagraphLength) {
                val chunk = text.substring(currentIndex).trim()
                if (chunk.isNotEmpty()) {
                    chunks.add(chunk)
                }
                break
            }

            // Calculate the maximum possible end index for this chunk
            val searchLimit = (currentIndex + maxParagraphLength).coerceAtMost(length)
            
            // Strategy 1: Look for sentence terminators (. ! ?)
            var splitIndex = -1
            for (i in searchLimit - 1 downTo currentIndex + (maxParagraphLength / 2)) { // Don't split too early
                if (text[i] in sentenceDelimiters) {
                    splitIndex = i + 1 // Include the delimiter
                    break
                }
            }
            
            // Strategy 2: Look for clause delimiters (, ; :) if no sentence end found
            if (splitIndex == -1) {
                for (i in searchLimit - 1 downTo currentIndex + (maxParagraphLength / 2)) {
                    if (text[i] in clauseDelimiters) {
                        splitIndex = i + 1
                        break
                    }
                }
            }

            // Strategy 3: Look for whitespace
             if (splitIndex == -1) {
                for (i in searchLimit - 1 downTo currentIndex) {
                    if (text[i].isWhitespace()) {
                        splitIndex = i + 1
                        break
                    }
                }
            }

            // Strategy 4: Hard split (force break)
            if (splitIndex == -1) {
                splitIndex = searchLimit
            }

            // Extract the chunk
            val chunk = text.substring(currentIndex, splitIndex).trim()
            if (chunk.isNotEmpty()) {
                chunks.add(chunk)
            }
            
            // Move forward
            currentIndex = splitIndex
        }
        
        return chunks
    }
}
