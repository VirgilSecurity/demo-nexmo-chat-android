package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class AddUserRequest (
        @SerializedName("conversation_id")
        val conversationId: String,
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("action")
        val action: String
)