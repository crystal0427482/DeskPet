package com.deskpet.app.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import kotlin.math.*

/**
 * 宠物动画管理器 — 负责动画进度循环、帧驱动。
 */
class PetAnimator {

    private var animator: ValueAnimator? = null
    private var onFrame: ((Float) -> Unit)? = null

    /** 当前动画进度 [0,1] */
    var progress: Float = 0f
        private set

    /** 呼吸动画（慢速，无限循环） */
    fun startBreathing(onFrame: (Float) -> Unit) {
        this.onFrame = onFrame
        stop()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000L  // 3秒一个呼吸周期
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                onFrame(progress)
            }
            start()
        }
    }

    /** 快速弹跳动画（一次性） */
    fun bounce(onEnd: (() -> Unit)? = null) {
        stop()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400L
            repeatCount = 0
            interpolator = object : android.view.animation.Interpolator {
                override fun getInterpolation(t: Float): Float {
                    // 模拟弹跳
                    return (1 - exp(-t * 5f) * cos(t * 8f)).toFloat()
                }
            }
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                onFrame?.invoke(progress)
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationEnd(a: android.animation.Animator) {
                    onEnd?.invoke()
                }
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
            })
            start()
        }
    }

    /** 停止动画 */
    fun stop() {
        animator?.cancel()
        animator = null
    }

    /** 释放资源 */
    fun release() {
        stop()
        onFrame = null
    }
}