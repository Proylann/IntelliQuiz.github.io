    package com.example.intelliquiz

    import android.annotation.SuppressLint
    import android.os.Bundle
    import android.util.Log
    import android.widget.Button
    import android.widget.LinearLayout
    import android.widget.TextView
    import androidx.appcompat.app.AppCompatActivity
    import com.example.intelliquiz.api.RetrofitClient
    import com.example.intelliquiz.model.Score
    import com.google.ai.client.generativeai.GenerativeModel
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import kotlin.time.Duration.Companion.seconds
    import kotlin.time.ExperimentalTime

    class RateLimiter(private val maxRequests: Int, private val perDuration: kotlin.time.Duration) {
        private val requests = mutableListOf<Long>()

        suspend fun acquire() {
            while (true) {
                val now = System.currentTimeMillis()
                synchronized(requests) {
                    requests.removeAll { it < now - perDuration.inWholeMilliseconds }
                    if (requests.size < maxRequests) {
                        requests.add(now)
                        return
                    }
                }
                kotlinx.coroutines.delay(100) // Wait before checking again
            }
        }
    }

    class QuizSection : AppCompatActivity() {
        private lateinit var username: String
        private lateinit var questionTextView: TextView
        private lateinit var choicesTextView: TextView
        private lateinit var scoreTextView: TextView
        private lateinit var card: LinearLayout
        private lateinit var choiceButtonA: Button
        private lateinit var choiceButtonB: Button
        private lateinit var choiceButtonC: Button
        private lateinit var choiceButtonD: Button
        private var score: Int = 0
        private var currentQuestionIndex: Int = 0
        private var maxQuestions: Int = 0
        private val askedQuestions = mutableSetOf<String>()
        private val rateLimiter = RateLimiter(maxRequests = 10, perDuration = 60.seconds)
        private val retryDelay = 5000L // 5 seconds
        private var currentQuestion: QuizQuestion? = null // Holds the current question

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_quiz_section)

            username = intent.getStringExtra("USERNAME") ?: ""

            // Fetch the difficulty level
            val difficulty = intent.getStringExtra("DIFFICULTY") ?: "Easy"
            maxQuestions = when (difficulty) {
                "Easy" -> 10
                "Medium" -> 15
                "Difficult" -> 20
                else -> 10
            }

            // Initialize UI elements
            scoreTextView = findViewById(R.id.score)
            questionTextView = findViewById(R.id.question)
            choicesTextView = findViewById(R.id.choices)
            card = findViewById(R.id.card)

            // Initialize choice buttons
            choiceButtonA = findViewById(R.id.buttonA)
            choiceButtonB = findViewById(R.id.buttonB)
            choiceButtonC = findViewById(R.id.buttonC)
            choiceButtonD = findViewById(R.id.buttonD)

            // Start quiz by fetching the first question
            fetchNextQuestion()

            // Set up choice button actions
            choiceButtonA.setOnClickListener { checkAnswer('A') }
            choiceButtonB.setOnClickListener { checkAnswer('B') }
            choiceButtonC.setOnClickListener { checkAnswer('C') }
            choiceButtonD.setOnClickListener { checkAnswer('D') }
        }

        @OptIn(ExperimentalTime::class)
        @SuppressLint("SecretInSource")
        private fun fetchNextQuestion() {
            if (currentQuestionIndex < maxQuestions) {
                val subject = intent.getStringExtra("SUBJECT") ?: "General Knowledge"
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = "AIzaSyAwJzzrUWMIPvbWxD-EPeJYO6p6mIxSfB0"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    var attemptCount = 0
                    val maxAttempts = 3

                    while (attemptCount < maxAttempts) {
                        try {
                            rateLimiter.acquire() // Wait for rate limit slot

                            val difficulty = when (maxQuestions) {
                                10 -> "Easy"
                                15 -> "Medium"
                                20 -> "Difficult"
                                else -> "Easy"
                            }

                            val prompt = "Generate a $difficulty-level question about $subject with four options and the correct answer in the following format: 'Question: <question_text>; Option1: <option1>; Option2: <option2>; Option3: <option3>; Option4: <option4>; Answer: <correct_answer>'"
                            val response = generativeModel.generateContent(prompt)
                            val responseText = response.text.toString()

                            val question = parseQuestion(responseText)

                            if (question != null && !askedQuestions.contains(question.question)) {
                                askedQuestions.add(question.question)
                                currentQuestion = question // Set the current question

                                withContext(Dispatchers.Main) {
                                    displayQuestion(question) // Display the current question
                                }
                                break
                            }

                        } catch (e: Exception) {
                            when {
                                e is com.google.ai.client.generativeai.type.ServerException && e.message?.contains("overloaded") == true -> {
                                    attemptCount++
                                    if (attemptCount >= maxAttempts) {
                                        withContext(Dispatchers.Main) {
                                            questionTextView.text = "Content generation stopped due to safety concerns. Please try again."
                                            card.visibility = LinearLayout.GONE
                                        }
                                        Log.e("FetchQuestionsError", "Safety concern triggered: ${e.message}")
                                        break
                                    } else {
                                        kotlinx.coroutines.delay(retryDelay)
                                    }
                                }
                                e.message?.contains("429") == true -> {
                                    // Handle rate limit exceeded
                                    Log.w("RateLimit", "Rate limit exceeded. Waiting before retry.")
                                    kotlinx.coroutines.delay(retryDelay)
                                    continue
                                }
                                else -> {
                                    withContext(Dispatchers.Main) {
                                        questionTextView.text = "An unexpected error occurred. Please try again later."
                                    }
                                    Log.e("FetchQuestionsError", "Error: ${e.message}")
                                    break
                                }
                            }
                        }
                    }
                }
            } else {
                showFinalScore()
            }
        }

        private fun parseQuestion(response: String): QuizQuestion? {
            // Updated regex to ensure it captures all four options
            val questionPattern = """Question:\s*(.*?)\s*Option1:\s*(.*?)\s*Option2:\s*(.*?)\s*Option3:\s*(.*?)\s*Option4:\s*(.*?)\s*Answer:\s*(.*?)\s*$""".toRegex()
            val match = questionPattern.find(response)

            return if (match != null) {
                val questionText = match.groupValues[1].trim()
                val options = listOf(
                    match.groupValues[2].trim(), // Option1
                    match.groupValues[3].trim(), // Option2
                    match.groupValues[4].trim(), // Option3
                    match.groupValues[5].trim()  // Option4
                )
                val correctAnswer = match.groupValues[6].trim() // Correct answer

                // Ensure that there are exactly 4 options
                if (options.size == 4) {
                    QuizQuestion(questionText, correctAnswer, options)
                } else {
                    Log.e("ParseError", "Expected 4 options, but got ${options.size}: $options")
                    null
                }
            } else {
                Log.e("ParseError", "Failed to parse question from response: $response")
                null
            }
        }

        private fun displayQuestion(question: QuizQuestion) {
            questionTextView.text = question.question
            choicesTextView.text = question.answers.mapIndexed { index, answer ->
                "${('A' + index)}. $answer"
            }.joinToString("\n") // This should display A, B, C, D correctly.
            card.visibility = LinearLayout.VISIBLE
        }

        @SuppressLint("SetTextI18n")
        private fun checkAnswer(userAnswerChar: Char) {
            currentQuestion?.let { currentQuestion ->
                val correctAnswerText = currentQuestion.correctAnswer.trim() // Trim whitespace from the correct answer

                // Get the selected answer based on the button pressed
                val userAnswerText = when (userAnswerChar) {
                    'A' -> currentQuestion.answers[0].trim() // Trim whitespace from the user's answer
                    'B' -> currentQuestion.answers[1].trim()
                    'C' -> currentQuestion.answers[2].trim()
                    'D' -> currentQuestion.answers[3].trim()
                    else -> "" // Default case if something goes wrong
                }

                // Check if the user's answer matches the correct answer
                if (userAnswerText.equals(correctAnswerText, ignoreCase = true)) {
                    score++
                    scoreTextView.text = "Correct! Current score: $score"
                } else {
                    scoreTextView.text = "Incorrect! Correct answer was: $correctAnswerText"
                }

                // Proceed to fetch the next question after a brief delay or immediately based on your preference
                currentQuestionIndex++
                fetchNextQuestion()
            }
        }



        private fun showFinalScore() {
            questionTextView.visibility = TextView.GONE
            choicesTextView.visibility = TextView.GONE
            card.visibility = LinearLayout.GONE

            scoreTextView.text = "Final Score: $score/$maxQuestions"
            scoreTextView.visibility = TextView.VISIBLE

            if (username.isNotEmpty()) {
                submitScoreToApi(username, score) // Submit score to the API if the username is available
            }
        }

        private fun submitScoreToApi(username: String, score: Int) {
            val scoreEntry = Score(username = username, score = score)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.postScore(scoreEntry)
                    Log.d("ScoreSubmission", "Score submitted: ${response.message()}")
                } catch (e: Exception) {
                    Log.e("ScoreSubmissionError", "Error submitting score: ${e.message}")
                }
            }
        }
    }

    // Data class for QuizQuestion
    data class QuizQuestion(
        val question: String,
        val correctAnswer: String,
        val answers: List<String>
    )
