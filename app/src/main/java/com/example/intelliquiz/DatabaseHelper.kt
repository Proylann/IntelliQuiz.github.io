package com.example.intelliquiz

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Database constants
    companion object {
        private const val DATABASE_NAME = "IntelliQuiz.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "Users"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USERNAME = "username"
    }

    // SQL statement to create the Users table
    private val CREATE_TABLE_USERS = (
            "CREATE TABLE $TABLE_USERS (" +
                    "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_USERNAME TEXT UNIQUE)"
            )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_USERS) // Create Users table
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db) // Create new table
    }

    // Method to insert a new user
    fun insertUser(username: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
        }
        val newRowId = db.insert(TABLE_USERS, null, values)
        db.close() // Close database connection
        return newRowId // Return the new row ID
    }

    // Method to retrieve all users
    fun getAllUsers(): List<String> {
        val userList = mutableListOf<String>()
        val db = readableDatabase
        val cursor: Cursor = db.query(TABLE_USERS, null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                userList.add(username)
            } while (cursor.moveToNext())
        }
        cursor.close() // Close cursor
        db.close() // Close database connection
        return userList // Return the list of usernames
    }
}
