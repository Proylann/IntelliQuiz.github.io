package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.intelliquiz.api.RetrofitClient
import com.example.intelliquiz.model.Score
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var userInp: EditText
    private lateinit var enterButton: Button
    private lateinit var difficultySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Adjust layout for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        userInp = findViewById(R.id.userInp)
        enterButton = findViewById(R.id.enterButton)
        difficultySpinner = findViewById(R.id.difficultySpinner)

        // Set up Spinner with difficulty levels
        val difficulties = arrayOf("Easy", "Medium", "Difficult")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter

        // Handle Enter button click
        enterButton.setOnClickListener {
            val username = userInp.text.toString().trim()
            val selectedDifficulty = difficultySpinner.selectedItem.toString()
            if (username.isNotEmpty()) {
                // Fetch existing scores from the API
                fetchScores(username, selectedDifficulty)
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchScores(username: String, difficulty: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<List<Score>> = RetrofitClient.apiService.getScores()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val scores = response.body() ?: emptyList()
                        Log.d("MainActivity", "Fetched ${scores.size} scores")

                        // Check if username already exists in the fetched scores
                        val usernameExists = scores.any { it.username == username }

                        if (usernameExists) {
                            // If username is found, display a message and don't proceed
                            Toast.makeText(
                                this@MainActivity,
                                "Username already exists",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Post new score with difficulty to the API
                            saveNewScore(username, difficulty)
                        }

                    } else {
                        Log.e("MainActivity", "Failed to fetch scores. Code: ${response.code()}")
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to fetch scores: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Network Error: ${t.message}")
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun saveNewScore(username: String, difficulty: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newScore = Score(username = username, score = 0, difficulty = difficulty)

                val response = RetrofitClient.apiService.postScore(newScore)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Proceed to Subjects activity after saving the score
                        val intent = Intent(this@MainActivity, Subjects::class.java)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("DIFFICULTY", difficulty)
                        startActivity(intent)
                    } else {
                        Log.e("MainActivity", "Failed to save score. Code: ${response.code()}")
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save score: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Network Error: ${t.message}")
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    }