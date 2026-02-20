package com.sensorlog.api

import com.sensorlog.model.PagedResponse
import com.sensorlog.model.Sensor
import com.sensorlog.model.SensorData
import com.sensorlog.model.SensorSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * FastAPI 서버 엔드포인트 정의
 * http://192.168.0.22:8000/docs 에서 실제 엔드포인트를 확인하여 수정하세요.
 */
interface SensorApiService {

    /**
     * 센서 목록 조회
     */
    @GET("sensors")
    suspend fun getSensors(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<Sensor>>

    /**
     * 기간별 센서 데이터 조회 (전체 센서)
     * 날짜 형식: YYYY-MM-DD 또는 YYYY-MM-DDTHH:MM:SS
     */
    @GET("sensor-data")
    suspend fun getSensorData(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("sensor_id") sensorId: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): Response<PagedResponse<SensorData>>

    /**
     * 특정 센서의 기간별 데이터 조회
     */
    @GET("sensors/{sensor_id}/data")
    suspend fun getSensorDataById(
        @Path("sensor_id") sensorId: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): Response<PagedResponse<SensorData>>

    /**
     * 기간별 센서 데이터 요약 (통계)
     */
    @GET("sensor-data/summary")
    suspend fun getSensorSummary(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("sensor_id") sensorId: String? = null
    ): Response<List<SensorSummary>>

    /**
     * 최신 센서 데이터 조회
     */
    @GET("sensor-data/latest")
    suspend fun getLatestSensorData(
        @Query("sensor_id") sensorId: String? = null
    ): Response<List<SensorData>>
}
