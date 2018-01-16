package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class JWT (
        @SerializedName("jwt")
        val jwt: String
)