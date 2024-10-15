    package com.example.intelliquiz

    import android.annotation.SuppressLint
    import android.os.Bundle
    import android.os.CountDownTimer
    import android.util.Log
    import android.view.View
    import android.widget.Button
    import android.widget.LinearLayout
    import android.widget.ProgressBar
    import android.widget.TextView
    import androidx.appcompat.app.AppCompatActivity
    import com.example.intelliquiz.api.RetrofitClient
    import com.example.intelliquiz.model.Score
    import com.google.ai.client.generativeai.GenerativeModel
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import kotlin.time.Duration.Companion.seconds
    import kotlin.time.ExperimentalTime

    class RateLimiter(private val maxRequests: Int, private val perDuration: kotlin.time.Duration) {
        private val requests = mutableListOf<Long>()

        suspend fun acquire() {
            var needToDelay = false
            val now = System.currentTimeMillis()

            synchronized(requests) {
                requests.removeAll { it < now - perDuration.inWholeMilliseconds }
                if (requests.size >= maxRequests) {
                    needToDelay = true
                } else {
                    requests.add(now)
                }
            }

            // Perform the delay outside of the synchronized block
            if (needToDelay) {
                delay(1000)
                synchronized(requests) {
                    requests.add(now)
                }
            }
        }
    }

    class QuizSection : AppCompatActivity() {
        private lateinit var username: String
        private lateinit var questionTextView: TextView
        private lateinit var choicesTextView: TextView
        private lateinit var scoreTextView: TextView
        private lateinit var timerTextView: TextView
        private lateinit var card: LinearLayout
        private lateinit var choiceButtonA: Button
        private lateinit var choiceButtonB: Button
        private lateinit var choiceButtonC: Button
        private lateinit var choiceButtonD: Button
        private var score: Int = 0
        private var currentQuestionIndex: Int = 0
        private var maxQuestions: Int = 0
        private val askedQuestions = mutableSetOf<String>()
        private val rateLimiter = RateLimiter(maxRequests = 20, perDuration = 60.seconds)
        private var currentQuestion: QuizQuestion? = null // Holds the current question
        private var Timer: CountDownTimer? = null
        private val totalTimeMedium = 10 * 60 * 1000L // 10 minutes in milliseconds
        private val totalTimeHard = 5 * 60 * 1000L // 5 minutes in milliseconds

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_quiz_section)
            timerTextView = findViewById(R.id.timer)

            username = intent.getStringExtra("USERNAME") ?: ""

            // Fetch the difficulty level
            val difficulty = intent.getStringExtra("DIFFICULTY") ?: "Easy"
            setupTimer(difficulty)
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
           // Timer TextView


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

        private fun setupTimer(difficulty: String) {
            when (difficulty) {
                "Medium" -> startTimer(totalTimeMedium)
                "Difficult" -> startTimer(totalTimeHard)
                else -> timerTextView.visibility = View.GONE
            }
        }

        private fun startTimer(totalTimeInMillis: Long) {
            timerTextView.visibility = View.VISIBLE
            Timer = object : CountDownTimer(totalTimeInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = millisUntilFinished / 60000
                    val seconds = (millisUntilFinished % 60000) / 1000
                    timerTextView.text = String.format("%d:%02d", minutes, seconds)
                }

                override fun onFinish() {
                    timerTextView.text = "0:00"
                    showFinalScore()
                }
            }.start()
        }

        @OptIn(ExperimentalTime::class)
        private fun fetchNextQuestion() {
            if (currentQuestionIndex < maxQuestions) {
                findViewById<ProgressBar>(R.id.loadingIndicator).visibility = View.VISIBLE
                card.visibility = LinearLayout.GONE
                val subject = intent.getStringExtra("SUBJECT") ?: "General Knowledge"
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = "AIzaSyAwJzzrUWMIPvbWxD-EPeJYO6p6mIxSfB0"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    var attemptCount = 0
                    val maxAttempts = 5 // Increased from 3 to 5

                    while (attemptCount < maxAttempts) {
                        try {
                            rateLimiter.acquire()

                            val difficulty = when (maxQuestions) {
                                10 -> "Easy"
                                15 -> "Medium"
                                20 -> "Difficult"
                                else -> "Easy"
                            }

                            val prompt = "Generate a unique $difficulty-level question about $subject with four options and the correct answer in the following format: 'Question: <question_text>; Option1: <option1>; Option2: <option2>; Option3: <option3>; Option4: <option4>; Answer: <correct_answer>'"
                            val response = generativeModel.generateContent(prompt)
                            val responseText = response.text.toString()

                            val question = parseQuestion(responseText)

                            if (question != null && !askedQuestions.contains(question.question)) {
                                askedQuestions.add(question.question)
                                currentQuestion = question

                                withContext(Dispatchers.Main) {
                                    displayQuestion(question)
                                }
                                return@launch // Exit the coroutine after successfully displaying a question
                            } else {
                                attemptCount++
                                delay(1000) // Wait 1 second before trying again
                            }
                        } catch (e: Exception) {
                            // ... (keep the existing error handling)
                        }
                    }

                    // If we've exhausted all attempts, show an error message
                    withContext(Dispatchers.Main) {
                        questionTextView.text = "Unable to generate a unique question. Please try again."
                        card.visibility = LinearLayout.GONE
                    }
                }
            } else {
                showFinalScore()
            }
        }

        private fun parseQuestion(response: String): QuizQuestion? {
            // Updated regex to capture the full answer text
            val questionPattern = """Question:\s*(.*?)\s*Option(?:1|A):\s*(.*?)\s*Option(?:2|B):\s*(.*?)\s*Option(?:3|C):\s*(.*?)\s*Option(?:4|D):\s*(.*?)\s*Answer:\s*(.+)""".toRegex(RegexOption.DOT_MATCHES_ALL)

            val match = questionPattern.find(response)

            return if (match != null) {
                val questionText = match.groupValues[1].trim()
                val options = listOf(
                    match.groupValues[2].trim(),
                    match.groupValues[3].trim(),
                    match.groupValues[4].trim(),
                    match.groupValues[5].trim()
                )
                val answerText = match.groupValues[6].trim()

                // Find the closest matching option for the answer
                val correctAnswerIndex = options.indexOfFirst { it.equals(answerText, ignoreCase = true) }
                val correctAnswer = if (correctAnswerIndex != -1) {
                    options[correctAnswerIndex]
                } else {
                    // If no exact match, find the most similar option
                    options.minByOrNull { levenshteinDistance(it.lowercase(), answerText.lowercase()) } ?: options[0]
                }

                // Log the parsed question details
                Log.d("ParseQuestion", "Question: $questionText")
                Log.d("ParseQuestion", "Options: $options")
                Log.d("ParseQuestion", "Answer Text: $answerText")
                Log.d("ParseQuestion", "Correct Answer: $correctAnswer")

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

   // function to calculate string similarity
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val m = s1.length
            val n = s2.length
            val dp = Array(m + 1) { IntArray(n + 1) }
            for (i in 0..m) dp[i][0] = i
            for (j in 0..n) dp[0][j] = j
            for (i in 1..m) {
                for (j in 1..n) {
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                    )
                }
            }
            return dp[m][n]
        }
        private fun displayQuestion(question: QuizQuestion) {
            questionTextView.text = question.question
            choicesTextView.text = question.answers.mapIndexed { index, answer ->
                "${('A' + index)}. $answer"
            }.joinToString("\n")
            card.visibility = LinearLayout.VISIBLE
            findViewById<ProgressBar>(R.id.loadingIndicator).visibility = View.GONE
        }

        private fun checkAnswer(userAnswerChar: Char) {
            currentQuestion?.let { currentQuestion ->
                val userAnswerIndex = when (userAnswerChar) {
                    'A' -> 0
                    'B' -> 1
                    'C' -> 2
                    'D' -> 3
                    else -> -1
                }

                if (userAnswerIndex != -1) {
                    val userAnswer = currentQuestion.answers[userAnswerIndex]
                    Log.d("AnswerCheck", "User selected: $userAnswerChar. Answer: $userAnswer")
                    Log.d("AnswerCheck", "Correct answer: ${currentQuestion.correctAnswer}")

                    if (userAnswer == currentQuestion.correctAnswer) {
                        score++
                        scoreTextView.text = "Correct! Current score: $score"
                        Log.d("AnswerCheck", "Result: Correct")
                    } else {
                        scoreTextView.text = "Incorrect! Correct answer was: ${currentQuestion.correctAnswer}"
                        Log.d("AnswerCheck", "Result: Incorrect")
                    }
                } else {
                    scoreTextView.text = "Invalid selection. Please choose A, B, C, or D."
                    Log.e("AnswerCheck", "Invalid selection: $userAnswerChar")
                }

                currentQuestionIndex++
            }

            fetchNextQuestion()
        }

        private fun showFinalScore() {
            Timer?.cancel() // Cancel the timer if it's running

            questionTextView.visibility = View.GONE
            choicesTextView.visibility = View.GONE
            card.visibility = View.GONE
            timerTextView.visibility = View.GONE

            scoreTextView.text = "Time's up! Final Score: $score/$currentQuestionIndex"
            scoreTextView.visibility = View.VISIBLE

            if (username.isNotEmpty()) {
                submitScoreToApi(username, score)
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

        override fun onDestroy() {
            super.onDestroy()
            Timer?.cancel() // Make sure to cancel the timer when the activity is destroyed
        }
    }



    // Data class for QuizQuestion
    data class QuizQuestion(
        val question: String,
        val correctAnswer: String,
        val answers: List<String>
    )
