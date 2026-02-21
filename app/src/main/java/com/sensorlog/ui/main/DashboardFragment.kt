package com.sensorlog.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sensorlog.R
import com.sensorlog.databinding.FragmentLiveDataBinding
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.util.ThresholdPreferences
import com.sensorlog.viewmodel.SensorViewModel

/**
 * 메인 화면: 현재 최신 센서 값 표시
 * - 상태 배너 (파란, 모두정상 / 경보발생)
 * - 온도 / 습도 카드 (큰 숫자 + 좌측 강조선)
 * - 범위 대비 SeekBar 시각화 + 상태 배지
 * - SwipeRefresh + 툴바 새로고침
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

        binding.swipeRefresh.setOnRefreshListener {
            fetchLatest()
            binding.swipeRefresh.isRefreshing = false
        }

        fetchLatest()
    }

    override fun onResume() {
        super.onResume()
        fetchLatest()
    }

    fun refresh() = fetchLatest()

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
                    updateBannerError()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.groupData.visibility   = View.GONE
                    binding.tvError.visibility     = View.VISIBLE
                    binding.tvError.text           = state.message
                    updateBannerError()
                }
            }
        }
    }

    private fun updateCards(data: SensorReading) {
        val tempMin = prefs.getTempMin().toDouble()
        val tempMax = prefs.getTempMax().toDouble()
        val humMin  = prefs.getHumidityMin().toDouble()
        val humMax  = prefs.getHumidityMax().toDouble()

        val tempOk = data.temperature in tempMin..tempMax
        val humOk  = data.humidity in humMin..humMax

        // ── 상태 배너 ──
        updateBanner(tempOk, humOk)

        // ── 온도 카드 ──
        binding.tvTemperatureValue.text = "%.1f".format(data.temperature)
        binding.tvTempRange.text = "⚙ 범위: %.0f ~ %.0f°C".format(tempMin, tempMax)
        updateBadge(binding.tvTempBadge, tempOk)
        updateAccent(binding.accentTemp, tempOk)
        updateSeekBars(
            binding.seekTempMin, binding.seekTempMax,
            data.temperature, tempMin, tempMax
        )

        // ── 습도 카드 ──
        binding.tvHumidityValue.text = "%.1f".format(data.humidity)
        binding.tvHumRange.text = "⚙ 범위: %.0f ~ %.0f%%".format(humMin, humMax)
        updateBadge(binding.tvHumBadge, humOk)
        updateAccent(binding.accentHum, humOk)
        updateSeekBars(
            binding.seekHumMin, binding.seekHumMax,
            data.humidity, humMin, humMax
        )

        // ── 공통 ──
        binding.tvSensorId.text    = "센서 ID: ${data.sensorId}"
        binding.tvLastUpdated.text = DateUtils.formatTimestamp(data.time)
    }

    private fun updateBanner(tempOk: Boolean, humOk: Boolean) {
        val allOk = tempOk && humOk
        binding.tvStatusValue.text = if (allOk) "모두 정상" else "경보 발생"
        binding.tvStatusDesc.text = if (allOk)
            "센서가 실시간으로 데이터를 수신 중입니다."
        else
            "임계값을 벗어난 센서가 있습니다. 확인하세요."
    }

    private fun updateBannerError() {
        binding.tvStatusValue.text = "연결 오류"
        binding.tvStatusDesc.text  = "서버에 연결할 수 없습니다."
    }

    private fun updateBadge(badge: android.widget.TextView, isOk: Boolean) {
        badge.text = if (isOk) "정상" else "초과"
        badge.setBackgroundResource(
            if (isOk) R.drawable.bg_status_badge else R.drawable.bg_badge_alert
        )
    }

    private fun updateAccent(accentView: View, isOk: Boolean) {
        accentView.setBackgroundColor(
            if (isOk) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        )
    }

    /**
     * ↓ seekMin: 현재값이 최솟값보다 얼마나 위에 있는지 (0%=최솟값, 100%=최댓값)
     * ↑ seekMax: 현재값이 최댓값보다 얼마나 아래에 있는지 (0%=최댓값, 100%=최솟값)
     */
    private fun updateSeekBars(
        seekMin: android.widget.SeekBar,
        seekMax: android.widget.SeekBar,
        value: Double, min: Double, max: Double
    ) {
        val range = (max - min).coerceAtLeast(0.001)
        val minPct = ((value - min) / range * 100).coerceIn(0.0, 100.0).toInt()
        val maxPct = ((max - value) / range * 100).coerceIn(0.0, 100.0).toInt()
        seekMin.progress = minPct
        seekMax.progress = maxPct
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
