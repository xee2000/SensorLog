package com.sensorlog.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlog.model.PagedResponse
import com.sensorlog.model.Sensor
import com.sensorlog.model.SensorData
import com.sensorlog.model.SensorSummary
import com.sensorlog.model.UiState
import com.sensorlog.repository.SensorRepository
import com.sensorlog.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Date

class SensorViewModel : ViewModel() {

    private val repository = SensorRepository()

    // 센서 목록
    private val _sensors = MutableLiveData<UiState<List<Sensor>>>()
    val sensors: LiveData<UiState<List<Sensor>>> = _sensors

    // 센서 데이터 리스트 (기간별)
    private val _sensorData = MutableLiveData<UiState<PagedResponse<SensorData>>>()
    val sensorData: LiveData<UiState<PagedResponse<SensorData>>> = _sensorData

    // 요약 통계
    private val _sensorSummary = MutableLiveData<UiState<List<SensorSummary>>>()
    val sensorSummary: LiveData<UiState<List<SensorSummary>>> = _sensorSummary

    // 최신 데이터
    private val _latestData = MutableLiveData<UiState<List<SensorData>>>()
    val latestData: LiveData<UiState<List<SensorData>>> = _latestData

    // 필터 상태
    private val _selectedStartDate = MutableLiveData<Date>(DateUtils.daysAgo(7))
    val selectedStartDate: LiveData<Date> = _selectedStartDate

    private val _selectedEndDate = MutableLiveData<Date>(DateUtils.todayEnd())
    val selectedEndDate: LiveData<Date> = _selectedEndDate

    private val _selectedSensorId = MutableLiveData<String?>(null)
    val selectedSensorId: LiveData<String?> = _selectedSensorId

    // 현재 페이지
    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false

    init {
        loadSensors()
        loadLatestData()
    }

    fun loadSensors() {
        viewModelScope.launch {
            _sensors.value = UiState.Loading
            _sensors.value = repository.getSensors()
        }
    }

    fun loadSensorData(resetPage: Boolean = true) {
        if (isLoading) return
        if (!resetPage && isLastPage) return

        if (resetPage) {
            currentPage = 1
            isLastPage = false
        }

        isLoading = true
        viewModelScope.launch {
            if (resetPage) _sensorData.value = UiState.Loading

            val startStr = DateUtils.formatForApi(_selectedStartDate.value ?: DateUtils.daysAgo(7))
            val endStr = DateUtils.formatForApi(_selectedEndDate.value ?: DateUtils.todayEnd())
            val sensorId = _selectedSensorId.value

            val result = repository.getSensorData(startStr, endStr, sensorId, currentPage)

            when (result) {
                is UiState.Success -> {
                    val response = result.data
                    if (resetPage) {
                        _sensorData.value = result
                    } else {
                        // 기존 데이터에 추가 (페이지네이션)
                        val existing = (_sensorData.value as? UiState.Success)?.data
                        val combined = existing?.copy(
                            items = existing.items + response.items,
                            page = response.page
                        ) ?: response
                        _sensorData.value = UiState.Success(combined)
                    }
                    isLastPage = currentPage >= response.pages
                    if (!isLastPage) currentPage++
                }
                else -> _sensorData.value = result
            }
            isLoading = false
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            _sensorSummary.value = UiState.Loading
            val startStr = DateUtils.formatForApi(_selectedStartDate.value ?: DateUtils.daysAgo(7))
            val endStr = DateUtils.formatForApi(_selectedEndDate.value ?: DateUtils.todayEnd())
            _sensorSummary.value = repository.getSensorSummary(startStr, endStr, _selectedSensorId.value)
        }
    }

    fun loadLatestData() {
        viewModelScope.launch {
            _latestData.value = UiState.Loading
            _latestData.value = repository.getLatestSensorData()
        }
    }

    fun loadNextPage() {
        loadSensorData(resetPage = false)
    }

    fun setDateRange(startDate: Date, endDate: Date) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
        loadSensorData(resetPage = true)
        loadSummary()
    }

    fun setSelectedSensor(sensorId: String?) {
        _selectedSensorId.value = sensorId
        loadSensorData(resetPage = true)
        loadSummary()
    }

    fun applyQuickFilter(days: Int) {
        val start = DateUtils.daysAgo(days)
        val end = DateUtils.todayEnd()
        setDateRange(start, end)
    }

    fun refresh() {
        loadLatestData()
        loadSensorData(resetPage = true)
        loadSummary()
    }
}
