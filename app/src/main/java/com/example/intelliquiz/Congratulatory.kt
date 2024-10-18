package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Congratulatory : AppCompatActivity() {

    private lateinit var message: TextView
    private lateinit var scoreTv: TextView
    private lateinit var leaderboards: Button
    private lateinit var home:Button




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_congratulatory)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val score = intent.getIntExtra("FINAL_SCORE", 0)
        message = findViewById(R.id.message)
        scoreTv = findViewById(R.id.scoreTextView)
        scoreTv.text = "Your Final score: $score"
        home = findViewById(R.id.home2)
        leaderboards = findViewById(R.id.leaderboards)

        home.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        leaderboards.setOnClickListener(){
            val intent = Intent(this, Leaderboards::class.java)
            startActivity(intent)
        }



    }
}