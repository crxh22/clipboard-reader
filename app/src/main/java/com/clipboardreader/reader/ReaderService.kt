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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.clipboardreader.MainActivity
import com.clipboardreader.Prefs
import com.clipboardreader.R

/** Foreground service owning the TTS engine + the playback notification. */
class ReaderService : Service() {

    private lateinit var engine: SpeechEngine
    private var audioManager: AudioManager? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        engine = SpeechEngine(this) { st -> main.post { onEngineState(st) } }
        engine.init()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null || action == ACTION_READ) {
            val text = intent?.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
            if (text.isEmpty()) {
                stopSelf()
                return START_NOT_STICKY
            }
            startForeground(NOTIF_ID, buildNotification(PlaybackState.State.PLAYING))
            requestFocus()
            engine.start(text, Prefs.langPref(this), Prefs.rate(this))
            return START_NOT_STICKY
        }

        // Control actions: ensure we are foreground for the delivery, then act.
        startForeground(NOTIF_ID, buildNotification(PlaybackState.state))
        if (PlaybackState.state == PlaybackState.State.IDLE && action != ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_STOP -> engine.stop()
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.resume()
            ACTION_TOGGLE -> toggle()
            ACTION_SKIP_BACK -> engine.skipWords(-2)
            ACTION_SKIP_FWD -> engine.skipWords(2)
        }
        return START_NOT_STICKY
    }

    private fun toggle() {
        when (PlaybackState.state) {
            PlaybackState.State.PLAYING -> engine.pause()
            PlaybackState.State.PAUSED -> engine.resume()
            PlaybackState.State.IDLE -> {}
        }
    }

    private fun onEngineState(st: PlaybackState.State) {
        PlaybackState.update(st)
        when (st) {
            PlaybackState.State.PLAYING, PlaybackState.State.PAUSED ->
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(st))
            PlaybackState.State.IDLE -> {
                abandonFocus()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
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
    private fun abandonFocus() {
        audioManager?.abandonAudioFocus(null)
    }

    private fun buildNotification(st: PlaybackState.State): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val playing = st == PlaybackState.State.PLAYING
        val toggleAction = if (playing) ACTION_PAUSE else ACTION_RESUME
        val toggleLabel = getString(if (playing) R.string.action_pause else R.string.action_resume)
        val toggleIcon = if (playing) R.drawable.ic_pause else R.drawable.ic_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_speak)
            .setContentTitle(getString(if (playing) R.string.notif_reading else R.string.notif_paused))
            .setContentIntent(open)
            .addAction(toggleIcon, toggleLabel, controlPi(toggleAction, 1))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), controlPi(ACTION_STOP, 2))
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun controlPi(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            this, requestCode, Intent(this, ReaderService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_reading), NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val ACTION_READ = "com.clipboardreader.READ"
        const val ACTION_STOP = "com.clipboardreader.STOP"
        const val ACTION_PAUSE = "com.clipboardreader.PAUSE"
        const val ACTION_RESUME = "com.clipboardreader.RESUME"
        const val ACTION_TOGGLE = "com.clipboardreader.TOGGLE"
        const val ACTION_SKIP_BACK = "com.clipboardreader.SKIP_BACK"
        const val ACTION_SKIP_FWD = "com.clipboardreader.SKIP_FWD"
        private const val CHANNEL_ID = "reading"
        private const val NOTIF_ID = 1

        /** Start reading [text]. Call from a foreground context (an Activity) or an overlay app. */
        fun read(ctx: Context, text: String) {
            val intent = Intent(ctx, ReaderService::class.java)
                .setAction(ACTION_READ)
                .putExtra(EXTRA_TEXT, text)
            startService(ctx, intent)
        }

        /** Send a control action (pause/resume/toggle/skip/stop) to the running reader. */
        fun control(ctx: Context, action: String) {
            startService(ctx, Intent(ctx, ReaderService::class.java).setAction(action))
        }

        private fun startService(ctx: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }
}
