package com.sensorlog.ui.detail

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.sensorlog.databinding.ActivitySensorDetailBinding
import com.sensorlog.model.SensorData
import com.sensorlog.util.DateUtils

class SensorDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorDetailBinding

    companion object {
        const val EXTRA_SENSOR_DATA = "extra_sensor_data"
        const val EXTRA_SENSOR_ID = "extra_sensor_id"
        const val EXTRA_SENSOR_NAME = "extra_sensor_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sensorId = intent.getStringExtra(EXTRA_SENSOR_ID) ?: ""
        val sensorName = intent.getStringExtra(EXTRA_SENSOR_NAME) ?: sensorId
        supportActionBar?.title = sensorName

        // Intent에서 SensorData 파싱 (직렬화 대신 개별 필드 전달)
        val value = intent.getDoubleExtra("value", 0.0)
        val unit = intent.getStringExtra("unit") ?: ""
        val timestamp = intent.getStringExtra("timestamp") ?: ""
        val location = intent.getStringExtra("location") ?: "-"
        val status = intent.getStringExtra("status") ?: "정상"

        binding.apply {
            tvDetailSensorId.text = "센서 ID: $sensorId"
            tvDetailSensorName.text = sensorName
            tvDetailValue.text = String.format("%.4f %s", value, unit)
            tvDetailTimestamp.text = DateUtils.formatTimestamp(timestamp)
            tvDetailLocation.text = "위치: $location"
            tvDetailStatus.text = "상태: $status"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
