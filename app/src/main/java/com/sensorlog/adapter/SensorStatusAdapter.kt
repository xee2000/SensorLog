package com.sensorlog.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sensorlog.databinding.ItemSensorStatusBinding
import com.sensorlog.model.SensorReading
import com.sensorlog.model.UiState
import com.sensorlog.util.DateUtils
import com.sensorlog.util.ThresholdPreferences

/**
 * 대시보드용 센서 상태 어댑터
 * - 각 행: 센서 ID, 온도/습도 값, 정상/위험 프로그레스바
 */
class SensorStatusAdapter(
    private val prefs: ThresholdPreferences
) : RecyclerView.Adapter<SensorStatusAdapter.ViewHolder>() {

    // 순서 유지를 위해 LinkedHashMap 사용
    private val items = mutableListOf<Pair<String, UiState<SensorReading>>>()

    fun submitData(data: Map<String, UiState<SensorReading>>) {
        items.clear()
        items.addAll(data.entries.map { it.key to it.value })
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemSensorStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (sensorId, state) = items[position]
        holder.bind(sensorId, state)
    }

    inner class ViewHolder(private val b: ItemSensorStatusBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(sensorId: String, state: UiState<SensorReading>) {
            b.tvSensorId.text = "📡  $sensorId"
            when (state) {
                is UiState.Success -> bindData(sensorId, state.data)
                is UiState.Loading -> bindPlaceholder("로딩 중...")
                else               -> bindPlaceholder("데이터 없음")
            }
        }

        private fun bindData(sensorId: String, data: SensorReading) {
            val tempMin = prefs.getTempMin(sensorId).toDouble()
            val tempMax = prefs.getTempMax(sensorId).toDouble()
            val humMin  = prefs.getHumidityMin(sensorId).toDouble()
            val humMax  = prefs.getHumidityMax(sensorId).toDouble()

            val tempOk = data.temperature in tempMin..tempMax
            val humOk  = data.humidity in humMin..humMax

            // 온도
            b.tvTempValue.text = "%.1f".format(data.temperature)
            b.tvTempRange.text = "허용 범위: %.0f ~ %.0f°C".format(tempMin, tempMax)
            applyStatus(b.tvTempBadge, b.pbTemp, tempOk)

            // 습도
            b.tvHumValue.text = "%.1f".format(data.humidity)
            b.tvHumRange.text = "허용 범위: %.0f ~ %.0f%%".format(humMin, humMax)
            applyStatus(b.tvHumBadge, b.pbHum, humOk)

            // 좌측 강조선
            val allOk = tempOk && humOk
            b.accentBar.setBackgroundColor(
                Color.parseColor(if (allOk) "#4CAF50" else "#F44336")
            )

            b.tvLastUpdated.text = DateUtils.formatTimestamp(data.time)
        }

        private fun bindPlaceholder(msg: String) {
            b.tvTempValue.text = "--"
            b.tvHumValue.text  = "--"
            b.tvTempRange.text = msg
            b.tvHumRange.text  = ""
            b.tvLastUpdated.text = ""
            b.accentBar.setBackgroundColor(Color.parseColor("#9E9E9E"))
            applyStatus(b.tvTempBadge, b.pbTemp, true)
            applyStatus(b.tvHumBadge, b.pbHum, true)
        }

        private fun applyStatus(
            badge: android.widget.TextView,
            bar: android.widget.ProgressBar,
            isOk: Boolean
        ) {
            val color = Color.parseColor(if (isOk) "#4CAF50" else "#F44336")
            // 배지
            badge.text = if (isOk) "정상" else "위험"
            badge.backgroundTintList = ColorStateList.valueOf(color)
            // 프로그레스바: 항상 100% + 색상으로만 상태 표현
            bar.progress = 100
            bar.progressTintList = ColorStateList.valueOf(color)
        }
    }
}
