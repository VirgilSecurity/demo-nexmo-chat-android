package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class APIError (
        @SerializedName("status")
        val status: Int,
        @SerializedName("error_code")
        val errorCode: Int,
        @SerializedName("message")
        val message: String
)