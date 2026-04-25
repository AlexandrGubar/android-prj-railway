package com.example.trainschedule

import com.google.gson.annotations.SerializedName

data class TrainRoute(
    @SerializedName("id") val id: String? = null,
    @SerializedName("tripId") val tripId: Int,
    @SerializedName("trainNumber") val trainNumber: String,
    @SerializedName("startStation") val startStation: String,
    @SerializedName("endStation") val endStation: String,
    @SerializedName("departureTime") val departureTime: String,
    @SerializedName("price") val price: Double,
    @SerializedName("trainType") val trainType: String,
    @SerializedName("carriageCount") val carriageCount: Int?,
    @SerializedName("departureDate") val departureDate: String?,
    @SerializedName("status") val status: String?
)
