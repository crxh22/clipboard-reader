package com.clipboardreader.intake

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.clipboardreader.R
import com.clipboardreader.reader.ReaderService

/**
 * No-UI activity launched by the Quick Settings tile / floating bubble / shortcut.
 *
 * Android 10+ only lets an app read the clipboard while it actually holds window
 * focus. onResume() runs BEFORE focus is granted, so reading there returns null
 * ("clipboard gol"). We therefore read in onWindowFocusChanged(true) — the first
 * moment we are guaranteed to have focus — then hand the text to the reader and finish.
 */
class ClipboardReadActivity : Activity() {

    private var handled = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !handled) {
            handled = true
            val text = clipboardText()
            if (text.isNullOrBlank()) {
                Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            } else {
                ReaderService.read(this, text)
            }
            finish()
        }
    }

    private fun clipboardText(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()
    }
}
