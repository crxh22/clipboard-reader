package com.clipboardreader

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clipboardreader.bubble.BubbleService
import com.clipboardreader.reader.PlaybackState
import com.clipboardreader.reader.ReaderService
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Locale
import kotlin.math.abs

/** Player + settings screen. Reflects live playback state; most use happens via select/share/bubble. */
class MainActivity : AppCompatActivity() {

    private val main = Handler(Looper.getMainLooper())
    private var updatingUi = false
    private var checkTts: TextToSpeech? = null

    private lateinit var btnPlay: ImageView
    private lateinit var tvNowLabel: TextView
    private lateinit var tvPreview: TextView
    private lateinit var pbProgress: ProgressBar
    private lateinit var swBubble: MaterialSwitch
    private lateinit var tvVoiceStatus: TextView
    private lateinit var btnInstallVoices: Button

    private val speedPills = listOf(
        R.id.pill08 to 0.8f, R.id.pill10 to 1.0f, R.id.pill12 to 1.2f, R.id.pill15 to 1.5f,
    )
    private val langPills = listOf(
        R.id.pillAuto to Prefs.LANG_AUTO, R.id.pillRo to Prefs.LANG_RO, R.id.pillRu to Prefs.LANG_RU,
    )

    private val stateListener: (PlaybackState.State) -> Unit = { st -> runOnUiThread { updatePlaybackUi(st) } }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.KEY_BUBBLE) runOnUiThread { syncBubbleSwitch() }
    }

    private val progressPoller = object : Runnable {
        override fun run() {
            pbProgress.progress = (PlaybackState.progress * 100).toInt()
            if (PlaybackState.state == PlaybackState.State.PLAYING) main.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlay)
        tvNowLabel = findViewById(R.id.tvNowLabel)
        tvPreview = findViewById(R.id.tvPreview)
        pbProgress = findViewById(R.id.pbProgress)
        swBubble = findViewById(R.id.swBubble)
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus)
        btnInstallVoices = findViewById(R.id.btnInstallVoices)

        btnPlay.setOnClickListener { onPlayClick() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            ReaderService.control(this, ReaderService.ACTION_SKIP_BACK)
        }
        findViewById<ImageView>(R.id.btnFwd).setOnClickListener {
            ReaderService.control(this, ReaderService.ACTION_SKIP_FWD)
        }
        findViewById<Button>(R.id.btnReadClipboard).setOnClickListener { readClipboard() }
        btnInstallVoices.setOnClickListener { installVoices() }

        setupSpeedPills()
        setupLangPills()

        swBubble.setOnCheckedChangeListener { _, checked ->
            if (updatingUi) return@setOnCheckedChangeListener
            if (checked) enableBubble() else disableBubble()
        }

        maybeRequestNotifications()
        checkVoices()
    }

    override fun onStart() {
        super.onStart()
        PlaybackState.addListener(stateListener) // invoked immediately with current state
        Prefs.registerChangeListener(this, prefsListener)
    }

    override fun onResume() {
        super.onResume()
        syncBubbleSwitch()
    }

    override fun onStop() {
        PlaybackState.removeListener(stateListener)
        Prefs.unregisterChangeListener(this, prefsListener)
        main.removeCallbacks(progressPoller)
        super.onStop()
    }

    private fun onPlayClick() {
        when (PlaybackState.state) {
            PlaybackState.State.PLAYING -> ReaderService.control(this, ReaderService.ACTION_PAUSE)
            PlaybackState.State.PAUSED -> ReaderService.control(this, ReaderService.ACTION_RESUME)
            PlaybackState.State.IDLE -> readClipboard()
        }
    }

    private fun updatePlaybackUi(state: PlaybackState.State) {
        btnPlay.setImageResource(
            if (state == PlaybackState.State.PLAYING) R.drawable.ic_pause else R.drawable.ic_play
        )
        if (state == PlaybackState.State.IDLE) {
            tvNowLabel.visibility = View.GONE
            tvPreview.text = getString(R.string.ui_idle_hint)
            pbProgress.progress = (PlaybackState.progress * 100).toInt()
        } else {
            tvNowLabel.visibility = View.VISIBLE
            tvPreview.text = PlaybackState.text.ifBlank { getString(R.string.ui_now_reading) }
        }
        main.removeCallbacks(progressPoller)
        if (state == PlaybackState.State.PLAYING) main.post(progressPoller)
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

    private fun setupSpeedPills() {
        val current = Prefs.rate(this)
        val selectedId = speedPills.minByOrNull { abs(it.second - current) }?.first
        speedPills.forEach { (id, rate) ->
            val pill = findViewById<TextView>(id)
            pill.isSelected = (id == selectedId)
            pill.setOnClickListener {
                speedPills.forEach { findViewById<TextView>(it.first).isSelected = (it.first == id) }
                Prefs.setRate(this, rate)
            }
        }
    }

    private fun setupLangPills() {
        val current = Prefs.langPref(this)
        langPills.forEach { (id, lang) ->
            val pill = findViewById<TextView>(id)
            pill.isSelected = (lang == current)
            pill.setOnClickListener {
                langPills.forEach { findViewById<TextView>(it.first).isSelected = (it.first == id) }
                Prefs.setLangPref(this, lang)
            }
        }
    }

    private fun syncBubbleSwitch() {
        updatingUi = true
        swBubble.isChecked = Prefs.bubbleEnabled(this) && Settings.canDrawOverlays(this)
        updatingUi = false
    }

    private fun enableBubble() {
        if (Settings.canDrawOverlays(this)) {
            Prefs.setBubbleEnabled(this, true)
            BubbleService.start(this)
        } else {
            Toast.makeText(this, R.string.bubble_need_overlay, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
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
                val ro = runCatching { checkTts?.isLanguageAvailable(Locale("ro", "RO")) ?: -2 }.getOrDefault(-2)
                val ru = runCatching { checkTts?.isLanguageAvailable(Locale("ru", "RU")) ?: -2 }.getOrDefault(-2)
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
