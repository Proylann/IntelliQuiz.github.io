package com.example.intelliquiz

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent;
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
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
        dbHelper = DatabaseHelper(this)
        userInp = findViewById(R.id.userInp)
        enterButton = findViewById(R.id.enterButton)

        enterButton.setOnClickListener {
            val username = userInp.text.toString().trim()
            if (username.isNotEmpty()) {
                dbHelper.insertUser(username)
                Toast.makeText(this, "Username Entered Succesfuly", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Subjects::class.java)
                startActivity(intent)


            } else {
                // Show error message
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()

            }
        }



    }
}