package com.sensorlog.repository

import com.sensorlog.api.RetrofitClient
import com.sensorlog.model.PagedResponse
import com.sensorlog.model.Sensor
import com.sensorlog.model.SensorData
import com.sensorlog.model.SensorSummary
import com.sensorlog.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository {

    private val api = RetrofitClient.sensorApiService

    suspend fun getSensors(): UiState<List<Sensor>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSensors()
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

    suspend fun getSensorData(
        startDate: String? = null,
        endDate: String? = null,
        sensorId: String? = null,
        page: Int = 1,
        size: Int = 50
    ): UiState<PagedResponse<SensorData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSensorData(startDate, endDate, sensorId, page, size)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null || body.items.isEmpty()) UiState.Empty
                else UiState.Success(body)
            } else {
                UiState.Error("서버 오류: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
        }
    }

    suspend fun getSensorDataById(
        sensorId: String,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        size: Int = 50
    ): UiState<PagedResponse<SensorData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSensorDataById(sensorId, startDate, endDate, page, size)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null || body.items.isEmpty()) UiState.Empty
                else UiState.Success(body)
            } else {
                UiState.Error("서버 오류: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
        }
    }

    suspend fun getSensorSummary(
        startDate: String? = null,
        endDate: String? = null,
        sensorId: String? = null
    ): UiState<List<SensorSummary>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSensorSummary(startDate, endDate, sensorId)
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

    suspend fun getLatestSensorData(
        sensorId: String? = null
    ): UiState<List<SensorData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestSensorData(sensorId)
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
}
