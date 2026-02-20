package com.sensorlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sensorlog.databinding.ItemSensorDataBinding
import com.sensorlog.model.SensorData
import com.sensorlog.util.DateUtils

class SensorDataAdapter(
    private val onItemClick: (SensorData) -> Unit
) : ListAdapter<SensorData, SensorDataAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorDataBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSensorDataBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SensorData) {
            binding.apply {
                tvSensorName.text = item.sensorName.ifEmpty { item.sensorId }
                tvValue.text = String.format("%.2f %s", item.value, item.unit)
                tvTimestamp.text = DateUtils.formatTimestamp(item.timestamp)
                tvLocation.text = item.location ?: "-"
                tvStatus.text = item.status ?: "정상"

                // 상태에 따른 색상
                val statusColor = when (item.status?.lowercase()) {
                    "error", "fault" -> android.graphics.Color.parseColor("#F44336")
                    "warning" -> android.graphics.Color.parseColor("#FF9800")
                    else -> android.graphics.Color.parseColor("#4CAF50")
                }
                tvStatus.setTextColor(statusColor)
                viewStatusIndicator.setBackgroundColor(statusColor)

                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SensorData>() {
        override fun areItemsTheSame(oldItem: SensorData, newItem: SensorData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SensorData, newItem: SensorData) =
            oldItem == newItem
    }
}
