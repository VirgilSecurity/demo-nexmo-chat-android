package com.virgilsecurity.chat.nexmo.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import com.virgilsecurity.chat.nexmo.db.dao.MessageDao
import com.virgilsecurity.chat.nexmo.db.dao.UserDao
import com.virgilsecurity.chat.nexmo.db.model.Message
import com.virgilsecurity.chat.nexmo.db.model.User

@Database(entities = arrayOf(User::class, Message::class), version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao() : UserDao
    abstract fun messageDao() : MessageDao

    companion object {
        const val DATABASE_NAME = "nexmo.db"
    }

}