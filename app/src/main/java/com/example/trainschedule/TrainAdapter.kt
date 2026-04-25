package com.example.trainschedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TrainAdapter(
    var trains: List<TrainRoute>,
    var isStaffMode: Boolean = false,
    private val onTrainClick: (TrainRoute) -> Unit,
    private val onDeleteClick: (TrainRoute) -> Unit,
    private val onEditClick: (TrainRoute) -> Unit
) : RecyclerView.Adapter<TrainAdapter.TrainViewHolder>() {

    class TrainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTrainNumber: TextView = view.findViewById(R.id.tvTrainNumber)
        val tvRoute: TextView = view.findViewById(R.id.tvRoute)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_train, parent, false)
        return TrainViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainViewHolder, position: Int) {
        val train = trains[position]

        holder.tvTrainNumber.text = train.trainNumber
        holder.tvRoute.text = "${train.startStation} → ${train.endStation}"
        holder.tvTime.text = "Відправлення: ${train.departureTime}"
        holder.tvPrice.text = "${train.price} ₴"
        holder.tvDate.text = "Дата: ${train.departureDate ?: "не вказана"}"

        when (train.status) {
            "OpenForBooking" -> {
                holder.tvStatus.text = "🟢 ВІДКРИТО ДЛЯ БРОНЮВАННЯ"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#C8E6C9")
                )
            }
            "Boarding" -> {
                holder.tvStatus.text = "🟡 ЙДЕ ПОСАДКА"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#E65100"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFE0B2")
                )
            }
            "InTransit" -> {
                holder.tvStatus.text = "🔵 В ДОРОЗІ"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#0D47A1"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#B3E5FC")
                )
            }
            "Cancelled" -> {
                holder.tvStatus.text = "🔴 СКАСОВАНО"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFCDD2")
                )
            }
            else -> {
                holder.tvStatus.text = "⚪ НЕВІДОМИЙ СТАТУС"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#616161"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E0E0E0")
                )
            }
        }

        holder.btnDelete.visibility = if (isStaffMode) View.VISIBLE else View.GONE
        holder.btnEdit.visibility = if (isStaffMode) View.VISIBLE else View.GONE

        holder.btnDelete.setOnClickListener { onDeleteClick(train) }
        holder.btnEdit.setOnClickListener { onEditClick(train) }
        holder.itemView.setOnClickListener { onTrainClick(train) }
    }

    override fun getItemCount(): Int = trains.size

    fun updateData(newTrains: List<TrainRoute>) {
        trains = newTrains
        notifyDataSetChanged()
    }
}