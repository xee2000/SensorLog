package com.sensorlog.api

import com.sensorlog.model.SensorReading
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * FastAPI 서버 엔드포인트
 * Base URL: http://192.168.0.22:8000/
 */
interface SensorApiService {

    /**
     * 기간별 센서 데이터 조회
     * GET /sensor/{sensor_id}?start=2026-02-01T00:00:00&end=2026-02-28T23:59:59
     */
    @GET("sensor/{sensor_id}")
    suspend fun getSensorData(
        @Path("sensor_id") sensorId: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<List<SensorReading>>

    /**
     * 최신 센서 데이터 조회
     * GET /sensor/{sensor_id}/latest
     */
    @GET("sensor/{sensor_id}/latest")
    suspend fun getLatestSensorData(
        @Path("sensor_id") sensorId: String
    ): Response<SensorReading>
}
