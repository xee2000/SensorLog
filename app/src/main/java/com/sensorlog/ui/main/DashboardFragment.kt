package com.sensorlog.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.sensorlog.databinding.FragmentDashboardBinding
import com.sensorlog.model.SensorData
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.viewmodel.SensorViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeViewModel()
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            legend.isEnabled = true
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
            }
        }
    }

    private fun observeViewModel() {
        // 최신 데이터 카드
        viewModel.latestData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressDashboard.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressDashboard.visibility = View.GONE
                    updateLatestCards(state.data)
                    updateChart(state.data)
                }
                is UiState.Empty -> {
                    binding.progressDashboard.visibility = View.GONE
                    binding.tvNoDashboardData.visibility = View.VISIBLE
                }
                is UiState.Error -> {
                    binding.progressDashboard.visibility = View.GONE
                    binding.tvNoDashboardData.visibility = View.VISIBLE
                    binding.tvNoDashboardData.text = state.message
                }
            }
        }

        // 요약 통계
        viewModel.sensorSummary.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                val summary = state.data.firstOrNull() ?: return@observe
                binding.tvAvgValue.text = String.format("평균: %.2f %s", summary.avgValue, summary.unit)
                binding.tvMinValue.text = String.format("최솟값: %.2f", summary.minValue)
                binding.tvMaxValue.text = String.format("최댓값: %.2f", summary.maxValue)
                binding.tvCountValue.text = "측정 횟수: ${summary.count}회"
            }
        }

        binding.btnRefreshDashboard.setOnClickListener {
            viewModel.loadLatestData()
            viewModel.loadSummary()
        }
    }

    private fun updateLatestCards(data: List<SensorData>) {
        if (data.isEmpty()) {
            binding.tvNoDashboardData.visibility = View.VISIBLE
            return
        }
        binding.tvNoDashboardData.visibility = View.GONE

        // 최신 측정값 카드 업데이트
        val latest = data.firstOrNull()
        latest?.let {
            binding.tvLatestSensorName.text = it.sensorName.ifEmpty { it.sensorId }
            binding.tvLatestValue.text = String.format("%.2f %s", it.value, it.unit)
            binding.tvLatestTime.text = DateUtils.formatTimestamp(it.timestamp)
        }
    }

    private fun updateChart(data: List<SensorData>) {
        if (data.isEmpty()) return

        // 센서별로 그룹화
        val grouped = data.groupBy { it.sensorId }
        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0")
        )

        val dataSets = grouped.entries.mapIndexed { index, (sensorId, readings) ->
            val entries = readings.mapIndexed { i, d ->
                Entry(i.toFloat(), d.value.toFloat())
            }
            LineDataSet(entries, readings.firstOrNull()?.sensorName ?: sensorId).apply {
                color = colors[index % colors.size]
                setCircleColor(colors[index % colors.size])
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
        }

        binding.lineChart.apply {
            this.data = LineData(dataSets)
            animateX(500)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
