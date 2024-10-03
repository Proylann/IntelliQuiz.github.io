package com.example.intelliquiz

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


import com.example.intelliquiz.api.RetrofitClient
import com.example.intelliquiz.ApiService
import com.example.intelliquiz.model.Score
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizSection : AppCompatActivity() {
    private var username: String = ""

    private lateinit var questionTextView: TextView
    private lateinit var choicesTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var card: LinearLayout
    private var score: Int = 0
    private var currentQuestionIndex: Int = 0
    private val maxQuestions = 5
    private val questions = mutableListOf<QuizQuestion>()
    private val askedQuestions = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_section)
        enableEdgeToEdge()

        username = intent.getStringExtra("USERNAME") ?: ""

        // Initialize UI elements
        scoreTextView = findViewById(R.id.score)
        questionTextView = findViewById(R.id.question)
        choicesTextView = findViewById(R.id.choices)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        card = findViewById(R.id.card)

        // Start quiz by fetching the first question
        fetchNextQuestion()

        // Set up submit button action
        submitButton.setOnClickListener {
            val userAnswer = answerInput.text.toString().trim()
            checkAnswer(userAnswer)
        }
    }

    private fun fetchNextQuestion() {
        if (currentQuestionIndex < maxQuestions) {
            val subject = intent.getStringExtra("SUBJECT") ?: "General Knowledge"
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "AIzaSyCXmnTNB43_fd0E8CmhINiYDPftjnzCTjU"
            )

            CoroutineScope(Dispatchers.IO).launch {
                var question: QuizQuestion? = null
                var attemptCount = 0
                val maxAttempts = 3

                // Keep fetching until a new question is found
                do {
                    try {
                        val prompt = "Generate one unique general knowledge question about $subject with four options and the correct answer in the following format: 'Question: <question_text>; Option1: <option1>; Option2: <option2>; Option3: <option3>; Option4: <option4>; Answer: <correct_answer>'"

                        // Call the API to generate content
                        val response = generativeModel.generateContent(prompt)
                        val responseText = response.text.toString()

                        // Parse the AI response
                        question = parseQuestion(responseText)

                    } catch (e: com.google.ai.client.generativeai.type.ServerException) {
                        if (e.message?.contains("overloaded") == true && attemptCount < maxAttempts) {
                            attemptCount++
                            kotlinx.coroutines.delay(2000)
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

        val userAnswerChar: Char? = userAnswer.uppercase().trim().firstOrNull()
        if (userAnswerChar !in answerMap.keys) {
            Log.d("InvalidAnswer", "User answer is not valid: $userAnswer")
            return
        }

        val userAnswerText = answerMap[userAnswerChar]
        val correctAnswerText = currentQuestion.correctAnswer

        val strippedUserAnswer = userAnswerText?.replace(Regex("Option\\s?\\d+:?|Option\\s?\\d+\\s?-?\\s*|-"), "")?.trim()?.replace("\\s+".toRegex(), "") ?: ""
        val strippedCorrectAnswer = correctAnswerText.replace(Regex("Option\\s?\\d+:?|Option\\s?\\d+\\s?-?\\s*|-"), "").trim().replace("\\s+".toRegex(), "")

        if (strippedUserAnswer.equals(strippedCorrectAnswer, ignoreCase = true)) {
            score++
        }
        currentQuestionIndex++
        fetchNextQuestion()
    }

    private fun showFinalScore() {
        questionTextView.visibility = TextView.GONE
        choicesTextView.visibility = TextView.GONE
        answerInput.visibility = EditText.GONE
        submitButton.visibility = Button.GONE
        card.visibility = LinearLayout.GONE

        scoreTextView.text = "Final Score: $score/$maxQuestions"
        scoreTextView.visibility = TextView.VISIBLE

        // Check if username is not empty and score is a valid integer
        if (username.isNotEmpty()) {
            submitScoreToApi(username, score)
        } else {
            Log.e("UsernameError", "Username is empty when trying to save score!")
        }
    }

    private fun submitScoreToApi(username: String, score: Int) {
        val apiService = RetrofitClient.apiService

        CoroutineScope(Dispatchers.IO).launch {
            // Create the Score object
            val scoreObject = Score(username = username, score = score)

            try {
                // Submit the score object to the API
                val response = apiService.postScore(scoreObject)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("ScoreSubmission", "Score submitted successfully: $score")
                    } else {
                        Log.e("ScoreSubmissionError", "Failed to submit score: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ScoreSubmissionError", "Exception occurred while submitting score: ${e.message}")
                }
            }
        }
    }



    data class QuizQuestion(
        val question: String,
        val correctAnswer: String,
        val answers: List<String>
    )
}
