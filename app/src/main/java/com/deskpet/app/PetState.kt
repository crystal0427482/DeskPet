package com.deskpet.app

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 宠物状态数据类 — 包含饥饿度、开心度、疲劳度，以及当前的动画状态。
 * 所有值范围 [0, 100]。
 */
data class PetState(
    @SerializedName("hunger") val hunger: Int = 80,
    @SerializedName("happiness") val happiness: Int = 70,
    @SerializedName("fatigue") val fatigue: Int = 30,
    @SerializedName("lastUpdateTime") val lastUpdateTime: Long = System.currentTimeMillis(),
    @SerializedName("currentAnimation") val currentAnimation: String = "idle",
    @SerializedName("petX") val petX: Float = 0f,
    @SerializedName("petY") val petY: Float = 0f,
    @SerializedName("isSleeping") val isSleeping: Boolean = false
) {
    /** 每秒饥饿度自然下降量 */
    companion object {
        const val HUNGER_DECAY_PER_SEC = 0.05f
        const val HAPPINESS_DECAY_PER_SEC = 0.03f
        const val FATIGUE_INCREASE_PER_SEC = 0.02f
        const val FATIGUE_DECREASE_PER_SEC = 0.08f  // 睡觉时恢复
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100

        fun fromJson(json: String): PetState =
            try { Gson().fromJson(json, PetState::class.java) } catch (_: Exception) { PetState() }
    }

    fun toJson(): String = Gson().toJson(this)

    /**
     * 根据距离上次更新时间，计算自然衰减后的状态。
     */
    fun decayed(nowMs: Long = System.currentTimeMillis()): PetState {
        val elapsedSec = ((nowMs - lastUpdateTime) / 1000f).coerceAtMost(3600f) // 最多计算1小时
        if (elapsedSec <= 0f) return this

        val newHunger = if (isSleeping) hunger else
            (hunger - HUNGER_DECAY_PER_SEC * elapsedSec).toInt().coerceIn(MIN_VALUE, MAX_VALUE)

        val newHappiness = if (isSleeping) happiness else
            (happiness - HAPPINESS_DECAY_PER_SEC * elapsedSec).toInt().coerceIn(MIN_VALUE, MAX_VALUE)

        val newFatigue = if (isSleeping)
            (fatigue - FATIGUE_DECREASE_PER_SEC * elapsedSec).toInt().coerceIn(MIN_VALUE, MAX_VALUE)
        else
            (fatigue + FATIGUE_INCREASE_PER_SEC * elapsedSec).toInt().coerceIn(MIN_VALUE, MAX_VALUE)

        // 自动唤醒：疲劳恢复到很低
        val wake = isSleeping && newFatigue <= 10

        return copy(
            hunger = newHunger,
            happiness = newHappiness,
            fatigue = newFatigue,
            lastUpdateTime = nowMs,
            isSleeping = if (wake) false else isSleeping,
            currentAnimation = if (wake) "idle" else currentAnimation
        )
    }

    /** 喂食：+25饥饿度，+10开心度 */
    fun feed(): PetState = copy(
        hunger = (hunger + 25).coerceIn(MIN_VALUE, MAX_VALUE),
        happiness = (happiness + 10).coerceIn(MIN_VALUE, MAX_VALUE),
        currentAnimation = "eat",
        isSleeping = false
    )

    /** 玩耍：+20开心度，+15疲劳度 */
    fun play(): PetState = copy(
        happiness = (happiness + 20).coerceIn(MIN_VALUE, MAX_VALUE),
        fatigue = (fatigue + 15).coerceIn(MIN_VALUE, MAX_VALUE),
        currentAnimation = "happy",
        isSleeping = false
    )

    /** 抚摸：+15开心度，-5疲劳度 */
    fun pet(): PetState = copy(
        happiness = (happiness + 15).coerceIn(MIN_VALUE, MAX_VALUE),
        fatigue = (fatigue - 5).coerceIn(MIN_VALUE, MAX_VALUE),
        currentAnimation = "love",
        isSleeping = false
    )

    /** 睡觉：开始/切换睡眠状态 */
    fun toggleSleep(): PetState = if (isSleeping) {
        copy(isSleeping = false, currentAnimation = "idle")
    } else {
        copy(isSleeping = true, currentAnimation = "sleep")
    }

    /** 检查宠物是否饿了 */
    val isHungry: Boolean get() = hunger < 30

    /** 检查宠物是否不开心 */
    val isUnhappy: Boolean get() = happiness < 30

    /** 检查宠物是否太累了 */
    val isExhausted: Boolean get() = fatigue > 80 && !isSleeping

    /** 综合表情优先级：distress > sleep > hungry > unhappy > tired > normal */
    val mood: String get() = when {
        hunger < 15 -> "cry"
        isSleeping -> "sleep"
        isExhausted -> "tired"
        isHungry -> "hungry"
        isUnhappy -> "sad"
        else -> currentAnimation.takeIf { it != "eat" && it != "love" } ?: "idle"
    }
}