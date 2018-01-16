package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class CreateConversationRequest(
        @SerializedName("display_name")
        val displayName: String
)