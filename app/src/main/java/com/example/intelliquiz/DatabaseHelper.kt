//package com.example.intelliquiz
//
//import android.content.ContentValues
//import android.content.Context
//import android.database.Cursor
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//
//class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
//
//        companion object {
//        private const val DATABASE_NAME = "IntelliQuiz.db"
//        private const val DATABASE_VERSION = 2
//
//        private const val TABLE_USERS = "Users"
//        private const val COLUMN_USER_ID = "user_id"
//        private const val COLUMN_USERNAME = "username"
//        private const val COLUMN_SCORE = "score"
//    }
//
//    private val CREATE_TABLE_USERS = (
//            "CREATE TABLE $TABLE_USERS (" +
//                    "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                    "$COLUMN_USERNAME TEXT UNIQUE, " +
//                    "$COLUMN_SCORE INTEGER)"
//            )
//
//    override fun onCreate(db: SQLiteDatabase) {
//        db.execSQL(CREATE_TABLE_USERS) // Create Users table
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
//        onCreate(db)
//    }
//
//    fun insertUser(username: String, score: Int): Long {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_USERNAME, username)
//            put(COLUMN_SCORE, score)
//        }
//        val newRowId = db.insert(TABLE_USERS, null, values)
//        db.close() // Close database connection
//        return newRowId // Return the new row ID
//    }
//
//
//    fun getAllUsersWithScores(): List<Pair<String, Int>> {
//        val userList = mutableListOf<Pair<String, Int>>()
//        val db = readableDatabase
//        val cursor: Cursor = db.query(TABLE_USERS, null, null, null, null, null, "$COLUMN_SCORE DESC")
//
//        if (cursor.moveToFirst()) {
//            do {
//                val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
//                val score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))
//                userList.add(Pair(username, score))
//            } while (cursor.moveToNext())
//        }
//        cursor.close()
//        db.close()
//        return userList
//    }
//    fun getUserByUsername(username: String): String? {
//        val db = readableDatabase
//        val cursor: Cursor = db.query(
//            TABLE_USERS, arrayOf(COLUMN_USERNAME),
//            "$COLUMN_USERNAME = ?", arrayOf(username),
//            null, null, null
//        )
//
//        var user: String? = null
//        if (cursor.moveToFirst()) {
//            user = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
//        }
//        cursor.close()
//        db.close()
//
//        return user
//    }
//
//
//    fun updateUserScore(username: String, newScore: Int): Int {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_SCORE, newScore)
//        }
//        val rowsAffected = db.update(TABLE_USERS, values, "$COLUMN_USERNAME = ?", arrayOf(username))
//        db.close()
//        return rowsAffected
//    }
//
//}
