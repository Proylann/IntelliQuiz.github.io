        package com.example.intelliquiz

        import android.content.Intent
        import android.os.Bundle
        import android.widget.Button
        import androidx.activity.enableEdgeToEdge
        import androidx.appcompat.app.AppCompatActivity
        import androidx.core.view.ViewCompat
        import androidx.core.view.WindowInsetsCompat

        class Welcome_Page : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                setContentView(R.layout.welcome_page)
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }

                val enterButton: Button = findViewById(R.id.playBtn)
                val LeaderBtn: Button = findViewById(R.id.leaderBtn)

                enterButton.setOnClickListener {

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)  // This starts the SecondActivity
                }

                LeaderBtn.setOnClickListener {
                    val intent  = Intent(this, Leaderboards::class.java)
                    startActivity(intent)
                }



            }
        }