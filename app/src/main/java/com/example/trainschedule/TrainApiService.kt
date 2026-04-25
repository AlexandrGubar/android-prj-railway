package com.example.trainschedule

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface TrainApiService {
    @GET("trains")
    suspend fun getTrains(): List<TrainRoute>

    @POST("trains")
    suspend fun addTrain(@Body train: TrainRoute): TrainRoute

    @DELETE("trains/{id}")
    suspend fun deleteTrain(@Path("id") id: String)

    @PUT("trains/{id}")
    suspend fun updateTrain(@Path("id") id: String, @Body train: TrainRoute): TrainRoute
}