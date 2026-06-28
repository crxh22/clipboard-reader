package com.clipboardreader.reader

/**
 * Splits long text into TTS-sized chunks, preferring sentence / line boundaries.
 * TextToSpeech rejects a single utterance longer than ~4000 chars.
 */
object TextChunker {
    fun chunk(text: String, maxLen: Int = 3500): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxLen) return listOf(trimmed)

        val out = ArrayList<String>()
        val sb = StringBuilder()
        val parts = trimmed.split(Regex("(?<=[.!?。！？\\n])"))
        for (part in parts) {
            if (part.isEmpty()) continue
            if (sb.isNotEmpty() && sb.length + part.length > maxLen) {
                out.add(sb.toString().trim())
                sb.setLength(0)
            }
            if (part.length > maxLen) {
                var i = 0
                while (i < part.length) {
                    val end = minOf(i + maxLen, part.length)
                    out.add(part.substring(i, end).trim())
                    i = end
                }
            } else {
                sb.append(part)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString().trim())
        return out.filter { it.isNotEmpty() }
    }
}
