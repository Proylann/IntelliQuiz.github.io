package com.example.intelliquiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class QuizSection : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private var username: String = ""

    private lateinit var questionTextView: TextView
    private lateinit var choicesTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var card:LinearLayout
    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    private lateinit var exitButton: Button
    private var score: Int = 0
    private var currentQuestionIndex: Int = 0
    private val maxQuestions = 5
    private val questions = mutableListOf<QuizQuestion>()
    private val askedQuestions = mutableSetOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_section)
        enableEdgeToEdge()

        // Db Shits
        dbHelper = DatabaseHelper(this)
        username = intent.getStringExtra("USERNAME") ?: username

        // Initialize UI elements
        scoreTextView = findViewById(R.id.score)
        questionTextView = findViewById(R.id.question)
        choicesTextView = findViewById(R.id.choices)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        card = findViewById(R.id.card)
        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)
        exitButton = findViewById(R.id.exitButton)
        nextButton.visibility = View.GONE
        backButton.visibility = View.GONE


        // Start quiz by fetching the first question
        fetchNextQuestion()

        // Set up submit button action
        submitButton.setOnClickListener {
            val userAnswer = answerInput.text.toString().trim()
            checkAnswer(userAnswer)
            // Show next/back buttons after answering
            nextButton.visibility = View.VISIBLE
            backButton.visibility = if (currentQuestionIndex > 1) View.VISIBLE else View.GONE

        }
        // Next button click listener
        nextButton.setOnClickListener {
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                displayQuestion(questions[currentQuestionIndex])
            } else {
                showFinalScore()
            }
            // Update button visibility
            backButton.visibility = if (currentQuestionIndex > 1) View.VISIBLE else View.GONE
            nextButton.visibility = if (currentQuestionIndex < questions.size - 1) View.VISIBLE else View.GONE
        }
        // Back button click listener
        backButton.setOnClickListener {
            currentQuestionIndex--
            displayQuestion(questions[currentQuestionIndex])
            // Update button visibility
            backButton.visibility = if (currentQuestionIndex > 1) View.VISIBLE else View.GONE
            nextButton.visibility = View.VISIBLE
        }
        // Exit button click listener
        exitButton.setOnClickListener {
            val intent = Intent(this, Welcome_Page::class.java)
            startActivity(intent)
            finish() // Optional: Finish QuizSection activity to prevent going back with back button
        }
    }
    private fun fetchNextQuestion() {
        if (currentQuestionIndex < maxQuestions) {
            val subject = intent.getStringExtra("SUBJECT") ?: "General Knowledge"
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "AIzaSyCXmnTNB43_fd0E8CmhINiYDPftjnzCTjU" // Replace with your actual API key
            )

            CoroutineScope(Dispatchers.IO).launch {
                var question: QuizQuestion? = null
                var responseText: String
                var attemptCount = 0
                val maxAttempts = 3

                // Keep fetching until a new question is found
                do {
                    try {
                        val prompt = "Generate one unique general knowledge question about $subject with four options and the correct answer in the following format: 'Question: <question_text>; Option1: <option1>; Option2: <option2>; Option3: <option3>; Option4: <option4>; Answer: <correct_answer>'"

                        // Call the API to generate content
                        val response = generativeModel.generateContent(prompt)
                        responseText = response.text.toString()

                        // Parse the AI response
                        question = parseQuestion(responseText)

                    } catch (e: com.google.ai.client.generativeai.type.ServerException) {
                        if (e.message?.contains("overloaded") == true && attemptCount < maxAttempts) {
                            attemptCount++
                            kotlinx.coroutines.delay(2000) // Delay before retrying
                        } else {
                            withContext(Dispatchers.Main) {
                                questionTextView.text = "Service unavailable. Please try again later."
                                answerInput.isEnabled = false
                                submitButton.isEnabled = false
                            }
                            return@launch
                        }
                    }
                } while (question == null || askedQuestions.contains(question?.question ?: ""))

                askedQuestions.add(question!!.question)
                questions.add(question)

                withContext(Dispatchers.Main) {
                    displayQuestion(question)
                }
            }
        } else {
            showFinalScore()
        }
    }

    private fun parseQuestion(response: String): QuizQuestion? {
        val questionPattern = """Question:\s*(.*?)\s*Option1:\s*(.*?)\s*Option2:\s*(.*?)\s*Option3:\s*(.*?)\s*Option4:\s*(.*?)\s*Answer:\s*(.*?)\s*$""".toRegex()
        val match = questionPattern.find(response)

        return if (match != null) {
            val (questionText, option1, option2, option3, option4, correctAnswer) = match.destructured
            val answers = listOf(option1.trim(), option2.trim(), option3.trim(), option4.trim())
            QuizQuestion(questionText.trim(), correctAnswer.trim(), answers)
        } else {
            Log.e("ParseError", "Failed to parse question from response: $response") // Log parsing error
            null
        }
    }

    private fun displayQuestion(question: QuizQuestion) {
        questionTextView.text = question.question
        choicesTextView.text = question.answers.mapIndexed { index, answer -> "${('A' + index)}. $answer" }.joinToString("\n")
        answerInput.text.clear()
    }

    private fun checkAnswer(userAnswer: String) {
        val currentQuestion = questions[currentQuestionIndex]
        val answerMap = mapOf(
            'A' to currentQuestion.answers[0],
            'B' to currentQuestion.answers[1],
            'C' to currentQuestion.answers[2],
            'D' to currentQuestion.answers[3]
        )

        // Clean and validate user input
        val userAnswerChar: Char? = userAnswer.uppercase().trim().firstOrNull()

        // Check if the user input is one of the valid options
        if (userAnswerChar !in answerMap.keys) {
            Log.d("InvalidAnswer", "User answer is not valid: $userAnswer")
            return // Exit if the answer is invalid
        }

        val userAnswerText = answerMap[userAnswerChar] // Get the mapped answer
        val correctAnswerText = currentQuestion.correctAnswer // Keep the correct answer as is

        Log.d("UserAnswer", userAnswerText ?: "None")
        Log.d("CorrectAnswer", correctAnswerText ?: "None")

        // Strip prefixes and unwanted characters from answers for comparison
        val strippedUserAnswer = userAnswerText?.replace(Regex("Option\\s?\\d+:?|Option\\s?\\d+\\s?-?\\s*|-"), "")?.trim()?.replace("\\s+".toRegex(), "") ?: ""
        val strippedCorrectAnswer = correctAnswerText?.replace(Regex("Option\\s?\\d+:?|Option\\s?\\d+\\s?-?\\s*|-"), "")?.trim()?.replace("\\s+".toRegex(), "") ?: ""

        // Debug comparison
        Log.d("ComparingAnswers", "User Answer: $strippedUserAnswer vs Correct Answer: $strippedCorrectAnswer")

        // Check if the user's answer matches the correct answer (case insensitive)
        if (strippedUserAnswer.equals(strippedCorrectAnswer, ignoreCase = true)) {
            score++
        }

        // Move to the next question regardless of whether the answer was correct
        currentQuestionIndex++
        fetchNextQuestion()
    }





    private fun showFinalScore() {
        // Hide other UI elements and display score
        questionTextView.visibility = TextView.GONE
        choicesTextView.visibility = TextView.GONE
        answerInput.visibility = EditText.GONE
        submitButton.visibility = Button.GONE
        card.visibility = LinearLayout.GONE

        scoreTextView.text = "Final Score: $score/$maxQuestions"
        scoreTextView.visibility = TextView.VISIBLE

        if (username.isNotEmpty()) { // Check if username is not empty
            // Check if the user already exists in the database
            val existingUser = dbHelper.getUserByUsername(username)
            if (existingUser != null) {
                // If the user exists, update their score
                dbHelper.updateUserScore(username, score)
            } else {
                // If the user doesn't exist, insert the user with their score
                dbHelper.insertUser(username, score)
            }
        } else {
            Log.e("UsernameError", "Username is empty when trying to save score!")
        }
    }



    data class QuizQuestion(
        val question: String,
        val correctAnswer: String,
        val answers: List<String>
    )
}





