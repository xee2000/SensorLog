package com.sensorlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.sensorlog.model.Sensor

class SensorFilterAdapter(
    private val onSensorSelected: (Sensor?) -> Unit
) : RecyclerView.Adapter<SensorFilterAdapter.ChipViewHolder>() {

    private val items = mutableListOf<Sensor?>()
    private var selectedPosition = 0

    fun submitList(sensors: List<Sensor>) {
        items.clear()
        items.add(null) // "전체" 항목
        items.addAll(sensors)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = Chip(parent.context).apply {
            isCheckable = true
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            }
        }
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val sensor = items[position]
        holder.bind(sensor, position == selectedPosition)
        holder.chip.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSensorSelected(sensor)
        }
    }

    override fun getItemCount() = items.size

    inner class ChipViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(sensor: Sensor?, isSelected: Boolean) {
            chip.text = sensor?.name ?: "전체"
            chip.isChecked = isSelected
        }
    }
}
