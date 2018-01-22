package com.virgilsecurity.chat.nexmo

import android.app.ProgressDialog
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.nexmo.sdk.conversation.client.*
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import com.nexmo.sdk.conversation.core.SubscriptionList
import com.virgilsecurity.chat.nexmo.adapters.MessagesRecyclerViewAdapter
import com.virgilsecurity.chat.nexmo.db.model.Message
import com.virgilsecurity.chat.nexmo.utils.NexmoUtils
import com.virgilsecurity.chat.nexmo.virgil.VirgilFacade
import com.virgilsecurity.sdk.client.model.CardModel
import com.virgilsecurity.sdk.securechat.exceptions.SecureChatException
import kotlinx.android.synthetic.main.activity_conversation.*

class ConversationActivity : AppCompatActivity() {
    val TAG = "NexmoConversation"

    private val conversationClient: ConversationClient = NexmoApp.instance.conversationClient
    private val messageDao = NexmoApp.instance.db.messageDao()

    private var progressDialog: ProgressDialog? = null
    private var toolbar: Toolbar? = null
    private val mMessages = ArrayList<Message>()
    private var mAdapter: MessagesRecyclerViewAdapter? = null
    val mHandler = Handler()
    var mConversation: Conversation? = null
    var mMemberCard: CardModel? = null
    var mSubscriptions: SubscriptionList = SubscriptionList()
    var conversationAttached = false
    private var sendingMessage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        if (intent.hasExtra("title")) {
            title = intent.getStringExtra("title")
        }

        ui()
        loadConversation()
    }

    override fun onResume() {
        super.onResume()
        attachConversation()
    }

    override fun onPause() {
        super.onPause()
        detachConversation()
    }

    private fun ui() {
        // Action bar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar);

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Send button
        sendButton.setOnClickListener {
            sendMessage()
        }
        // Message text editor
        message.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (EditorInfo.IME_NULL == actionId
                    && keyEvent.action == KeyEvent.ACTION_DOWN) {
                sendMessage()
                true
            }
            false
        }

        // Messages list
        Log.v(TAG, "My user id: ${conversationClient.user.userId}")
        mAdapter = MessagesRecyclerViewAdapter(conversationClient.user.userId, mMessages)
        messages.layoutManager = LinearLayoutManager(this)
        messages.adapter = mAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // handle arrow click here
        if (item?.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadConversation() {
        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading_conversation), true)
        if (intent.hasExtra("conversation_id")) {
            // Load conversation by ID
            val conversationId = intent.getStringExtra("conversation_id")
            Log.d(TAG, "Load conversation ${conversationId}")
            mConversation = conversationClient.getConversation(conversationId)
            initConversation()
        } else if (intent.hasExtra("user_id")) {
            // Load conversation with user
            val userId = intent.getStringExtra("user_id")
            val userName = intent.getStringExtra("user_name")
            loadConversation(userId, userName)
        }
    }

    private fun loadConversation(userId: String, userName: String) {
        Log.d(TAG, "Find conversation for user ${userName} : ${userId}")
        for (conversation in conversationClient.conversationList) {
            conversation.members.forEach {
                if (it.userId.equals(userId)) {
                    Log.d(TAG, "Use coversation ${conversation.conversationId} for user ${userName}")
                    mConversation = conversation
                    initConversation()
                    return
                }
            }
        }
        Log.d(TAG, "No conversation for user ${userName}. Create a new one")
        createConversation(userName)
    }

    private fun initConversation() {
        joinConversationIfNotJoined()
        attachConversation()
        AsyncTask.execute {
            initMembers()
            loadMessages()
        }
    }


    private fun createConversation(userName: String) {
        conversationClient.newConversation(true, userName, object : RequestHandler<Conversation> {
            override fun onError(apiError: NexmoAPIError?) {
                closeWithError("Conversation is not created", apiError)
            }

            override fun onSuccess(result: Conversation?) {
                Log.d(TAG, "Created conversation ${result?.conversationId} for user ${userName}")
                mConversation = result
                mConversation?.invite(userName, object : RequestHandler<Member> {
                    override fun onError(apiError: NexmoAPIError?) {
                        closeWithError("Can't invite user ${userName} into conversation", apiError)
                    }

                    override fun onSuccess(result: Member?) {
                        Log.d(TAG, "User ${result?.name} invited into conversation")
                        mMemberCard = VirgilFacade.instance.virgilApi.cards.find(result?.name).firstOrNull()?.model
                        initConversation()
                    }
                })
            }
        })
    }

    private fun joinConversationIfNotJoined() {
        if (mConversation?.getMember(mConversation?.memberId)?.joinedAt == null) {
            Log.d(TAG, "Join user to conversation ${mConversation?.conversationId}")
            mConversation?.join(object : RequestHandler<Member> {
                override fun onError(apiError: NexmoAPIError?) {
                    Log.d(TAG, "User NOT joined to conversation ${mConversation?.conversationId}")
                }

                override fun onSuccess(result: Member?) {
                    Log.d(TAG, "User joined to conversation ${mConversation?.conversationId}")
                }

            })
        }
    }

    private fun attachConversation() {
        Log.d(TAG, "Attach conversation ${mConversation?.conversationId}")
        mConversation?.let {
            if (!conversationAttached) {
                conversationAttached = true

                mConversation?.messageEvent()?.add {
                    Log.d(TAG, "Received message: ${it.id}")
                    if (it is Text) {
                        AsyncTask.execute {
                            receiveMessage(it)
                        }
                    }
                }?.addTo(mSubscriptions)
            }
            progressDialog?.dismiss()
        }
    }

    private fun detachConversation() {
        Log.d(TAG, "Detach conversation")
        mSubscriptions.unsubscribeAll()
        conversationAttached = false
    }

    private fun sendMessage() {
        val text = message.text.toString()
        if (sendingMessage || text.isBlank()) {
            return
        }
        Log.d(TAG, "Send message: ${text}")
        mMemberCard?.let {
            AsyncTask.execute {
                try {
                    val encryptedMessage = VirgilFacade.instance.encrypt(text, mMemberCard!!)
                    sendingMessage = true
                    mConversation?.sendText(encryptedMessage, object : RequestHandler<Event> {
                        override fun onSuccess(result: Event?) {
                            Log.v(TAG, "Message '${text}' sent as encrypted: ${encryptedMessage}")
                            // Save message in database
                            val hash = encryptedMessage.hashCode().toString()
                            val msg = Message(hash, mConversation!!.conversationId, result!!.member.userId, text)
                            messageDao.insert(msg)
                            mHandler.post {
                                message.setText("")
                                sendingMessage = false
                            }
                        }

                        override fun onError(apiError: NexmoAPIError?) {
                            sendingMessage = false
                            Log.e(TAG, "Send message error", apiError)
                            apiError?.message?.let {
                                NexmoApp.instance.showError(apiError!!.message!!)
                            }
                        }
                    })
                } catch (e: SecureChatException) {
                    Log.e(TAG, "Message encryption error", e)
                    NexmoApp.instance.showError("Message encryption error")
                }
            }
        }
    }

    private fun receiveMessage(textMessage: Text) {
        Log.d(TAG, "Receive text message ${textMessage.id} from ${textMessage.member.userId} \n" +
                "Text is: ${textMessage.text}")

        var message: Message?
        if (conversationClient.user.userId.equals(textMessage.member.userId)) {

            // This message was sent by myself. Find in database
            Log.v(TAG, "Message ${textMessage.id} was sent by myself")
            val hash = textMessage.text.hashCode().toString()
            message = messageDao.getMessage(mConversation!!.conversationId, hash)

            if (message != null) {
                // Remove cached message in database
                messageDao.delete(mConversation!!.conversationId, hash)

                // Set new ID for message
                message.messageId = textMessage.id
                message.date = textMessage.timestamp
            }
        } else {
            // Message from other conversation member
            Log.v(TAG, "Message ${textMessage.id} was sent by ${textMessage.member.userId}")

            // Try to find message in database
            message = messageDao.getMessage(mConversation!!.conversationId, textMessage.id)
            if (message == null) {
                // Decrypt message
                var text: String
                try {
                    text = VirgilFacade.instance.decrypt(textMessage.text, mMemberCard!!)
                } catch (e: SecureChatException) {
                    Log.e(TAG, "Decryption failed", e)
                    text = "Decryption failed"
                }
                message = Message(textMessage.id, mConversation!!.conversationId,
                        textMessage.member.userId, text, textMessage.timestamp)
            }
        }
        message?.let {
            mMessages.add(message!!)

            // Save message in database
            messageDao.insert(message!!)
        }

        mHandler.post {
            mAdapter?.notifyDataSetChanged()
            messages.scrollToPosition(mMessages.size - 1)
        }
    }

    private fun loadMessages() {
        Log.d(TAG, "Load messages for conversation ${mConversation?.conversationId}")
        val storedMessages = messageDao.getMessages(mConversation!!.conversationId)
        mMessages.addAll(storedMessages)

        mConversation?.events?.forEach {
            if (it is Text) {
                receiveMessage(it)
            }
        }

        mMessages.sortWith(compareBy(Message::date))
        mHandler.post {
            mAdapter?.notifyDataSetChanged()
            messages.scrollToPosition(mMessages.size - 1)
        }
    }

    private fun initMembers() {
        if (mMemberCard == null) {
            val userName = NexmoUtils.getConversationPartner(mConversation!!)?.name
            mMemberCard = VirgilFacade.instance.virgilApi.cards.find(userName).firstOrNull()?.model

            Log.d(TAG, "User's ${userName} Virgil Card id is ${mMemberCard?.id}")
        }
    }

    private fun closeWithError(message: String, apiError: NexmoAPIError?) {
        Log.e(TAG, message, apiError)
        NexmoApp.instance.showError(message)
        detachConversation()
        progressDialog?.dismiss()
        finish()
    }
}
