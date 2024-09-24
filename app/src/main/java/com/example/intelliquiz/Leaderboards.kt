package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Leaderboards : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var leaderboardLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaderboards)

        // Set up insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val homeButton: Button = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {

            val intent = Intent(this, Welcome_Page::class.java)
            startActivity(intent)
        }
        dbHelper = DatabaseHelper(this)
        leaderboardLayout = findViewById(R.id.leaderboards)
        displayUsers()

    }




    private fun displayUsers() {
        val users = dbHelper.getAllUsers() // Retrieve all usernames from the database
        val userEntriesCount = leaderboardLayout.childCount - 2 // Count of existing user entries
        if (userEntriesCount > 0) {
            leaderboardLayout.removeViewsInLayout(2, userEntriesCount)
        }

        // Add each user to the leaderboard layout
        for (username in users) {
            val userEntry = TextView(this).apply {
                text = username
                textSize = 18f // Set text size
                setPadding(16, 16, 16, 16) // Add padding
            }
            leaderboardLayout.addView(userEntry) // Add user entry to layout
        }
    }


}
