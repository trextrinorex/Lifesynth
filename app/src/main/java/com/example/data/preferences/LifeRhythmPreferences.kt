package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class LifeRhythmPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("liferhythm_prefs", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isHealthConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_HEALTH_CONNECT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HEALTH_CONNECT_ENABLED, value).apply()

    var isEveningDigestEnabled: Boolean
        get() = prefs.getBoolean(KEY_EVENING_DIGEST_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EVENING_DIGEST_ENABLED, value).apply()

    var nightWindowStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_WINDOW_START, 21) // 9 PM
        set(value) = prefs.edit().putInt(KEY_NIGHT_WINDOW_START, value).apply()

    var nightWindowEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_WINDOW_END, 11) // 11 AM
        set(value) = prefs.edit().putInt(KEY_NIGHT_WINDOW_END, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_HEALTH_CONNECT_ENABLED = "key_health_connect_enabled"
        private const val KEY_EVENING_DIGEST_ENABLED = "key_evening_digest_enabled"
        private const val KEY_NIGHT_WINDOW_START = "key_night_window_start"
        private const val KEY_NIGHT_WINDOW_END = "key_night_window_end"
    }
}
