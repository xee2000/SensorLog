package com.sensorlog.ui.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensorlog.R
import com.sensorlog.adapter.SensorDataAdapter
import com.sensorlog.adapter.SensorFilterAdapter
import com.sensorlog.databinding.FragmentSensorListBinding
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.viewmodel.SensorViewModel
import java.util.Calendar
import java.util.Date

class SensorListFragment : Fragment() {

    private var _binding: FragmentSensorListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SensorViewModel by viewModels()
    private lateinit var sensorDataAdapter: SensorDataAdapter
    private lateinit var sensorFilterAdapter: SensorFilterAdapter

    private var startDate: Date = DateUtils.daysAgo(7)
    private var endDate: Date = DateUtils.todayEnd()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupDateFilter()
        setupQuickFilters()
        observeViewModel()

        viewModel.loadSensorData()
        updateDateDisplay()
    }

    private fun setupRecyclerViews() {
        // 센서 데이터 리스트
        sensorDataAdapter = SensorDataAdapter { sensorData ->
            // 상세 화면으로 이동
            Toast.makeText(requireContext(), "${sensorData.sensorName}: ${sensorData.value} ${sensorData.unit}", Toast.LENGTH_SHORT).show()
        }

        binding.rvSensorData.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorDataAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                    ) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }

        // 센서 필터 칩
        sensorFilterAdapter = SensorFilterAdapter { sensor ->
            viewModel.setSelectedSensor(sensor?.id)
        }
        binding.rvSensorFilter.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = sensorFilterAdapter
        }
    }

    private fun setupDateFilter() {
        binding.btnStartDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = startDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    startDate = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                    }.time
                    updateDateDisplay()
                    applyDateFilter()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnEndDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = endDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    endDate = Calendar.getInstance().apply {
                        set(year, month, day, 23, 59, 59)
                    }.time
                    updateDateDisplay()
                    applyDateFilter()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupQuickFilters() {
        binding.chipToday.setOnClickListener {
            startDate = DateUtils.todayStart()
            endDate = DateUtils.todayEnd()
            updateDateDisplay()
            applyDateFilter()
        }
        binding.chip3Days.setOnClickListener {
            startDate = DateUtils.daysAgo(3)
            endDate = DateUtils.todayEnd()
            updateDateDisplay()
            applyDateFilter()
        }
        binding.chip7Days.setOnClickListener {
            startDate = DateUtils.daysAgo(7)
            endDate = DateUtils.todayEnd()
            updateDateDisplay()
            applyDateFilter()
        }
        binding.chip30Days.setOnClickListener {
            startDate = DateUtils.daysAgo(30)
            endDate = DateUtils.todayEnd()
            updateDateDisplay()
            applyDateFilter()
        }
    }

    private fun updateDateDisplay() {
        binding.btnStartDate.text = DateUtils.formatForDisplay(startDate)
        binding.btnEndDate.text = DateUtils.formatForDisplay(endDate)
    }

    private fun applyDateFilter() {
        viewModel.setDateRange(startDate, endDate)
    }

    private fun observeViewModel() {
        // 센서 목록 (필터 칩용)
        viewModel.sensors.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> sensorFilterAdapter.submitList(state.data)
                else -> {}
            }
        }

        // 센서 데이터
        viewModel.sensorData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    sensorDataAdapter.submitList(state.data.items)
                    binding.tvTotalCount.text = "총 ${state.data.total}건"
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    sensorDataAdapter.submitList(emptyList())
                    binding.tvTotalCount.text = "총 0건"
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
            }
        }

        // SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
