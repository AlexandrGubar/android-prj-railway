package com.example.trainschedule

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

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

        when (seat.status) {
            SeatStatus.FREE -> {
                holder.cardSeat.setCardBackgroundColor(Color.WHITE)
                holder.cardSeat.strokeColor = Color.parseColor("#E5E5EA")
                holder.tvSeatNumber.setTextColor(Color.parseColor("#1C1C1E"))
            }
            SeatStatus.OCCUPIED -> {
                holder.cardSeat.setCardBackgroundColor(Color.parseColor("#F2F2F7"))
                holder.cardSeat.strokeColor = Color.TRANSPARENT
                holder.tvSeatNumber.setTextColor(Color.parseColor("#D1D1D6"))
            }
            SeatStatus.SELECTED -> {
                holder.cardSeat.setCardBackgroundColor(Color.parseColor("#5E35B1"))
                holder.cardSeat.strokeColor = Color.parseColor("#5E35B1")
                holder.tvSeatNumber.setTextColor(Color.WHITE)
            }
        }

        holder.itemView.setOnClickListener {
            if (seat.status == SeatStatus.OCCUPIED) return@setOnClickListener

            if (seat.status == SeatStatus.SELECTED) {
                seat.status = SeatStatus.FREE
                selectedSeatIndex = -1
                onSeatSelected(null)
            } else {
                if (selectedSeatIndex != -1) {
                    seats[selectedSeatIndex].status = SeatStatus.FREE
                }
                seat.status = SeatStatus.SELECTED
                selectedSeatIndex = position
                onSeatSelected(seat)
            }
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = seats.size

    fun updateSeats(newSeats: List<Seat>) {
        seats = newSeats
        selectedSeatIndex = -1
        notifyDataSetChanged()
    }
}