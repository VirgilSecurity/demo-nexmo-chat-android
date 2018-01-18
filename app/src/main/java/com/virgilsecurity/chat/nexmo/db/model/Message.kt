package com.virgilsecurity.chat.nexmo.db.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "messages", primaryKeys = arrayOf("message_id", "conversation_id"))
class Message (
        @ColumnInfo(name = "message_id")
        var messageId: String = "",

        @ColumnInfo(name = "conversation_id")
        var conversationId: String = "",

        @ColumnInfo(name = "sender_id")
        var senderId: String = "",

        @ColumnInfo(name = "text")
        var text: String = "",

        @ColumnInfo(name = "date")
        var date: Date = Date()
)