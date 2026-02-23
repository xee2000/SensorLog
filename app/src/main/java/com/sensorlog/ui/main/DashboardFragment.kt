package com.sensorlog.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sensorlog.adapter.SensorStatusAdapter
import com.sensorlog.databinding.FragmentLiveDataBinding
import com.sensorlog.model.UiState
import com.sensorlog.util.ThresholdPreferences
import com.sensorlog.viewmodel.SensorViewModel

/**
 * 현재 값 화면: 등록된 모든 센서의 최신 온습도를 한 화면에 표시
 * - 상태 배너 (모두정상 / 경보발생)
 * - 센서별 카드: 온도·습도 값 + 정상/위험 프로그레스바
 * - SwipeRefresh + 툴바 새로고침
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentLiveDataBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SensorViewModel by viewModels()
    private lateinit var prefs: ThresholdPreferences
    private lateinit var sensorAdapter: SensorStatusAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = ThresholdPreferences(requireContext())
        sensorAdapter = SensorStatusAdapter(prefs)

        binding.rvSensors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSensors.adapter = sensorAdapter

        observeViewModel()

        binding.swipeRefresh.setOnRefreshListener {
            fetchAll()
            binding.swipeRefresh.isRefreshing = false
        }

        fetchAll()
    }

    override fun onResume() {
        super.onResume()
        fetchAll()
    }

    fun refresh() = fetchAll()

    private fun fetchAll() {
        val ids = prefs.getSensorIds()
        if (ids.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = "설정 탭에서 센서 ID를 먼저 등록해 주세요."
            updateBannerError()
            return
        }
        viewModel.loadAllLatestData(ids)
    }

    private fun observeViewModel() {
        viewModel.allLatestData.observe(viewLifecycleOwner) { dataMap ->
            val loading = dataMap.values.any { it is UiState.Loading }

            if (loading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.rvSensors.visibility   = View.GONE
                binding.tvError.visibility     = View.GONE
                return@observe
            }

            binding.progressBar.visibility = View.GONE
            binding.tvError.visibility     = View.GONE
            binding.rvSensors.visibility   = View.VISIBLE

            sensorAdapter.submitData(dataMap)

            // 하나라도 임계값 초과 시 경보 배너
            val anyAlert = dataMap.entries.any { (id, state) ->
                if (state !is UiState.Success) return@any false
                val d = state.data
                val tempOk = d.temperature in
                    prefs.getTempMin(id).toDouble()..prefs.getTempMax(id).toDouble()
                val humOk = d.humidity in
                    prefs.getHumidityMin(id).toDouble()..prefs.getHumidityMax(id).toDouble()
                !tempOk || !humOk
            }
            updateBanner(!anyAlert)
        }
    }

    private fun updateBanner(allOk: Boolean) {
        binding.tvStatusValue.text = if (allOk) "모두 정상" else "경보 발생"
        binding.tvStatusDesc.text  = if (allOk)
            "모든 센서가 정상 범위 내에 있습니다."
        else
            "임계값을 벗어난 센서가 있습니다. 확인하세요."
    }

    private fun updateBannerError() {
        binding.tvStatusValue.text = "연결 오류"
        binding.tvStatusDesc.text  = "서버에 연결할 수 없습니다."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
