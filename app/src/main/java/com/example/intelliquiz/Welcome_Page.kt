        package com.example.intelliquiz

        import android.os.Bundle
        import androidx.activity.enableEdgeToEdge
        import androidx.appcompat.app.AppCompatActivity
        import androidx.core.view.ViewCompat
        import androidx.core.view.WindowInsetsCompat

        class Welcome_Page : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                setContentView(R.layout.welcome_page)
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }
        }