package com.clipboardreader

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.clipboardreader.bubble.BubbleService
import com.clipboardreader.reader.ReaderService
import java.util.Locale

/** Settings + manual-read screen. Most use happens via select/share/tile/bubble, not here. */
class MainActivity : AppCompatActivity() {

    private var updatingUi = false
    private var checkTts: TextToSpeech? = null

    private lateinit var swBubble: SwitchCompat
    private lateinit var tvVoiceStatus: TextView
    private lateinit var btnInstallVoices: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnReadClipboard).setOnClickListener { readClipboard() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            ReaderService.control(this, ReaderService.ACTION_STOP)
        }

        val rg = findViewById<RadioGroup>(R.id.rgLang)
        when (Prefs.langPref(this)) {
            Prefs.LANG_RO -> rg.check(R.id.rbRo)
            Prefs.LANG_RU -> rg.check(R.id.rbRu)
            else -> rg.check(R.id.rbAuto)
        }
        rg.setOnCheckedChangeListener { _, id ->
            Prefs.setLangPref(
                this,
                when (id) {
                    R.id.rbRo -> Prefs.LANG_RO
                    R.id.rbRu -> Prefs.LANG_RU
                    else -> Prefs.LANG_AUTO
                },
            )
        }

        val sb = findViewById<SeekBar>(R.id.sbSpeed)
        sb.max = 150
        sb.progress = (((Prefs.rate(this) - 0.5f) / 0.01f).toInt()).coerceIn(0, 150)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.setRate(this@MainActivity, 0.5f + progress * 0.01f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        swBubble = findViewById(R.id.swBubble)
        swBubble.setOnCheckedChangeListener { _, checked ->
            if (updatingUi) return@setOnCheckedChangeListener
            if (checked) enableBubble() else disableBubble()
        }

        tvVoiceStatus = findViewById(R.id.tvVoiceStatus)
        btnInstallVoices = findViewById(R.id.btnInstallVoices)
        btnInstallVoices.setOnClickListener { installVoices() }

        maybeRequestNotifications()
        checkVoices()
    }

    override fun onResume() {
        super.onResume()
        updatingUi = true
        swBubble.isChecked = Prefs.bubbleEnabled(this) && Settings.canDrawOverlays(this)
        updatingUi = false
    }

    private fun readClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = cm?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrBlank()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
        } else {
            ReaderService.read(this, text)
        }
    }

    private fun enableBubble() {
        if (Settings.canDrawOverlays(this)) {
            Prefs.setBubbleEnabled(this, true)
            BubbleService.start(this)
        } else {
            Toast.makeText(this, R.string.bubble_need_overlay, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
            updatingUi = true
            swBubble.isChecked = false
            updatingUi = false
        }
    }

    private fun disableBubble() {
        Prefs.setBubbleEnabled(this, false)
        BubbleService.stop(this)
    }

    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun checkVoices() {
        checkTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ro = runCatching {
                    checkTts?.isLanguageAvailable(Locale("ro", "RO")) ?: -2
                }.getOrDefault(-2)
                val ru = runCatching {
                    checkTts?.isLanguageAvailable(Locale("ru", "RU")) ?: -2
                }.getOrDefault(-2)
                val roOk = ro >= TextToSpeech.LANG_AVAILABLE
                val ruOk = ru >= TextToSpeech.LANG_AVAILABLE
                runOnUiThread { updateVoiceStatus(roOk, ruOk) }
            }
        }
    }

    private fun updateVoiceStatus(roOk: Boolean, ruOk: Boolean) {
        if (roOk && ruOk) {
            tvVoiceStatus.text = getString(R.string.voices_ok)
            btnInstallVoices.visibility = View.GONE
        } else {
            tvVoiceStatus.text = getString(R.string.voices_missing)
            btnInstallVoices.visibility = View.VISIBLE
        }
    }

    private fun installVoices() {
        val candidates = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        for (intent in candidates) {
            if (runCatching { startActivity(intent); true }.getOrDefault(false)) return
        }
        Toast.makeText(this, R.string.voices_missing, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        checkTts?.shutdown()
        checkTts = null
        super.onDestroy()
    }
}
