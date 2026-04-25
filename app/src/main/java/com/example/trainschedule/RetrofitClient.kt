package com.example.trainschedule

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://69ea964f15c7e2d51269e9c0.mockapi.io/"

    val apiService: TrainApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrainApiService::class.java)
    }
}