package com.example.transai.data.processor

import kotlin.test.Test
import kotlin.test.assertEquals

class SmartParagraphSplitterTest {

    @Test
    fun testShortParagraphNotSplit() {
        val splitter = SmartParagraphSplitter(maxParagraphLength = 50)
        val input = listOf("Short text.")
        val output = splitter.process(input)
        assertEquals(input, output)
    }

    @Test
    fun testSplitBySentence() {
        // "One. Two. " is 10 chars including trailing space
        val splitter = SmartParagraphSplitter(maxParagraphLength = 10)
        val text = "One. Two. Three." 
        // 0123456789012345
        // One. Two. Three.
        
        // Iteration 1:
        // currentIndex = 0
        // searchLimit = 10
        // text[0..9] = "One. Two. "
        // Look for delimiter from 9 down to 5
        // text[8] is '.' -> splitIndex = 9
        // chunk = "One. Two."
        
        // Iteration 2:
        // currentIndex = 9
        // remaining = " Three." (length 7)
        // 7 <= 10 -> add "Three."
        
        val output = splitter.process(listOf(text))
        assertEquals(listOf("One. Two.", "Three."), output)
    }

    @Test
    fun testSplitByCommaFallback() {
        val splitter = SmartParagraphSplitter(maxParagraphLength = 10)
        val text = "One, Two, Three"
        // No periods.
        // "One, Two, " -> 10 chars.
        // Should split at comma.
        
        val output = splitter.process(listOf(text))
        assertEquals(listOf("One, Two,", "Three"), output)
    }

    @Test
    fun testHardSplitFallback() {
        val splitter = SmartParagraphSplitter(maxParagraphLength = 5)
        val text = "ABCDEFGHIJ"
        // No delimiters.
        // Should hard split.
        
        val output = splitter.process(listOf(text))
        assertEquals(listOf("ABCDE", "FGHIJ"), output)
    }
}
