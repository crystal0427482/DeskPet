package com.deskpet.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.deskpet.app.data.PetRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repository: PetRepository
    private lateinit var petPreviewView: PetView

    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = PetRepository(this)

        // 宠物预览
        petPreviewView = PetView(this).apply {
            setPetSize(140f)
            updateState(repository.petState.value)
        }
        findViewById<android.widget.FrameLayout>(R.id.petPreview).addView(
            petPreviewView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 状态条绑定
        val progressHunger = findViewById<android.widget.ProgressBar>(R.id.progressHunger)
        val progressHappiness = findViewById<android.widget.ProgressBar>(R.id.progressHappiness)
        val progressFatigue = findViewById<android.widget.ProgressBar>(R.id.progressFatigue)
        val tvHunger = findViewById<android.widget.TextView>(R.id.tvHunger)
        val tvHappiness = findViewById<android.widget.TextView>(R.id.tvHappiness)
        val tvFatigue = findViewById<android.widget.TextView>(R.id.tvFatigue)
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvStatus)
        val btnToggle = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnToggleService)

        // 监听状态变化更新 UI
        lifecycleScope.launch {
            repository.petState.collectLatest { state ->
                progressHunger.progress = state.hunger
                progressHappiness.progress = state.happiness
                progressFatigue.progress = state.fatigue
                tvHunger.text = "${state.hunger}%"
                tvHappiness.text = "${state.happiness}%"
                tvFatigue.text = "${state.fatigue}%"
                petPreviewView.updateState(state)

                // 表情提示
                val moodEmoji = when (state.mood) {
                    "sleep" -> "💤"
                    "happy", "love" -> "😸"
                    "eat" -> "🍖"
                    "hungry" -> "😿"
                    "sad", "cry" -> "😿"
                    "tired" -> "😾"
                    else -> "🐱"
                }
                tvStatus.text = if (isServiceRunning) "$moodEmoji 宠物运行中" else "宠物已关闭"
            }
        }

        // 启动/停止按钮
        btnToggle.setOnClickListener {
            if (isServiceRunning) {
                stopServices()
                btnToggle.text = getString(R.string.btn_start_pet)
            } else {
                if (checkOverlayPermission()) {
                    startServices()
                    btnToggle.text = getString(R.string.btn_stop_pet)
                }
            }
        }

        // 交互按钮
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFeed).setOnClickListener {
            repository.updateState { it.feed() }
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPlay).setOnClickListener {
            repository.updateState { it.play() }
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSleep).setOnClickListener {
            repository.updateState { it.toggleSleep() }
        }

        // 更新按钮文字
        updateToggleButton(btnToggle)
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return false
            }
        }
        return true
    }

    private fun startServices() {
        val overlayIntent = Intent(this, PetOverlayService::class.java)
        val serviceIntent = Intent(this, PetService::class.java)

        ContextCompat.startForegroundService(this, serviceIntent)
        ContextCompat.startForegroundService(this, overlayIntent)

        isServiceRunning = true
    }

    private fun stopServices() {
        stopService(Intent(this, PetOverlayService::class.java))
        stopService(Intent(this, PetService::class.java))
        isServiceRunning = false
    }

    private fun updateToggleButton(btn: com.google.android.material.button.MaterialButton) {
        btn.text = if (isServiceRunning)
            getString(R.string.btn_stop_pet)
        else
            getString(R.string.btn_start_pet)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 不清除服务，让它们继续后台运行
    }
}