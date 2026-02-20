package com.sensorlog.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sensorlog.databinding.FragmentLiveDataBinding
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.util.ThresholdPreferences
import com.sensorlog.viewmodel.SensorViewModel

/**
 * 화면 2: 현재 최신 센서 값 표시
 * - 온도 / 습도 카드 (큰 숫자)
 * - 임계값 대비 상태 표시 (정상 / 경고)
 * - 수동 새로고침 버튼
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentLiveDataBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by viewModels()
    private lateinit var prefs: ThresholdPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = ThresholdPreferences(requireContext())

        observeViewModel()

        binding.btnRefresh.setOnClickListener { fetchLatest() }
        binding.swipeRefresh.setOnRefreshListener {
            fetchLatest()
            binding.swipeRefresh.isRefreshing = false
        }

        fetchLatest()
    }

    override fun onResume() {
        super.onResume()
        // 탭 전환 시에도 최신값 갱신
        fetchLatest()
    }

    private fun fetchLatest() {
        val sensorId = prefs.getSensorId().ifBlank { "S001" }
        viewModel.loadLatestData(sensorId)
    }

    private fun observeViewModel() {
        viewModel.latestData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.groupData.visibility   = View.GONE
                    binding.tvError.visibility     = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.groupData.visibility   = View.VISIBLE
                    binding.tvError.visibility     = View.GONE
                    updateCards(state.data)
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.groupData.visibility   = View.GONE
                    binding.tvError.visibility     = View.VISIBLE
                    binding.tvError.text           = "데이터가 없습니다."
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.groupData.visibility   = View.GONE
                    binding.tvError.visibility     = View.VISIBLE
                    binding.tvError.text           = state.message
                }
            }
        }
    }

    private fun updateCards(data: SensorReading) {
        val tempMin = prefs.getTempMin().toDouble()
        val tempMax = prefs.getTempMax().toDouble()
        val humMin  = prefs.getHumidityMin().toDouble()
        val humMax  = prefs.getHumidityMax().toDouble()

        // 온도 카드
        binding.tvTemperatureValue.text = "%.1f°C".format(data.temperature)
        val tempOk = data.temperature in tempMin..tempMax
        binding.tvTemperatureStatus.text = if (tempOk) "정상" else "⚠️ 임계값 초과"
        binding.tvTemperatureStatus.setTextColor(
            if (tempOk) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        )
        binding.tvTemperatureRange.text = "범위: %.1f ~ %.1f°C".format(tempMin, tempMax)

        // 습도 카드
        binding.tvHumidityValue.text = "%.1f%%".format(data.humidity)
        val humOk = data.humidity in humMin..humMax
        binding.tvHumidityStatus.text = if (humOk) "정상" else "⚠️ 임계값 초과"
        binding.tvHumidityStatus.setTextColor(
            if (humOk) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        )
        binding.tvHumidityRange.text = "범위: %.1f ~ %.1f%%".format(humMin, humMax)

        // 공통
        binding.tvSensorId.text   = "센서 ID: ${data.sensorId}"
        binding.tvLastUpdated.text = "마지막 갱신: ${DateUtils.formatTimestamp(data.time)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
