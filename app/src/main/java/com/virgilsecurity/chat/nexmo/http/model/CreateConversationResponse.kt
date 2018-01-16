package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class CreateConversationResponse (
        @SerializedName("id")
        val id: String,
        @SerializedName("href")
        val href: String
)