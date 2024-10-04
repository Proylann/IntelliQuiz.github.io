package com.example.intelliquiz

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.intelliquiz.api.RetrofitClient
import com.example.intelliquiz.model.Score
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var userInp: EditText
    private lateinit var enterButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userInp = findViewById(R.id.userInp)
        enterButton = findViewById(R.id.enterButton)

        enterButton.setOnClickListener {
            val username = userInp.text.toString().trim()
            if (username.isNotEmpty()) {
                // Fetch existing scores from the API
                fetchScores(username) // Fetch scores when user enters their name
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchScores(username: String) {
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
                            // If username does not exist, proceed to the Subjects activity
                            val intent = Intent(this@MainActivity, Subjects::class.java)
                            intent.putExtra("USERNAME", username)
                            startActivity(intent)
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
}