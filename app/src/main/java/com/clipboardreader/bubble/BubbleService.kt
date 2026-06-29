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
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.clipboardreader.MainActivity
import com.clipboardreader.Prefs
import com.clipboardreader.R
import com.clipboardreader.intake.ClipboardReadActivity
import com.clipboardreader.reader.PlaybackState
import com.clipboardreader.reader.ReaderService
import kotlin.math.abs

/**
 * Draggable floating button over other apps. Explicit touch handling (no GestureDetector):
 *  - single tap  : idle -> read clipboard; playing -> pause; paused -> resume
 *  - double tap  : open the app
 *  - long press  : pop out ⏪ / ⏩ (skip 2 words); auto-hides after a few seconds, and the
 *                  timer RESETS on each skip so you can chain skips
 *  - drag        : reposition; an "X" target appears at the bottom — drop on it to close the bubble
 */
class BubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubble: ImageView? = null
    private var controls: View? = null
    private var closeTarget: View? = null
    private val params = WindowManager.LayoutParams()
    private val main = Handler(Looper.getMainLooper())
    private val density get() = resources.displayMetrics.density

    private var isDown = false
    private var moved = false
    private var longPressFired = false
    private var startX = 0
    private var startY = 0
    private var downX = 0f
    private var downY = 0f
    private var lastTapUp = 0L

    private val longPressRun = Runnable { if (isDown && !moved) { longPressFired = true; showControls() } }
    private val singleTapRun = Runnable { onBubbleTap() }
    private val hideControlsRun = Runnable { removeControls() }
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
        val size = (density * 48).toInt()
        params.width = size
        params.height = size
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSLUCENT
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = (density * 200).toInt()

        val pad = (density * 11).toInt()
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
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                    moved = false
                    longPressFired = false
                    startX = params.x
                    startY = params.y
                    downX = e.rawX
                    downY = e.rawY
                    main.postDelayed(longPressRun, LONG_PRESS_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (!moved && (abs(dx) > TOUCH_SLOP || abs(dy) > TOUCH_SLOP)) {
                        moved = true
                        main.removeCallbacks(longPressRun)
                        removeControls()
                        showCloseTarget()
                    }
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager?.updateViewLayout(view, params)
                        highlightCloseTarget(isOverClose(e.rawX, e.rawY))
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDown = false
                    main.removeCallbacks(longPressRun)
                    if (moved) {
                        val overClose = isOverClose(e.rawX, e.rawY)
                        hideCloseTarget()
                        if (overClose) {
                            closeBubble()
                            return@setOnTouchListener true
                        }
                    } else if (!longPressFired) {
                        val now = e.eventTime
                        if (now - lastTapUp < DOUBLE_TAP_MS) {
                            lastTapUp = 0L
                            main.removeCallbacks(singleTapRun)
                            openApp()
                        } else {
                            lastTapUp = now
                            main.postDelayed(singleTapRun, DOUBLE_TAP_MS)
                        }
                    }
                }
            }
            true
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

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // --- Drag-to-close target ---

    private fun showCloseTarget() {
        if (closeTarget != null) return
        val wm = windowManager ?: return
        val size = (density * 64).toInt()
        val pad = (density * 16).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            setBackgroundResource(R.drawable.close_target_bg)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setPadding(pad, pad, pad, pad)
        }
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.y = (density * 90).toInt()
        wm.addView(view, lp)
        closeTarget = view
    }

    private fun highlightCloseTarget(over: Boolean) {
        val v = closeTarget ?: return
        v.scaleX = if (over) 1.3f else 1f
        v.scaleY = if (over) 1.3f else 1f
        v.backgroundTintList = if (over) ColorStateList.valueOf(0xFFE53935.toInt()) else null
    }

    private fun hideCloseTarget() {
        closeTarget?.let { windowManager?.removeView(it) }
        closeTarget = null
    }

    private fun isOverClose(rawX: Float, rawY: Float): Boolean {
        if (closeTarget == null) return false
        val dm = resources.displayMetrics
        val size = density * 64
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels - density * 90 - size / 2f
        val dx = rawX - cx
        val dy = rawY - cy
        return dx * dx + dy * dy < size * size
    }

    private fun closeBubble() {
        Prefs.setBubbleEnabled(this, false)
        stopSelf()
    }

    // --- Long-press back/forward panel (centered under the bubble, clamped to edges) ---

    private fun showControls() {
        if (controls != null) {
            resetHideTimer()
            return
        }
        val wm = windowManager ?: return
        val p6 = (density * 6).toInt()
        val btn = (density * 46).toInt()
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.controls_bg)
            setPadding(p6, p6, p6, p6)
            addView(skipButton(R.drawable.ic_skip_back, ReaderService.ACTION_SKIP_BACK, btn))
            addView(skipButton(R.drawable.ic_skip_fwd, ReaderService.ACTION_SKIP_FWD, btn))
        }
        val dm = resources.displayMetrics
        val bubbleSize = (density * 48).toInt()
        val panelW = btn * 2 + p6 * 2
        val panelH = btn + p6 * 2
        val gap = (density * 8).toInt()

        var x = params.x + bubbleSize / 2 - panelW / 2
        x = x.coerceIn(0, (dm.widthPixels - panelW).coerceAtLeast(0))
        var y = params.y + bubbleSize + gap
        if (y + panelH > dm.heightPixels) y = params.y - panelH - gap
        if (y < 0) y = 0

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = x
        lp.y = y
        wm.addView(bar, lp)
        controls = bar
        resetHideTimer()
    }

    private fun skipButton(icon: Int, action: String, size: Int): ImageView {
        val pad = (density * 9).toInt()
        return ImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener {
                ReaderService.control(this@BubbleService, action)
                resetHideTimer()
            }
        }
    }

    private fun resetHideTimer() {
        main.removeCallbacks(hideControlsRun)
        main.postDelayed(hideControlsRun, CONTROLS_TIMEOUT_MS)
    }

    private fun removeControls() {
        main.removeCallbacks(hideControlsRun)
        controls?.let { windowManager?.removeView(it) }
        controls = null
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
        removeControls()
        hideCloseTarget()
        bubble?.let { windowManager?.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.clipboardreader.BUBBLE_STOP"
        private const val CHANNEL_ID = "bubble"
        private const val NOTIF_ID = 2
        private const val CONTROLS_TIMEOUT_MS = 4000L
        private const val LONG_PRESS_MS = 450L
        private const val DOUBLE_TAP_MS = 300L
        private const val TOUCH_SLOP = 16

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
