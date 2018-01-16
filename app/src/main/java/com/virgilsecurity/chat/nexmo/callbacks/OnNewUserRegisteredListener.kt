package com.virgilsecurity.chat.nexmo.callbacks

interface OnNewUserRegisteredListener {
    fun onNewUserRegistered(userName: String, jwt: String)
}