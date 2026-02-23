package com.sensorlog.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.sensorlog.databinding.FragmentThresholdBinding
import com.sensorlog.service.SensorMonitorService
import com.sensorlog.util.ThresholdPreferences

/**
 * 화면 3: 임계값 설정 + 모니터링 제어
 * - 센서 관리: 칩 목록에서 선택/추가/삭제
 * - 센서 ID 는 읽기 전용으로 표시만 함
 * - 선택된 센서의 온도·습도 임계값 설정
 * - 백그라운드 모니터링 시작/중지
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentThresholdBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: ThresholdPreferences

    private var selectedSensorId: String = ""

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

        binding.btnAddSensor.setOnClickListener { showAddSensorDialog() }
        binding.btnSave.setOnClickListener { saveThresholds() }
        binding.btnToggleMonitoring.setOnClickListener { toggleMonitoring() }

        refreshSensorChips()
        updateMonitoringUi()
    }

    // ── 센서 칩 목록 갱신 ────────────────────────────────────────────────────

    private fun refreshSensorChips() {
        binding.chipGroupSensors.removeAllViews()
        val ids = prefs.getSensorIds()

        ids.forEach { sensorId ->
            val chip = Chip(requireContext()).apply {
                text = sensorId
                isCloseIconVisible = true
                isCheckable = true
                setOnClickListener { selectSensor(sensorId) }
                setOnCloseIconClickListener { confirmRemoveSensor(sensorId) }
            }
            binding.chipGroupSensors.addView(chip)
        }

        // 이전에 선택됐던 센서가 있으면 유지, 없으면 첫 번째 선택
        val toSelect = if (ids.contains(selectedSensorId)) selectedSensorId
                       else ids.firstOrNull() ?: ""

        if (toSelect.isNotBlank()) {
            selectSensor(toSelect)
            // 해당 칩 체크 상태로
            for (i in 0 until binding.chipGroupSensors.childCount) {
                val chip = binding.chipGroupSensors.getChildAt(i) as? Chip
                if (chip?.text == toSelect) { chip.isChecked = true; break }
            }
        } else {
            binding.layoutThresholdConfig.visibility = View.GONE
        }
    }

    private fun selectSensor(sensorId: String) {
        selectedSensorId = sensorId
        binding.tvSelectedSensorId.text = sensorId
        binding.etTempMin.setText(prefs.getTempMin(sensorId).toInt().toString())
        binding.etTempMax.setText(prefs.getTempMax(sensorId).toInt().toString())
        binding.etHumMin.setText(prefs.getHumidityMin(sensorId).toInt().toString())
        binding.etHumMax.setText(prefs.getHumidityMax(sensorId).toInt().toString())
        binding.layoutThresholdConfig.visibility = View.VISIBLE
    }

    // ── 센서 추가 ────────────────────────────────────────────────────────────

    private fun showAddSensorDialog() {
        val et = EditText(requireContext()).apply {
            hint = "센서 ID 입력 (예: S002)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("센서 추가")
            .setView(et)
            .setPositiveButton("추가") { _, _ ->
                val id = et.text.toString().trim()
                when {
                    id.isBlank() ->
                        Toast.makeText(requireContext(), "센서 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    prefs.getSensorIds().contains(id) ->
                        Toast.makeText(requireContext(), "이미 등록된 센서입니다.", Toast.LENGTH_SHORT).show()
                    else -> {
                        prefs.addSensorId(id)
                        refreshSensorChips()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 센서 삭제 ────────────────────────────────────────────────────────────

    private fun confirmRemoveSensor(sensorId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("센서 삭제")
            .setMessage("'$sensorId' 를 목록에서 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                prefs.removeSensorId(sensorId)
                if (selectedSensorId == sensorId) selectedSensorId = ""
                refreshSensorChips()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 임계값 저장 ──────────────────────────────────────────────────────────

    private fun saveThresholds() {
        if (selectedSensorId.isBlank()) {
            Toast.makeText(requireContext(), "센서를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val tempMin = binding.etTempMin.text.toString().toFloatOrNull()
        val tempMax = binding.etTempMax.text.toString().toFloatOrNull()
        val humMin  = binding.etHumMin.text.toString().toFloatOrNull()
        val humMax  = binding.etHumMax.text.toString().toFloatOrNull()

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

        prefs.setTempMin(selectedSensorId, tempMin)
        prefs.setTempMax(selectedSensorId, tempMax)
        prefs.setHumidityMin(selectedSensorId, humMin)
        prefs.setHumidityMax(selectedSensorId, humMax)

        Toast.makeText(requireContext(), "[$selectedSensorId] 임계값이 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // ── 모니터링 ─────────────────────────────────────────────────────────────

    private fun toggleMonitoring() {
        if (prefs.isMonitoringEnabled()) stopMonitoring()
        else requestNotificationPermissionAndStart()
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
