package com.example.trainschedule

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

// Стани місця
enum class SeatStatus { FREE, OCCUPIED, SELECTED }
data class Seat(val number: Int, var status: SeatStatus)

class SeatAdapter(
    private var seats: List<Seat>,
    private val onSeatSelected: (Seat?) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    private var selectedSeatIndex: Int = -1

    class SeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardSeat: MaterialCardView = view.findViewById(R.id.cardSeat)
        val tvSeatNumber: TextView = view.findViewById(R.id.tvSeatNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.tvSeatNumber.text = seat.number.toString()

        // Фарбуємо квадратики залежно від статусу
        when (seat.status) {
            SeatStatus.FREE -> {
                holder.cardSeat.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Світло-зелений
                holder.cardSeat.strokeColor = Color.parseColor("#4CAF50")
                holder.tvSeatNumber.setTextColor(Color.parseColor("#1B5E20"))
            }
            SeatStatus.OCCUPIED -> {
                holder.cardSeat.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Світло-червоний
                holder.cardSeat.strokeColor = Color.parseColor("#EF5350")
                holder.tvSeatNumber.setTextColor(Color.parseColor("#D32F2F"))
            }
            SeatStatus.SELECTED -> {
                holder.cardSeat.setCardBackgroundColor(Color.parseColor("#2196F3")) // Синій
                holder.cardSeat.strokeColor = Color.parseColor("#0D47A1")
                holder.tvSeatNumber.setTextColor(Color.WHITE)
            }
        }

        // Обробка кліку
        holder.itemView.setOnClickListener {
            if (seat.status == SeatStatus.OCCUPIED) return@setOnClickListener // Зайняте не клікається

            // Якщо клікнули на вже обране - знімаємо вибір
            if (seat.status == SeatStatus.SELECTED) {
                seat.status = SeatStatus.FREE
                selectedSeatIndex = -1
                onSeatSelected(null)
            } else {
                // Знімаємо вибір з попереднього місця (щоб можна було обрати лише одне)
                if (selectedSeatIndex != -1) {
                    seats[selectedSeatIndex].status = SeatStatus.FREE
                }
                // Обираємо нове
                seat.status = SeatStatus.SELECTED
                selectedSeatIndex = position
                onSeatSelected(seat)
            }
            notifyDataSetChanged() // Оновлюємо сітку
        }
    }

    override fun getItemCount() = seats.size

    fun updateSeats(newSeats: List<Seat>) {
        seats = newSeats
        selectedSeatIndex = -1 // Скидаємо вибір при зміні вагона
        notifyDataSetChanged()
    }
}