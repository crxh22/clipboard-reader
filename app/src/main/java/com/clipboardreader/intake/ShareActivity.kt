package com.clipboardreader.intake

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.clipboardreader.reader.ReaderService

/** Handles text shared from another app's Share sheet (ACTION_SEND, text/plain). */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            ReaderService.read(this, text)
        }
        finish()
    }
}
