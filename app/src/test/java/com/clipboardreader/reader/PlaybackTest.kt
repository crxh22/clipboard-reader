package com.clipboardreader.reader

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {

    @Test
    fun wordStarts_handles_multiple_spaces() {
        assertArrayEquals(intArrayOf(0, 6, 12), Playback.wordStarts("alpha beta  gamma"))
    }

    @Test
    fun wordIndexAt_maps_position_to_word() {
        val ws = Playback.wordStarts("alpha beta gamma") // 0, 6, 11
        assertEquals(0, Playback.wordIndexAt(ws, 3))
        assertEquals(1, Playback.wordIndexAt(ws, 6))
        assertEquals(2, Playback.wordIndexAt(ws, 99))
    }

    @Test
    fun skipTarget_moves_by_words_and_clamps() {
        val ws = Playback.wordStarts("alpha beta gamma delta") // 0, 6, 11, 17
        assertEquals(0, Playback.skipTarget(ws, 11, -2)) // from gamma, back 2 -> alpha
        assertEquals(11, Playback.skipTarget(ws, 0, 2))  // from alpha, fwd 2 -> gamma
        assertEquals(17, Playback.skipTarget(ws, 17, 5)) // clamp high
        assertEquals(0, Playback.skipTarget(ws, 0, -5))  // clamp low
    }

    @Test
    fun pieces_short_text_is_single_piece() {
        val p = Playback.pieces("hello world", 0)
        assertEquals(1, p.size)
        assertEquals(0, p[0].first)
        assertEquals("hello world", p[0].second)
    }

    @Test
    fun pieces_split_is_contiguous_and_lossless() {
        val text = "Ana are mere si pere. ".repeat(400)
        val p = Playback.pieces(text, 0, 100)
        assertTrue(p.size > 1)
        p.forEach { assertTrue(it.second.length <= 100) }
        assertEquals(text, p.joinToString("") { it.second })
        var expected = 0
        p.forEach { assertEquals(expected, it.first); expected += it.second.length }
    }

    @Test
    fun pieces_from_offset_keeps_global_base() {
        val p = Playback.pieces("0123456789abcdef", 10, 100)
        assertEquals(10, p[0].first)
        assertEquals("abcdef", p[0].second)
    }
}
