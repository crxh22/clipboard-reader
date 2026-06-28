package com.clipboardreader.reader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clipboardreader.MainActivity
import com.clipboardreader.Prefs
import com.clipboardreader.R

/** Foreground service that owns the TTS engine and the "reading…" notification. */
class ReaderService : Service() {

    private lateinit var engine: SpeechEngine
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        engine = SpeechEngine(
            context = this,
            onComplete = { stopReading() },
            onFail = { stopReading() },
        )
        engine.init()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopReading()
            return START_NOT_STICKY
        }
        val text = intent?.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        requestFocus()
        engine.speak(
            SpeechEngine.Request(
                text = text,
                langOverride = Prefs.langPref(this),
                rate = Prefs.rate(this),
            )
        )
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun requestFocus() {
        audioManager?.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        )
    }

    @Suppress("DEPRECATION")
    private fun stopReading() {
        engine.stop()
        audioManager?.abandonAudioFocus(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ReaderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_speak)
            .setContentTitle(getString(R.string.notif_reading))
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.action_stop), stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_reading),
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val ACTION_STOP = "com.clipboardreader.STOP"
        private const val CHANNEL_ID = "reading"
        private const val NOTIF_ID = 1

        /** Start reading [text]. Must be called from a foreground context (an Activity). */
        fun read(ctx: Context, text: String) {
            val intent = Intent(ctx, ReaderService::class.java).putExtra(EXTRA_TEXT, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }
}
