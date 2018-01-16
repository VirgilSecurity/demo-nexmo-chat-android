package com.virgilsecurity.chat.nexmo.http.model

import com.google.gson.annotations.SerializedName

data class CSR (
        @SerializedName("csr")
        val csr: String
)