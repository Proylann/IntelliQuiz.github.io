package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.intelliquiz.api.RetrofitClient
import com.example.intelliquiz.model.Score
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Leaderboards : AppCompatActivity() {

    private lateinit var leaderboardLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var difficultyText: TextView

    private lateinit var apiService: ApiService

    private val difficulties = arrayOf("Easy", "Medium", "Difficult")
    private var currentDifficultyIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboards)

        // Initialize views and API service
        leaderboardLayout = findViewById(R.id.leaderboards)
        progressBar = findViewById(R.id.progressBar)
        difficultyText = findViewById(R.id.difficultyText)
        apiService = RetrofitClient.apiService

        // Home button functionality
        val homeButton: Button = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            startActivity(Intent(this, Welcome_Page::class.java))
        }

        // Difficulty change buttons
        val prevDifficulty: Button = findViewById(R.id.prevDifficulty)
        val nextDifficulty: Button = findViewById(R.id.nextDifficulty)

        prevDifficulty.setOnClickListener {
            changeDifficulty(-1) // Move to previous difficulty
        }

        nextDifficulty.setOnClickListener {
            changeDifficulty(1) // Move to next difficulty
        }

        // Initial display
        displayUsers(difficulties[currentDifficultyIndex])
    }

    private fun changeDifficulty(direction: Int) {
        currentDifficultyIndex = (currentDifficultyIndex + direction).coerceIn(0, difficulties.size - 1)
        difficultyText.text = difficulties[currentDifficultyIndex]
        displayUsers(difficulties[currentDifficultyIndex]) // Fetch and display the new difficulty scores
    }

    private fun displayUsers(difficulty: String) {
        // Show progress bar while loading
        progressBar.visibility = View.VISIBLE
        leaderboardLayout.removeAllViews() // Clear previous scores

        // Fetch scores for the selected difficulty
        fetchScores(difficulty)
    }

    private fun fetchScores(difficulty: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getScores(difficulty) // Fetching scores based on difficulty
                if (response.isSuccessful) {
                    // Ensure the response body is of type List<Score>
                    val scores = response.body() ?: emptyList()

                    // Create a mutable list to hold filtered scores
                    val filteredScores = mutableListOf<Score>()

                    // Manually filter scores using a for loop
                    for (score in scores) {
                        if (score.score > 0) {
                            filteredScores.add(score)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE // Hide progress bar when done
                        updateLeaderboardUI(filteredScores) // Update UI with filtered scores
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        showErrorMessage("Error fetching scores: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ScoresFetchError", "Error fetching scores: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showErrorMessage("Network Error: ${e.message}")
                }
            }
        }
    }


    private fun updateLeaderboardUI(scores: List<Score>) {
        if (scores.isEmpty()) {
            showErrorMessage("No scores available for this difficulty.")
            return
        }

        // Sort scores in descending order
        val sortedScores = scores.sortedByDescending { it.score }

        sortedScores.forEach { score ->
            val userCard = layoutInflater.inflate(R.layout.score_card, leaderboardLayout, false)

            val usernameText: TextView = userCard.findViewById(R.id.username)
            val scoreText: TextView = userCard.findViewById(R.id.scoreTextView)

            usernameText.text = score.username
            scoreText.text = "${score.score}"

            leaderboardLayout.addView(userCard)
        }
    }

    private fun showErrorMessage(message: String) {
        // Use Snackbar for error messages
        Snackbar.make(leaderboardLayout, message, Snackbar.LENGTH_LONG).show()
    }
}
