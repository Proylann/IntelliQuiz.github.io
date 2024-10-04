package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.intelliquiz.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Leaderboards : AppCompatActivity() {

    private lateinit var leaderboardLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboards)

        // Initialize views and API service
        leaderboardLayout = findViewById(R.id.leaderboards)
        progressBar = findViewById(R.id.progressBar)
        apiService = RetrofitClient.apiService

        val homeButton: Button = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            val intent = Intent(this, Welcome_Page::class.java)
            startActivity(intent)
        }

        displayUsers()
    }

    private fun displayUsers() {
        // Show progress bar while loading
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getScores()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Hide progress bar when done

                    if (response.isSuccessful) {
                        val scores = response.body() ?: emptyList()
                        leaderboardLayout.removeAllViews()

                        scores.forEach { score ->
                            val userCard = layoutInflater.inflate(R.layout.score_card, leaderboardLayout, false)

                            val usernameText: TextView = userCard.findViewById(R.id.username)
                            val scoreText: TextView = userCard.findViewById(R.id.score)

                            usernameText.text = score.username
                            scoreText.text = "${score.score}"

                            leaderboardLayout.addView(userCard)
                        }
                    } else {
                        showErrorMessage("Error: ${response.message()}")
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showErrorMessage("Network Error: ${t.message}")
                }
            }
        }
    }

    private fun showErrorMessage(message: String) {
        leaderboardLayout.removeAllViews()
        val errorText = TextView(this).apply {
            text = message
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        leaderboardLayout.addView(errorText)
    }
}
