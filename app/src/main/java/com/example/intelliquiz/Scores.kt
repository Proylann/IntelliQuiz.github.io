package com.example.intelliquiz.model

data class Score(
    val id: Int = 0,
    val username: String,
    val score: Int,
    val difficulty: String,  // Add the difficulty field
    val created_at: String = "", // or use LocalDateTime
    val updated_at: String = ""   // or use LocalDateTime
)
