package com.rainan.quickcts

import android.content.Context
import android.content.SharedPreferences

/**
 * Quản lý cấu hình tùy chọn của người dùng trong QuickCTS (Lưu trữ qua SharedPreferences).
 */
object PreferencesManager {
    private const val PREFS_NAME = "quickcts_preferences"
    private const val KEY_DELAY_MS = "pref_delay_ms"
    private const val KEY_HAPTIC_FEEDBACK = "pref_haptic_feedback"

    const val DEFAULT_DELAY_MS = 300L
    const val MIN_DELAY_MS = 0L
    const val MAX_DELAY_MS = 1000L
    const val DEFAULT_HAPTIC = true

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Lấy độ trễ kích hoạt CTS (tính bằng milliseconds).
     */
    fun getDelayMs(context: Context): Long {
        return getPrefs(context).getLong(KEY_DELAY_MS, DEFAULT_DELAY_MS)
    }

    /**
     * Cập nhật độ trễ kích hoạt CTS.
     */
    fun setDelayMs(context: Context, delayMs: Long) {
        val clamped = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        getPrefs(context).edit().putLong(KEY_DELAY_MS, clamped).apply()
    }

    /**
     * Kiểm tra tùy chọn rung haptic khi kích hoạt thành công.
     */
    fun isHapticEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC)
    }

    /**
     * Bật/tắt tùy chọn rung haptic.
     */
    fun setHapticEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }
}
