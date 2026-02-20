package com.sensorlog.repository

import com.sensorlog.api.RetrofitClient
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository {

    private val api = RetrofitClient.sensorApiService

    /**
     * GET /sensor/{sensor_id}?start=...&end=...
     * ISO 8601 형식 예: 2026-02-01T00:00:00
     */
    suspend fun getSensorData(
        sensorId: String,
        start: String,
        end: String
    ): UiState<List<SensorReading>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSensorData(sensorId, start, end)
            if (response.isSuccessful) {
                val body = response.body()
                if (body.isNullOrEmpty()) UiState.Empty
                else UiState.Success(body)
            } else {
                UiState.Error("서버 오류: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
        }
    }

    /**
     * GET /sensor/{sensor_id}/latest
     */
    suspend fun getLatestSensorData(
        sensorId: String
    ): UiState<SensorReading> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestSensorData(sensorId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) UiState.Empty
                else UiState.Success(body)
            } else {
                UiState.Error("서버 오류: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
        }
    }
}
