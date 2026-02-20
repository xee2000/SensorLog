package com.sensorlog.ui.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sensorlog.adapter.SensorDataAdapter
import com.sensorlog.databinding.FragmentSensorLogBinding
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.viewmodel.SensorViewModel
import java.util.Calendar
import java.util.Date

/**
 * 화면 1: 기간별 센서 데이터 조회
 * - 센서 ID 입력
 * - 시작/종료 날짜 선택 (DatePicker)
 * - 빠른 필터 (오늘 / 3일 / 7일 / 30일)
 * - 결과 리스트 (시각, 온도, 습도)
 */
class SensorListFragment : Fragment() {

    private var _binding: FragmentSensorLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()
    private val adapter = SensorDataAdapter()

    private var startDate: Date = DateUtils.daysAgo(7)
    private var endDate: Date   = DateUtils.todayEnd()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSensorData.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSensorData.adapter = adapter

        updateDateButtons()
        setupListeners()
        observeViewModel()

        // 화면 진입 시 자동 조회
        search()
    }

    private fun setupListeners() {
        // 시작일 DatePicker
        binding.btnStartDate.setOnClickListener {
            val c = Calendar.getInstance().apply { time = startDate }
            DatePickerDialog(
                requireContext(),
                { _, y, m, d -> startDate = DateUtils.startOfDay(y, m, d); updateDateButtons() },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 종료일 DatePicker
        binding.btnEndDate.setOnClickListener {
            val c = Calendar.getInstance().apply { time = endDate }
            DatePickerDialog(
                requireContext(),
                { _, y, m, d -> endDate = DateUtils.endOfDay(y, m, d); updateDateButtons() },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 빠른 날짜 필터 칩 — 누르면 날짜 변경 후 즉시 조회
        binding.chipToday.setOnClickListener  { applyQuickRange(0);  search() }
        binding.chip3Days.setOnClickListener  { applyQuickRange(3);  search() }
        binding.chip7Days.setOnClickListener  { applyQuickRange(7);  search() }
        binding.chip30Days.setOnClickListener { applyQuickRange(30); search() }

        // 조회 버튼
        binding.btnSearch.setOnClickListener { search() }

        // SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener {
            search()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun applyQuickRange(days: Int) {
        startDate = if (days == 0) DateUtils.todayStart() else DateUtils.daysAgo(days)
        endDate   = DateUtils.todayEnd()
        updateDateButtons()
    }

    private fun updateDateButtons() {
        binding.btnStartDate.text = DateUtils.toDisplayDate(startDate)
        binding.btnEndDate.text   = DateUtils.toDisplayDate(endDate)
    }

    private fun search() {
        val sensorId = binding.etSensorId.text.toString().trim()
        viewModel.loadSensorData(
            sensorId  = sensorId,
            startIso  = DateUtils.toIso(startDate),
            endIso    = DateUtils.toIso(endDate)
        )
    }

    private fun observeViewModel() {
        viewModel.sensorDataLog.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.tvError.visibility     = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.tvError.visibility     = View.GONE
                    adapter.submitList(state.data.toMutableList())
                    binding.tvTotalCount.text = "총 ${state.data.size}건"
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.VISIBLE
                    binding.tvError.visibility     = View.GONE
                    adapter.submitList(emptyList())
                    binding.tvTotalCount.text = "총 0건"
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.tvError.visibility     = View.VISIBLE
                    binding.tvError.text           = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
