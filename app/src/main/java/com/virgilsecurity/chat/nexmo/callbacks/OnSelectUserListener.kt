package com.virgilsecurity.chat.nexmo.callbacks

interface OnSelectUserListener : OnCreateNewUserListener {
    fun onSelectUser(userName: String, jwt: String)
}