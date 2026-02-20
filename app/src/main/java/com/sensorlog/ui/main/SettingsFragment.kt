package com.sensorlog.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.sensorlog.databinding.FragmentThresholdBinding
import com.sensorlog.service.SensorMonitorService
import com.sensorlog.util.ThresholdPreferences

/**
 * 화면 3: 임계값 설정 + 모니터링 제어
 * - 센서 ID (기본값: S001)
 * - 온도 최솟값 / 최댓값
 * - 습도 최솟값 / 최댓값
 * - 저장 버튼
 * - 모니터링 시작 / 중지 버튼 (60초마다 백그라운드 체크)
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentThresholdBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: ThresholdPreferences

    // Android 13+ 알림 권한 요청
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startMonitoring()
            else Toast.makeText(requireContext(), "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThresholdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = ThresholdPreferences(requireContext())

        loadCurrentValues()
        updateMonitoringUi()

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnToggleMonitoring.setOnClickListener { toggleMonitoring() }
    }

    private fun loadCurrentValues() {
        binding.etSensorId.setText(prefs.getSensorId().ifBlank { "S001" })
        binding.etTempMin.setText(prefs.getTempMin().toInt().toString())
        binding.etTempMax.setText(prefs.getTempMax().toInt().toString())
        binding.etHumMin.setText(prefs.getHumidityMin().toInt().toString())
        binding.etHumMax.setText(prefs.getHumidityMax().toInt().toString())
    }

    private fun saveSettings() {
        val sensorId = binding.etSensorId.text.toString().trim()
        val tempMin  = binding.etTempMin.text.toString().toFloatOrNull()
        val tempMax  = binding.etTempMax.text.toString().toFloatOrNull()
        val humMin   = binding.etHumMin.text.toString().toFloatOrNull()
        val humMax   = binding.etHumMax.text.toString().toFloatOrNull()

        if (sensorId.isBlank()) {
            binding.etSensorId.error = "센서 ID를 입력해 주세요."
            return
        }
        if (tempMin == null || tempMax == null || humMin == null || humMax == null) {
            Toast.makeText(requireContext(), "모든 임계값을 올바르게 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (tempMin >= tempMax) {
            Toast.makeText(requireContext(), "온도 최솟값은 최댓값보다 작아야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (humMin >= humMax) {
            Toast.makeText(requireContext(), "습도 최솟값은 최댓값보다 작아야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.setSensorId(sensorId)
        prefs.setTempMin(tempMin);  prefs.setTempMax(tempMax)
        prefs.setHumidityMin(humMin); prefs.setHumidityMax(humMax)

        Toast.makeText(requireContext(), "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun toggleMonitoring() {
        if (prefs.isMonitoringEnabled()) {
            stopMonitoring()
        } else {
            // 저장 먼저
            saveSettings()
            requestNotificationPermissionAndStart()
        }
    }

    private fun requestNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) startMonitoring()
            else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        SensorMonitorService.start(requireContext())
        prefs.setMonitoringEnabled(true)
        updateMonitoringUi()
        Toast.makeText(requireContext(), "모니터링을 시작합니다. (60초 주기)", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        SensorMonitorService.stop(requireContext())
        prefs.setMonitoringEnabled(false)
        updateMonitoringUi()
        Toast.makeText(requireContext(), "모니터링을 중지합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun updateMonitoringUi() {
        val enabled = prefs.isMonitoringEnabled()
        binding.tvMonitoringStatus.text =
            if (enabled) "● 모니터링 중 (60초 주기)" else "○ 모니터링 중지됨"
        binding.tvMonitoringStatus.setTextColor(
            if (enabled) android.graphics.Color.parseColor("#4CAF50")
            else android.graphics.Color.parseColor("#9E9E9E")
        )
        binding.btnToggleMonitoring.text = if (enabled) "모니터링 중지" else "모니터링 시작"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
