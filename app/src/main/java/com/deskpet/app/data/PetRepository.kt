package com.deskpet.app.data

import android.content.Context
import android.content.SharedPreferences
import com.deskpet.app.PetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 宠物数据仓库 — SharedPreferences 持久化 + StateFlow 实时状态
 */
class PetRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("deskpet_prefs", Context.MODE_PRIVATE)

    private val _petState = MutableStateFlow(loadState())
    val petState: StateFlow<PetState> = _petState.asStateFlow()

    /** 从本地加载状态，自动应用衰减 */
    private fun loadState(): PetState {
        val json = prefs.getString(KEY_PET_STATE, null)
        val raw = if (json != null) PetState.fromJson(json) else PetState()
        return raw.decayed()
    }

    /** 保存状态到本地，同时更新 StateFlow */
    fun saveState(state: PetState) {
        prefs.edit().putString(KEY_PET_STATE, state.toJson()).apply()
        _petState.value = state
    }

    /** 更新状态（立即保存） */
    fun updateState(transform: (PetState) -> PetState) {
        val newState = transform(_petState.value)
        saveState(newState)
    }

    /** 读取当前位置 */
    fun getPosition(): Pair<Float, Float> {
        val x = prefs.getFloat(KEY_PET_X, -1f)
        val y = prefs.getFloat(KEY_PET_Y, -1f)
        return x to y
    }

    /** 保存位置 */
    fun savePosition(x: Float, y: Float) {
        prefs.edit().putFloat(KEY_PET_X, x).putFloat(KEY_PET_Y, y).apply()
    }

    companion object {
        private const val KEY_PET_STATE = "pet_state"
        private const val KEY_PET_X = "pet_x"
        private const val KEY_PET_Y = "pet_y"
    }
}