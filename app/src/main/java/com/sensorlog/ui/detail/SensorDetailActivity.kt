package com.sensorlog.ui.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// 현재 버전에서는 사용하지 않음 — 상세 정보는 LiveData 화면에서 직접 표시
class SensorDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
