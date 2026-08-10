package com.deskpet.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.deskpet.app.data.PetRepository

/**
 * 宠物悬浮窗服务 — 使用 WindowManager 在其他应用上方显示宠物。
 */
class PetOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var petView: PetView
    private lateinit var repository: PetRepository
    private var layoutParams: WindowManager.LayoutParams? = null

    // 拖拽相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        repository = PetRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        showPetWindow()
        return START_STICKY
    }

    private fun showPetWindow() {
        petView = PetView(this).apply {
            setPetSize(150f)
            updateState(repository.petState.value)

            // 触摸事件 → 拖拽 + 交互
            setOnTouchListener { _, event -> handleTouch(event) }
        }

        val (savedX, savedY) = repository.getPosition()
        val displayMetrics = resources.displayMetrics

        layoutParams = WindowManager.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX > 0) savedX.toInt() else displayMetrics.widthPixels / 2 - 75
            y = if (savedY > 0) savedY.toInt() else displayMetrics.heightPixels / 3
        }

        windowManager.addView(petView, layoutParams)

        // 监听状态变化更新绘制
        repository.petState.let { flow ->
            // 定期拉取最新状态（避免内存泄漏，用简单轮询）
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams!!.x
                initialY = layoutParams!!.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                if (kotlin.math.abs(deltaX) > 5 || kotlin.math.abs(deltaY) > 5) {
                    isDragging = true
                    layoutParams!!.x = initialX + deltaX
                    layoutParams!!.y = initialY + deltaY
                    windowManager.updateViewLayout(petView, layoutParams)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // 不是拖拽，触发交互
                    val handled = petView.onTouchEvent(event)
                    petView.onInteraction = { action ->
                        handleInteraction(action)
                    }
                    if (!handled) {
                        // fallback: 触发 PetView 逻辑
                        handleInteraction("play")
                    }
                } else {
                    // 保存位置
                    repository.savePosition(
                        layoutParams!!.x.toFloat(),
                        layoutParams!!.y.toFloat()
                    )
                }
                return true
            }
        }
        return false
    }

    private fun handleInteraction(action: String) {
        repository.updateState { state ->
            when (action) {
                "feed" -> state.feed()
                "play" -> state.play()
                "pet" -> state.pet()
                "sleep" -> state.toggleSleep()
                else -> state
            }
        }
        petView.updateState(repository.petState.value)
    }

    private fun buildNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_running))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_overlay),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (::petView.isInitialized && layoutParams != null) {
            try { windowManager.removeView(petView) } catch (_: Exception) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "deskpet_overlay"
        private const val NOTIFICATION_ID = 1001
    }
}