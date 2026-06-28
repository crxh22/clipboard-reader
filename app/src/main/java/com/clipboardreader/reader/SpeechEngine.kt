package com.clipboardreader.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/**
 * Thin wrapper around [TextToSpeech]: async init, language auto-detect, chunked playback.
 * [onComplete] fires when the whole text has been spoken; [onFail] on init/playback error.
 */
class SpeechEngine(
    context: Context,
    private val onComplete: () -> Unit,
    private val onFail: (String) -> Unit,
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Request? = null
    private var total = 0
    private var completed = 0

    data class Request(val text: String, val langOverride: String, val rate: Float)

    private val appContext = context.applicationContext

    fun init() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(listener)
                pending?.let { req -> pending = null; speak(req) }
            } else {
                onFail("init")
            }
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            completed++
            if (completed >= total) onComplete()
        }
        @Deprecated("Deprecated in TextToSpeech", ReplaceWith("onError(utteranceId, errorCode)"))
        override fun onError(utteranceId: String?) {
            onFail("playback")
        }
        override fun onError(utteranceId: String?, errorCode: Int) {
            onFail("playback:$errorCode")
        }
    }

    fun speak(req: Request) {
        val engine = tts
        if (engine == null || !ready) {
            pending = req
            return
        }
        engine.setLanguage(LanguageDetector.localeFor(req.text, req.langOverride))
        engine.setSpeechRate(req.rate.coerceIn(0.5f, 2.0f))
        val chunks = TextChunker.chunk(req.text)
        if (chunks.isEmpty()) {
            onComplete()
            return
        }
        total = chunks.size
        completed = 0
        chunks.forEachIndexed { i, chunk ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunk, mode, null, "u$i")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
