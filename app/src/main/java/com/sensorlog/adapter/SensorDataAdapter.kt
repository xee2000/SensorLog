package com.sensorlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sensorlog.databinding.ItemSensorReadingBinding
import com.sensorlog.model.SensorReading
import com.sensorlog.util.DateUtils

class SensorDataAdapter : ListAdapter<SensorReading, SensorDataAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorReadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val b: ItemSensorReadingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SensorReading) {
            b.tvTime.text        = DateUtils.formatTimestamp(item.time)
            b.tvTemperature.text = "%.1f°C".format(item.temperature)
            b.tvHumidity.text    = "%.1f%%".format(item.humidity)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SensorReading>() {
        override fun areItemsTheSame(o: SensorReading, n: SensorReading) = o.time == n.time && o.sensorId == n.sensorId
        override fun areContentsTheSame(o: SensorReading, n: SensorReading) = o == n
    }
}
