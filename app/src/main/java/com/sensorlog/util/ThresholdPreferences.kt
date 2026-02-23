package com.sensorlog.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 임계값 설정을 SharedPreferences에 저장/조회하는 헬퍼
 * - 여러 센서 ID 관리 (KEY_SENSOR_IDS: 콤마 구분)
 * - 센서별 임계값: "temp_min_{sensorId}" 형태의 키
 */
class ThresholdPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME      = "threshold_prefs"
        private const val KEY_SENSOR_IDS  = "sensor_ids"
        const val KEY_MONITORING          = "monitoring_enabled"

        const val DEFAULT_TEMP_MIN: Float = 0f
        const val DEFAULT_TEMP_MAX: Float = 40f
        const val DEFAULT_HUM_MIN: Float  = 20f
        const val DEFAULT_HUM_MAX: Float  = 80f
    }

    // ── 센서 ID 목록 ───────────────────────────────────────────────────────────

    fun getSensorIds(): List<String> {
        if (prefs.contains(KEY_SENSOR_IDS)) {
            val raw = prefs.getString(KEY_SENSOR_IDS, "") ?: ""
            return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        // 구버전 단일 sensor_id 마이그레이션
        val legacy = prefs.getString("sensor_id", "") ?: ""
        return if (legacy.isNotBlank()) listOf(legacy) else listOf("S001", "S002")
    }

    fun setSensorIds(ids: List<String>) =
        prefs.edit().putString(KEY_SENSOR_IDS, ids.joinToString(",")).apply()

    fun addSensorId(id: String) {
        val ids = getSensorIds().toMutableList()
        if (!ids.contains(id)) {
            ids.add(id)
            setSensorIds(ids)
        }
    }

    fun removeSensorId(id: String) {
        setSensorIds(getSensorIds().filter { it != id })
    }

    /** 첫 번째 센서 ID (서비스/하위 호환) */
    fun getSensorId(): String = getSensorIds().firstOrNull() ?: "S001"

    // ── 센서별 임계값 ──────────────────────────────────────────────────────────

    fun getTempMin(sensorId: String): Float =
        prefs.getFloat("temp_min_$sensorId", DEFAULT_TEMP_MIN)
    fun setTempMin(sensorId: String, v: Float) =
        prefs.edit().putFloat("temp_min_$sensorId", v).apply()

    fun getTempMax(sensorId: String): Float =
        prefs.getFloat("temp_max_$sensorId", DEFAULT_TEMP_MAX)
    fun setTempMax(sensorId: String, v: Float) =
        prefs.edit().putFloat("temp_max_$sensorId", v).apply()

    fun getHumidityMin(sensorId: String): Float =
        prefs.getFloat("hum_min_$sensorId", DEFAULT_HUM_MIN)
    fun setHumidityMin(sensorId: String, v: Float) =
        prefs.edit().putFloat("hum_min_$sensorId", v).apply()

    fun getHumidityMax(sensorId: String): Float =
        prefs.getFloat("hum_max_$sensorId", DEFAULT_HUM_MAX)
    fun setHumidityMax(sensorId: String, v: Float) =
        prefs.edit().putFloat("hum_max_$sensorId", v).apply()

    // ── 모니터링 상태 ──────────────────────────────────────────────────────────

    fun isMonitoringEnabled(): Boolean = prefs.getBoolean(KEY_MONITORING, false)
    fun setMonitoringEnabled(v: Boolean) = prefs.edit().putBoolean(KEY_MONITORING, v).apply()
}
