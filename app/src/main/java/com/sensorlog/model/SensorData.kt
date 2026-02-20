package com.sensorlog.model

import com.google.gson.annotations.SerializedName

/**
 * 개별 센서 측정값 데이터 모델
 * FastAPI 서버의 응답 스키마에 맞게 수정하세요.
 */
data class SensorData(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("sensor_id")
    val sensorId: String = "",

    @SerializedName("sensor_name")
    val sensorName: String = "",

    @SerializedName("value")
    val value: Double = 0.0,

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("timestamp")
    val timestamp: String = "",  // ISO 8601: "2024-01-15T10:30:00"

    @SerializedName("location")
    val location: String? = null,

    @SerializedName("status")
    val status: String? = null
)

/**
 * 센서 목록 아이템 모델
 */
data class Sensor(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("type")
    val type: String = "",

    @SerializedName("location")
    val location: String? = null,

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("is_active")
    val isActive: Boolean = true
)

/**
 * API 페이지네이션 응답 래퍼
 */
data class PagedResponse<T>(
    @SerializedName("items")
    val items: List<T> = emptyList(),

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("size")
    val size: Int = 20,

    @SerializedName("pages")
    val pages: Int = 0
)

/**
 * 기간별 집계 데이터 모델
 */
data class SensorSummary(
    @SerializedName("sensor_id")
    val sensorId: String = "",

    @SerializedName("sensor_name")
    val sensorName: String = "",

    @SerializedName("min_value")
    val minValue: Double = 0.0,

    @SerializedName("max_value")
    val maxValue: Double = 0.0,

    @SerializedName("avg_value")
    val avgValue: Double = 0.0,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("unit")
    val unit: String = "",

    @SerializedName("start_time")
    val startTime: String = "",

    @SerializedName("end_time")
    val endTime: String = ""
)
