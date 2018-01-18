package com.virgilsecurity.chat.nexmo.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nexmo.sdk.conversation.client.Conversation
import com.nexmo.sdk.conversation.client.ConversationClient
import com.nexmo.sdk.conversation.client.Member
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import com.nexmo.sdk.conversation.core.SubscriptionList
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.adapters.ConversationsRecyclerViewAdapter
import com.virgilsecurity.chat.nexmo.utils.NexmoUtils

class ConversationsFragment : Fragment() {
    private val TAG = "NexmoConversations"

    private val mConversationClient: ConversationClient = NexmoApp.instance.conversationClient
    private val mSubscriptions: SubscriptionList = SubscriptionList()
    private var mListener: OnSelectConversationFragmentInteractionListener? = null
    private var mConversationsList = ArrayList<ConversationsRecyclerViewAdapter.ConversationDto>()
    private var mAdapter: ConversationsRecyclerViewAdapter? = null
    private var mHandler: Handler = Handler()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectConversationFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnSelectConversationFragmentInteractionListener")
        }
    }

    override fun onStart() {
        super.onStart()
        mConversationClient.getConversations(object: RequestHandler<List<Conversation>> {
            override fun onError(apiError: NexmoAPIError?) {
                Log.e(TAG, "Conversations are not loaded", apiError)
                NexmoApp.instance.showError("Can't load conversations")
            }

            override fun onSuccess(result: List<Conversation>?) {
                Log.d(TAG, "Loaded ${result?.size} mConversation")
                mConversationsList.clear()
                var itemNotProcessed = result!!.size
                result?.forEach {
                    it.update(object: RequestHandler<Conversation> {
                        override fun onError(apiError: NexmoAPIError?) {
                            Log.e(TAG, "Failed to update Conversation ${it.conversationId}")
                            if (--itemNotProcessed == 0) {
                                mHandler.post {
                                    mConversationsList.sortWith(compareBy(ConversationsRecyclerViewAdapter.ConversationDto::name))
                                    mAdapter?.notifyDataSetChanged()
                                }
                            }
                        }

                        override fun onSuccess(result: Conversation?) {
                            result?.let{
                                mConversationsList.add(ConversationsRecyclerViewAdapter.ConversationDto(it.conversationId, NexmoUtils.getConversationName(it)))
                                if (--itemNotProcessed == 0) {
                                    mHandler.post {
                                        mConversationsList.sortWith(compareBy(ConversationsRecyclerViewAdapter.ConversationDto::name))
                                        mAdapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                        }

                    })
                }
            }

        })
    }

    override fun onResume() {
        super.onResume()
        mConversationClient.invitedEvent().add {
            it.conversation.join(object : RequestHandler<Member> {
                override fun onSuccess(result: Member?) {
                    Log.d(TAG, "Joined to conversation")
                }

                override fun onError(apiError: NexmoAPIError?) {
                    Log.e(TAG, "Join to conversation error", apiError)
                }

            })
        }.addTo(mSubscriptions)
    }

    override fun onPause() {
        super.onPause()
        mSubscriptions.unsubscribeAll()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_conversations_list, container, false)
        mAdapter = ConversationsRecyclerViewAdapter(mConversationsList, mListener)
        var recyclerView = view.findViewById<RecyclerView>(R.id.conversations)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = mAdapter

        return view
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnSelectConversationFragmentInteractionListener {
        fun onConversationSelectedFragmentInteraction(conversation: ConversationsRecyclerViewAdapter.ConversationDto)
    }

    companion object {
        fun newInstance(): ConversationsFragment {
            return ConversationsFragment()
        }
    }
}