package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class NexmoUser (
        @SerializedName("id")
        val id: String,
        @SerializedName("href")
        val href: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("virgil_card")
        var virgilCard: String
)