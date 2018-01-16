package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class AddUserResponse (
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("user_name")
        val userName: String,
        @SerializedName("state")
        val state: String
)