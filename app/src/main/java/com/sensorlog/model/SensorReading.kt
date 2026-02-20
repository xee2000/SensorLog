package com.sensorlog.model

import com.google.gson.annotations.SerializedName

/**
 * API 응답 모델 — GET /sensor/{sensor_id} 및 /sensor/{sensor_id}/latest
 * {
 *   "sensor_id": "string",
 *   "time":      "2026-02-20T05:28:54.563Z",
 *   "temperature": 25.3,
 *   "humidity":    60.1
 * }
 */
data class SensorReading(
    @SerializedName("sensor_id")  val sensorId: String = "",
    @SerializedName("time")       val time: String = "",
    @SerializedName("temperature") val temperature: Double = 0.0,
    @SerializedName("humidity")   val humidity: Double = 0.0
)
