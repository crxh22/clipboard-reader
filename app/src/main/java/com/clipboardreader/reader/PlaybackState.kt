package com.clipboardreader.reader

import java.util.concurrent.CopyOnWriteArraySet

/** Process-wide playback state so the bubble + notification can reflect play / pause / idle. */
object PlaybackState {
    enum class State { IDLE, PLAYING, PAUSED }

    @Volatile
    var state: State = State.IDLE
        private set

    /** Text currently loaded for reading (for the in-app "now reading" card). */
    @Volatile
    var text: String = ""

    /** Read progress 0f..1f (for the in-app progress bar). */
    @Volatile
    var progress: Float = 0f

    private val listeners = CopyOnWriteArraySet<(State) -> Unit>()

    fun update(s: State) {
        state = s
        listeners.forEach { it(s) }
    }

    fun addListener(l: (State) -> Unit) {
        listeners.add(l)
        l(state)
    }

    fun removeListener(l: (State) -> Unit) {
        listeners.remove(l)
    }
}
