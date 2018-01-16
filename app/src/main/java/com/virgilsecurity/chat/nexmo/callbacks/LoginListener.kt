package com.virgilsecurity.chat.nexmo.callbacks

interface LoginListener {
    fun onLoginStarted()

    fun onLoginFinished(userName: String, jwt: String)

    fun onLoginError(errorMessage: String)
}