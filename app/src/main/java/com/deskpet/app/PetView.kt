package com.deskpet.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.deskpet.app.utils.PetAnimator
import com.deskpet.app.utils.PetDrawer

/**
 * 宠物自定义 View — 绘制 + 动画 + 触摸交互。
 * 可用于 Activity 内预览，也可嵌入悬浮窗。
 */
class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val animator = PetAnimator()
    private var petState: PetState = PetState()
    private var petSize: Float = 160f

    /** 触摸交互回调 */
    var onInteraction: ((String) -> Unit)? = null

    /** 上次点击时间，用于区分单击/双击 */
    private var lastClickTime = 0L

    init {
        animator.startBreathing { progress ->
            invalidate()
        }
    }

    /** 更新宠物状态并重绘 */
    fun updateState(state: PetState) {
        this.petState = state
        // 根据动画类型触发弹跳
        when (state.currentAnimation) {
            "eat", "happy", "love" -> {
                animator.bounce {
                    // 动画结束后回到呼吸模式
                    animator.startBreathing { invalidate() }
                }
            }
        }
        invalidate()
    }

    /** 设置宠物大小 */
    fun setPetSize(size: Float) {
        petSize = size
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        PetDrawer.draw(canvas, cx, cy, petSize, petState.mood, animator.progress)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val now = System.currentTimeMillis()

            when {
                // 双击 → 睡觉/唤醒
                now - lastClickTime < 400 -> {
                    onInteraction?.invoke("sleep")
                }
                // 长按（触摸在头部区域） → 抚摸
                event.y < height * 0.35f -> {
                    onInteraction?.invoke("pet")
                }
                // 点击 → 玩耍
                else -> {
                    onInteraction?.invoke("play")
                }
            }

            lastClickTime = now
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.release()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.progress.equals(Float.NaN) && animator.progress >= 0f) {
            // 重新开始呼吸动画
        }
    }
}