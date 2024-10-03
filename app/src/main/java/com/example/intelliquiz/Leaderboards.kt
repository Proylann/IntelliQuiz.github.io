package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.intelliquiz.api.RetrofitClient
import com.example.intelliquiz.model.Score
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Leaderboards : AppCompatActivity() {

    private lateinit var leaderboardLayout: LinearLayout
    private lateinit var apiService: ApiService // Define your API service interface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaderboards)

        // Initialize your API service here (Retrofit setup)
        apiService = RetrofitClient.apiService // Access the apiService from RetrofitClient

        val homeButton: Button = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            val intent = Intent(this, Welcome_Page::class.java)
            startActivity(intent)
        }

        leaderboardLayout = findViewById(R.id.leaderboards)
        displayUsers()
    }

    private fun displayUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Call the getScores method
                val response = apiService.getScores()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val scores = response.body() ?: emptyList()

                        leaderboardLayout.removeAllViews() // Clear existing views
                        scores.forEach { score ->
                            val userEntry = TextView(this@Leaderboards).apply {
                                text = "${score.username} - Score: ${score.score}"
                                textSize = 18f
                                setPadding(16, 16, 16, 16)
                            }
                            leaderboardLayout.addView(userEntry)
                        }
                    } else {
                        // Handle error response
                        leaderboardLayout.removeAllViews()
                        val errorText = TextView(this@Leaderboards).apply {
                            text = "Error: ${response.message()}"
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                        }
                        leaderboardLayout.addView(errorText)
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    // Handle failure
                    leaderboardLayout.removeAllViews()
                    val errorText = TextView(this@Leaderboards).apply {
                        text = "Network Error: ${t.message}"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    leaderboardLayout.addView(errorText)
                }
            }
        }
    }
}
