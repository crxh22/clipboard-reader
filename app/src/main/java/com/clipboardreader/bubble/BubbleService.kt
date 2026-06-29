package com.clipboardreader.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.clipboardreader.MainActivity
import com.clipboardreader.Prefs
import com.clipboardreader.R
import com.clipboardreader.intake.ClipboardReadActivity
import com.clipboardreader.reader.PlaybackState
import com.clipboardreader.reader.ReaderService
import kotlin.math.abs

/**
 * Draggable floating button over other apps. Behaviour depends on playback state:
 * idle -> read the clipboard; playing -> pause; paused -> resume (acts like play/pause).
 * Works even where the selection menu is hidden (Facebook): copy -> tap bubble.
 */
class BubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubble: ImageView? = null
    private val params = WindowManager.LayoutParams()
    private val main = Handler(Looper.getMainLooper())

    private val stateListener: (PlaybackState.State) -> Unit = { st -> main.post { updateIcon(st) } }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) {
            addBubble()
            PlaybackState.addListener(stateListener)
        }
    }

    private fun addBubble() {
        if (bubble != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val size = (resources.displayMetrics.density * 48).toInt()
        params.width = size
        params.height = size
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSLUCENT
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = (resources.displayMetrics.density * 200).toInt()

        val pad = (resources.displayMetrics.density * 11).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_stat_speak)
            setBackgroundResource(R.drawable.bubble_bg)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setPadding(pad, pad, pad, pad)
        }
        attachTouch(view)
        wm.addView(view, params)
        bubble = view
    }

    private fun updateIcon(st: PlaybackState.State) {
        bubble?.setImageResource(
            when (st) {
                PlaybackState.State.PLAYING -> R.drawable.ic_pause
                PlaybackState.State.PAUSED -> R.drawable.ic_play
                PlaybackState.State.IDLE -> R.drawable.ic_stat_speak
            }
        )
    }

    private fun attachTouch(view: View) {
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onBubbleTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun onBubbleTap() {
        when (PlaybackState.state) {
            PlaybackState.State.PLAYING -> ReaderService.control(this, ReaderService.ACTION_PAUSE)
            PlaybackState.State.PAUSED -> ReaderService.control(this, ReaderService.ACTION_RESUME)
            PlaybackState.State.IDLE -> startActivity(
                Intent(this, ClipboardReadActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_speak)
            .setContentTitle(getString(R.string.bubble_active))
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_hide_bubble), stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_bubble), NotificationManager.IMPORTANCE_MIN)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setBubbleEnabled(this, false)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        PlaybackState.removeListener(stateListener)
        bubble?.let { windowManager?.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.clipboardreader.BUBBLE_STOP"
        private const val CHANNEL_ID = "bubble"
        private const val NOTIF_ID = 2

        fun start(ctx: Context) {
            val intent = Intent(ctx, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BubbleService::class.java))
        }
    }
}
