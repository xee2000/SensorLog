package com.sensorlog.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sensorlog.BuildConfig
import com.sensorlog.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 설정된 서버 주소 표시
        binding.tvCurrentBaseUrl.text = BuildConfig.BASE_URL

        binding.btnSaveSettings.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "서버 주소 변경은 app/build.gradle의 BASE_URL을 수정 후 재빌드하세요.",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.tvAppVersion.text = "Version ${BuildConfig.VERSION_NAME}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
