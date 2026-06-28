package com.clipboardreader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun short_text_is_one_chunk() {
        assertEquals(listOf("Salut lume"), TextChunker.chunk("Salut lume"))
    }

    @Test
    fun blank_text_yields_no_chunks() {
        assertTrue(TextChunker.chunk("   \n  ").isEmpty())
    }

    @Test
    fun long_text_splits_under_limit_without_losing_content() {
        val text = "Aceasta este o propozitie de test. ".repeat(400)
        val chunks = TextChunker.chunk(text, maxLen = 1000)

        assertTrue("expected multiple chunks", chunks.size > 1)
        chunks.forEach { assertTrue("chunk too long: ${it.length}", it.length <= 1000) }

        val strip = Regex("\\s+")
        assertEquals(
            text.replace(strip, ""),
            chunks.joinToString("").replace(strip, ""),
        )
    }
}
