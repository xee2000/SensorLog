package com.sensorlog.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import com.sensorlog.repository.SensorRepository
import com.sensorlog.util.DateUtils
import kotlinx.coroutines.launch

class SensorViewModel : ViewModel() {

    private val repository = SensorRepository()

    // --- 기간별 데이터 조회 화면 ---
    private val _sensorDataLog = MutableLiveData<UiState<List<SensorReading>>>()
    val sensorDataLog: LiveData<UiState<List<SensorReading>>> = _sensorDataLog

    // --- 최신값 화면 ---
    private val _latestData = MutableLiveData<UiState<SensorReading>>()
    val latestData: LiveData<UiState<SensorReading>> = _latestData

    /**
     * 기간별 센서 데이터 조회
     * @param sensorId 센서 ID
     * @param startIso ISO 8601 시작시각 (예: 2026-02-01T00:00:00)
     * @param endIso   ISO 8601 종료시각 (예: 2026-02-28T23:59:59)
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
     * 최신 센서 데이터 조회
     * @param sensorId 센서 ID (ThresholdPreferences에서 가져온 값)
     */
    fun loadLatestData(sensorId: String) {
        if (sensorId.isBlank()) {
            _latestData.value = UiState.Error("설정 탭에서 센서 ID를 먼저 입력해 주세요.")
            return
        }
        viewModelScope.launch {
            _latestData.value = UiState.Loading
            _latestData.value = repository.getLatestSensorData(sensorId)
        }
    }
}
