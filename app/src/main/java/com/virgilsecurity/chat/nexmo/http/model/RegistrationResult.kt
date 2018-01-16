package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class RegistrationResult (
        @SerializedName("user")
        val user: NexmoUser,
        @SerializedName("jwt")
        val jwt: String
)