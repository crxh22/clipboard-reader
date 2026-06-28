package com.clipboardreader.intake

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.clipboardreader.R
import com.clipboardreader.reader.ReaderService

/**
 * No-UI activity launched by the Quick Settings tile / floating bubble / shortcut.
 * Reads the clipboard in [onResume] (when it actually holds focus — required on Android 10+)
 * then hands the text to [ReaderService] and finishes.
 */
class ClipboardReadActivity : Activity() {
    override fun onResume() {
        super.onResume()
        val text = clipboardText()
        if (text.isNullOrBlank()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
        } else {
            ReaderService.read(this, text)
        }
        finish()
    }

    private fun clipboardText(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()
    }
}
