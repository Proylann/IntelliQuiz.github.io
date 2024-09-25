package com.example.intelliquiz

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.intelliquiz.QuizSection
import com.example.intelliquiz.R

class Subjects: AppCompatActivity() {
    //passing the username para di mawala
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subjects)

        //retrieve usernamee gar
        username = intent.getStringExtra("USERNAME") ?: ""




        // Initialize buttons for each subject
        val mathButton: ImageButton = findViewById(R.id.mathButton)
        val scienceButton: ImageButton = findViewById(R.id.scienceButton)
        val geographyButton: ImageButton = findViewById(R.id.geographyButton)
        val historyButton: ImageButton = findViewById(R.id.historyButton)
        val englishButton: ImageButton = findViewById(R.id.englishBtn)
        val randomButton: ImageButton = findViewById(R.id.random)

        // Set click listeners for each subject button
        mathButton.setOnClickListener {
            startQuiz("Math")
        }

        scienceButton.setOnClickListener {
            startQuiz("Science")
        }

        geographyButton.setOnClickListener {
            startQuiz("Geography")
        }

        historyButton.setOnClickListener {
            startQuiz("Philippine History")
        }

        englishButton.setOnClickListener {
            startQuiz("English Subject")
        }

        randomButton.setOnClickListener {
            startQuiz("Random questions")
        }



    }

    private fun startQuiz(subject: String) {
        val intent = Intent(this, QuizSection::class.java)
        intent.putExtra("SUBJECT", subject)
        intent.putExtra("USERNAME", username) // Pass username to QuizSection
        startActivity(intent)
    }

}
