package com.clipboardreader.reader

/**
 * Pure, unit-testable playback math: word boundaries, TTS-piece splitting and skip targets.
 * No Android dependencies, so it can be verified on the JVM without a device.
 */
object Playback {
    const val MAX_PIECE = 3500

    /** Char index where each whitespace-separated word starts. Non-empty for non-blank text. */
    fun wordStarts(text: String): IntArray {
        val starts = ArrayList<Int>()
        var inWord = false
        for (i in text.indices) {
            val ws = text[i].isWhitespace()
            if (!ws && !inWord) {
                starts.add(i)
                inWord = true
            } else if (ws) {
                inWord = false
            }
        }
        if (starts.isEmpty()) starts.add(0)
        return starts.toIntArray()
    }

    /** Index of the word at, or just before, char [pos]. */
    fun wordIndexAt(starts: IntArray, pos: Int): Int {
        var lo = 0
        var hi = starts.size - 1
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (starts[mid] <= pos) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }

    /** New char position after moving [delta] words from [pos] (clamped to the text). */
    fun skipTarget(starts: IntArray, pos: Int, delta: Int): Int {
        if (starts.isEmpty()) return 0
        val idx = (wordIndexAt(starts, pos) + delta).coerceIn(0, starts.size - 1)
        return starts[idx]
    }

    /**
     * Split text[[from]..] into contiguous pieces of at most [maxLen] chars, cutting at
     * sentence/space boundaries where possible. Returns (globalStartOffset, pieceText).
     */
    fun pieces(text: String, from: Int, maxLen: Int = MAX_PIECE): List<Pair<Int, String>> {
        val out = ArrayList<Pair<Int, String>>()
        var i = from.coerceIn(0, text.length)
        while (i < text.length) {
            var end = minOf(i + maxLen, text.length)
            if (end < text.length) {
                val cut = text.substring(i, end).lastIndexOfAny(charArrayOf('.', '!', '?', '\n', ' '))
                if (cut > 0) end = i + cut + 1
            }
            out.add(i to text.substring(i, end))
            i = end
        }
        return out
    }
}
