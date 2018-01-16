package com.virgilsecurity.chat.nexmo.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.virgilsecurity.chat.nexmo.db.dao.UserDao
import com.virgilsecurity.chat.nexmo.db.model.Message
import com.virgilsecurity.chat.nexmo.db.model.User

@Database(entities = arrayOf(User::class, Message::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao() : UserDao

    companion object {
        const val DATABASE_NAME = "nexmo.db"
    }
}