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

    @Test
    fun testAliceLongParagraph() {
        // Real example from Alice in Wonderland
        val text = "There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, “Oh dear! Oh dear! I shall be late!” (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural); but when the Rabbit actually took a watch out of its waistcoat-pocket, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it, and burning with curiosity, she ran across the field after it, and fortunately was in time to see it pop down a large rabbit-hole under the hedge."
        
        // Target: Split into roughly 3 parts (~200-300 chars each)
        val splitter = SmartParagraphSplitter(maxParagraphLength = 300)
        val output = splitter.process(listOf(text))
        
        println("Output size: ${output.size}")
        output.forEachIndexed { index, s -> 
            println("Part $index (${s.length} chars): $s") 
        }
        
        // Expecting at least 2 parts, likely 3
        assert(output.size >= 2) { "Should be split into at least 2 parts, but got ${output.size}" }
    }
}
