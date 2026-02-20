package com.sensorlog.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 임계값 설정을 SharedPreferences에 저장/조회하는 헬퍼
 */
class ThresholdPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "threshold_prefs"
        const val KEY_SENSOR_ID     = "sensor_id"
        const val KEY_TEMP_MIN      = "temp_min"
        const val KEY_TEMP_MAX      = "temp_max"
        const val KEY_HUMIDITY_MIN  = "humidity_min"
        const val KEY_HUMIDITY_MAX  = "humidity_max"
        const val KEY_MONITORING    = "monitoring_enabled"

        // 기본값
        const val DEFAULT_TEMP_MIN: Float     = 0f
        const val DEFAULT_TEMP_MAX: Float     = 40f
        const val DEFAULT_HUMIDITY_MIN: Float = 20f
        const val DEFAULT_HUMIDITY_MAX: Float = 80f
    }

    fun getSensorId(): String = prefs.getString(KEY_SENSOR_ID, "") ?: ""
    fun setSensorId(v: String) = prefs.edit().putString(KEY_SENSOR_ID, v).apply()

    fun getTempMin(): Float = prefs.getFloat(KEY_TEMP_MIN, DEFAULT_TEMP_MIN)
    fun setTempMin(v: Float) = prefs.edit().putFloat(KEY_TEMP_MIN, v).apply()

    fun getTempMax(): Float = prefs.getFloat(KEY_TEMP_MAX, DEFAULT_TEMP_MAX)
    fun setTempMax(v: Float) = prefs.edit().putFloat(KEY_TEMP_MAX, v).apply()

    fun getHumidityMin(): Float = prefs.getFloat(KEY_HUMIDITY_MIN, DEFAULT_HUMIDITY_MIN)
    fun setHumidityMin(v: Float) = prefs.edit().putFloat(KEY_HUMIDITY_MIN, v).apply()

    fun getHumidityMax(): Float = prefs.getFloat(KEY_HUMIDITY_MAX, DEFAULT_HUMIDITY_MAX)
    fun setHumidityMax(v: Float) = prefs.edit().putFloat(KEY_HUMIDITY_MAX, v).apply()

    fun isMonitoringEnabled(): Boolean = prefs.getBoolean(KEY_MONITORING, false)
    fun setMonitoringEnabled(v: Boolean) = prefs.edit().putBoolean(KEY_MONITORING, v).apply()
}
