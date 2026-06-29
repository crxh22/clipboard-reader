package com.clipboardreader.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.clipboardreader.Prefs

/**
 * TTS glue. Plays [start]ed text and supports pause / resume / word-skip by tracking a global
 * char position via onRangeStart. A generation counter marks utterances from an interrupted
 * (flushed/stopped) batch as stale, so an intentional stop never flips state incorrectly.
 */
class SpeechEngine(
    context: Context,
    private val onState: (PlaybackState.State) -> Unit,
    private val onError: (String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingPlay = false

    private var text = ""
    private var pos = 0
    private var starts = IntArray(0)
    private var lang = Prefs.LANG_AUTO
    private var rate = 1f

    private var gen = 0
    private val bases = ArrayList<Int>()
    private var lastSeq = -1

    fun init() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(listener)
                if (pendingPlay) {
                    pendingPlay = false
                    speakFrom(pos)
                }
            } else {
                onError("init")
                onState(PlaybackState.State.IDLE)
            }
        }
    }

    fun start(text: String, lang: String, rate: Float) {
        this.text = text
        this.lang = lang
        this.rate = rate
        this.pos = 0
        this.starts = Playback.wordStarts(text)
        PlaybackState.progress = 0f
        if (text.isBlank()) {
            onState(PlaybackState.State.IDLE)
            return
        }
        if (ready) speakFrom(0) else pendingPlay = true
    }

    fun pause() {
        gen++ // invalidate in-flight utterances so their onError/onDone are ignored
        tts?.stop()
        onState(PlaybackState.State.PAUSED)
    }

    fun resume() {
        if (text.isBlank()) return
        if (ready) speakFrom(pos) else pendingPlay = true
    }

    fun skipWords(delta: Int) {
        if (text.isBlank()) return
        pos = Playback.skipTarget(starts, pos, delta)
        if (ready) speakFrom(pos) else pendingPlay = true
    }

    fun stop() {
        gen++
        tts?.stop()
        text = ""
        pos = 0
        PlaybackState.progress = 0f
        onState(PlaybackState.State.IDLE)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun speakFrom(from: Int) {
        val engine = tts ?: return
        val g = ++gen
        engine.setLanguage(LanguageDetector.localeFor(text, lang))
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        bases.clear()
        val pieces = Playback.pieces(text, from)
        if (pieces.isEmpty()) {
            onState(PlaybackState.State.IDLE)
            return
        }
        lastSeq = pieces.size - 1
        pieces.forEachIndexed { seq, (base, piece) ->
            bases.add(base)
            val mode = if (seq == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(piece, mode, null, "u${g}_$seq")
        }
        onState(PlaybackState.State.PLAYING)
    }

    private fun parse(id: String?): Pair<Int, Int>? {
        val parts = id?.removePrefix("u")?.split("_") ?: return null
        if (parts.size != 2) return null
        val g = parts[0].toIntOrNull() ?: return null
        val seq = parts[1].toIntOrNull() ?: return null
        return g to seq
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val (g, seq) = parse(utteranceId) ?: return
            if (g != gen) return
            val base = bases.getOrNull(seq) ?: return
            pos = base + start
            if (text.isNotEmpty()) {
                PlaybackState.progress = ((base + start).toFloat() / text.length).coerceIn(0f, 1f)
            }
        }

        override fun onDone(utteranceId: String?) {
            val (g, seq) = parse(utteranceId) ?: return
            if (g == gen && seq == lastSeq) {
                pos = text.length
                PlaybackState.progress = 1f
                onState(PlaybackState.State.IDLE)
            }
        }

        @Deprecated("Deprecated in TextToSpeech")
        override fun onError(utteranceId: String?) = fail(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = fail(utteranceId)

        private fun fail(utteranceId: String?) {
            val (g, _) = parse(utteranceId) ?: return
            if (g == gen) onState(PlaybackState.State.IDLE)
        }
    }
}
