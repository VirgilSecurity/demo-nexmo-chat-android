package com.virgilsecurity.chat.nexmo

import android.app.Application
import android.arch.persistence.room.Room
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.nexmo.sdk.conversation.client.ConversationClient
import com.virgilsecurity.chat.nexmo.db.AppDatabase
import com.virgilsecurity.chat.nexmo.http.NexmoServerApi

class NexmoApp : Application() {

    val TAG = "NexmoApp"

    lateinit var serverClient : NexmoServerApi
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var conversationClient: ConversationClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        db = Room.databaseBuilder(this, AppDatabase::class.java,
                AppDatabase.DATABASE_NAME).build()
//        db = Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java).build()
        serverClient = NexmoServerApi.create()
        conversationClient = ConversationClient.ConversationClientBuilder().context(this).build()
    }

    @JvmOverloads private fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
        Log.v(TAG, text)
        Handler(Looper.getMainLooper()).post {
            val toast = Toast.makeText(applicationContext, text, duration)
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }

    fun showInfo(text: String) {
        Log.e(TAG, text)
        showToast(text, Toast.LENGTH_SHORT)
    }

    fun showError(text: String) {
        Log.e(TAG, text)
        showToast(text, Toast.LENGTH_LONG)
    }

    companion object {
        lateinit var instance: NexmoApp
            private set
    }
}