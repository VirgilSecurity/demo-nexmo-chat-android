package com.virgilsecurity.chat.nexmo.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.virgilsecurity.chat.nexmo.db.model.Message
import com.virgilsecurity.chat.nexmo.db.model.User

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY date")
    fun getMessages() : List<Message>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY date")
    fun getMessages(conversationId: String) : List<Message>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND message_id = :message_id")
    fun getMessage(conversationId: String, message_id: String) : Message

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<Message>)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId AND message_id = :message_id")
    fun delete(conversationId: String, message_id: String)
}