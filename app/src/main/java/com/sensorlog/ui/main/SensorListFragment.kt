package com.sensorlog.ui.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensorlog.adapter.SensorDataAdapter
import com.sensorlog.databinding.FragmentSensorLogBinding
import com.sensorlog.model.SensorReading
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
 * - 결과 리스트: 30건씩 표시, 스크롤 끝 도달 시 추가 30건
 */
class SensorListFragment : Fragment() {

    private var _binding: FragmentSensorLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()
    private val adapter = SensorDataAdapter()

    private var startDate: Date = DateUtils.daysAgo(7)
    private var endDate: Date   = DateUtils.todayEnd()

    // 페이징 상태
    private var fullList: List<SensorReading> = emptyList()
    private var displayedCount = 0
    private val PAGE_SIZE = 30

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

        // 스크롤 끝 감지 → 다음 페이지 로드
        binding.rvSensorData.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1) && displayedCount < fullList.size) {
                    loadNextPage()
                }
            }
        })

        updateDateButtons()
        setupListeners()
        observeViewModel()
        search()
    }

    private fun setupListeners() {
        binding.btnStartDate.setOnClickListener {
            val c = Calendar.getInstance().apply { time = startDate }
            DatePickerDialog(
                requireContext(),
                { _, y, m, d -> startDate = DateUtils.startOfDay(y, m, d); updateDateButtons() },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnEndDate.setOnClickListener {
            val c = Calendar.getInstance().apply { time = endDate }
            DatePickerDialog(
                requireContext(),
                { _, y, m, d -> endDate = DateUtils.endOfDay(y, m, d); updateDateButtons() },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.chipToday.setOnClickListener  { applyQuickRange(0);  search() }
        binding.chip3Days.setOnClickListener  { applyQuickRange(3);  search() }
        binding.chip7Days.setOnClickListener  { applyQuickRange(7);  search() }
        binding.chip30Days.setOnClickListener { applyQuickRange(30); search() }

        binding.btnSearch.setOnClickListener { search() }

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

    fun refresh() = search()

    private fun search() {
        val sensorId = binding.etSensorId.text.toString().trim()
        fullList = emptyList()
        displayedCount = 0
        adapter.submitList(emptyList())
        viewModel.loadSensorData(
            sensorId = sensorId,
            startIso = DateUtils.toIso(startDate),
            endIso   = DateUtils.toIso(endDate)
        )
    }

    /** 다음 PAGE_SIZE 건을 어댑터에 추가 표시 */
    private fun loadNextPage() {
        if (displayedCount >= fullList.size) return
        val next = (displayedCount + PAGE_SIZE).coerceAtMost(fullList.size)
        adapter.submitList(fullList.subList(0, next).toMutableList())
        displayedCount = next
        updateCountText()
    }

    private fun updateCountText() {
        val total = fullList.size
        binding.tvTotalCount.text =
            if (displayedCount >= total) "총 ${total}건"
            else "총 ${total}건 중 ${displayedCount}건 표시"
    }

    private fun observeViewModel() {
        viewModel.sensorDataLog.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility  = View.VISIBLE
                    binding.tvEmpty.visibility      = View.GONE
                    binding.tvError.visibility      = View.GONE
                    binding.rvSensorData.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility  = View.GONE
                    binding.tvEmpty.visibility      = View.GONE
                    binding.tvError.visibility      = View.GONE
                    binding.rvSensorData.visibility = View.VISIBLE
                    // 전체 데이터 저장 후 첫 페이지만 표시
                    fullList = state.data
                    displayedCount = 0
                    loadNextPage()
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility  = View.GONE
                    binding.tvEmpty.visibility      = View.VISIBLE
                    binding.tvError.visibility      = View.GONE
                    binding.rvSensorData.visibility = View.GONE
                    fullList = emptyList()
                    displayedCount = 0
                    adapter.submitList(emptyList())
                    binding.tvTotalCount.text = "총 0건"
                }
                is UiState.Error -> {
                    binding.progressBar.visibility  = View.GONE
                    binding.tvEmpty.visibility      = View.GONE
                    binding.tvError.visibility      = View.VISIBLE
                    binding.rvSensorData.visibility = View.GONE
                    binding.tvError.text            = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
