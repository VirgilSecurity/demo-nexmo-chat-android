package com.virgilsecurity.chat.nexmo.db.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "messages")
class Message (
        @PrimaryKey(autoGenerate = false)
        @ColumnInfo(name = "message_id")
        var id: String = "",

        @ColumnInfo(name = "text")
        var text: String = ""
)