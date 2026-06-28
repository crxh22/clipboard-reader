package com.clipboardreader.intake

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.clipboardreader.reader.ReaderService

/** Handles "Citeste cu voce" from the text-selection popup (ACTION_PROCESS_TEXT). */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            ReaderService.read(this, text)
        }
        finish()
    }
}
