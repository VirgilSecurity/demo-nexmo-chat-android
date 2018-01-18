package com.virgilsecurity.chat.nexmo.utils

import com.nexmo.sdk.conversation.client.Conversation
import com.nexmo.sdk.conversation.client.Member
import com.virgilsecurity.chat.nexmo.NexmoApp

object NexmoUtils {
    val currentUser = NexmoApp.instance.conversationClient.user

    fun getConversationPartner(conversation: Conversation): Member? {
        conversation.members.forEach {
            if (!it.userId.equals(currentUser.userId)) {
                return it
            }
        }
        return null
    }

    fun getConversationName(conversation: Conversation): String {
        val partner = getConversationPartner(conversation)
        if (partner == null) {
            return conversation.displayName
        }
        return partner.name
    }
}