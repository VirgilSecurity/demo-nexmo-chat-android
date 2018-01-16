package com.virgilsecurity.chat.nexmo.callbacks

interface RegistrationListener {
    fun onRegistrationStarted()

    fun onRegistrationFinished(userName: String, jwt: String)

    fun onRegistrationError(errorMessage: String)
}