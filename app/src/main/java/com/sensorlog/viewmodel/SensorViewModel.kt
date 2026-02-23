package com.sensorlog.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import com.sensorlog.repository.SensorRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SensorViewModel : ViewModel() {

    private val repository = SensorRepository()

    // --- 기간별 데이터 조회 화면 ---
    private val _sensorDataLog = MutableLiveData<UiState<List<SensorReading>>>()
    val sensorDataLog: LiveData<UiState<List<SensorReading>>> = _sensorDataLog

    // --- 전체 센서 최신값 (대시보드) Map<sensorId, UiState> ---
    private val _allLatestData = MutableLiveData<Map<String, UiState<SensorReading>>>()
    val allLatestData: LiveData<Map<String, UiState<SensorReading>>> = _allLatestData

    /**
     * 기간별 센서 데이터 조회
     */
    fun loadSensorData(sensorId: String, startIso: String, endIso: String) {
        if (sensorId.isBlank()) {
            _sensorDataLog.value = UiState.Error("센서 ID를 입력해 주세요.")
            return
        }
        viewModelScope.launch {
            _sensorDataLog.value = UiState.Loading
            _sensorDataLog.value = repository.getSensorData(sensorId, startIso, endIso)
        }
    }

    /**
     * 여러 센서의 최신값 병렬 조회 (대시보드용)
     */
    fun loadAllLatestData(sensorIds: List<String>) {
        if (sensorIds.isEmpty()) return
        viewModelScope.launch {
            // 모든 센서 로딩 상태로 먼저 표시
            _allLatestData.value = sensorIds.associateWith { UiState.Loading }
            // 병렬 API 호출
            val deferred = sensorIds.map { id -> id to async { repository.getLatestSensorData(id) } }
            _allLatestData.value = deferred.associate { (id, d) -> id to d.await() }
        }
    }
}
