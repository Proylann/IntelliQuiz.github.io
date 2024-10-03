package com.example.intelliquiz

import com.example.intelliquiz.model.Score
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/scores")
    suspend fun getScores(): Response<List<Score>>

    @POST("api/scores")
    suspend fun postScore(@Body score: Score): Response<Score>
}
