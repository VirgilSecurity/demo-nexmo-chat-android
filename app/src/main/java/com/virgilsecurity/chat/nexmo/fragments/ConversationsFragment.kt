package com.virgilsecurity.chat.nexmo.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nexmo.sdk.conversation.client.Conversation
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.adapters.ConversationRecyclerViewAdapter

class ConversationsFragment : Fragment() {
    private val TAG = "NexmoConversations"

    private var mListener: OnSelectConversationFragmentInteractionListener? = null
    private var mConversationsList = ArrayList<Conversation>()
    private var mAdapter: ConversationRecyclerViewAdapter? = null
    private var mHandler: Handler = Handler()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectConversationFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnSelectConversationFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        NexmoApp.instance.conversationClient.getConversations(object: RequestHandler<List<Conversation>> {
            override fun onError(apiError: NexmoAPIError?) {
                Log.e(TAG, "Conversations are not loaded", apiError)
                NexmoApp.instance.showError("Can't load conversations")
            }

            override fun onSuccess(result: List<Conversation>?) {
                Log.d(TAG, "Loaded ${result?.size} conversation")
                mConversationsList.clear()
                mConversationsList.addAll(result!!)

                mHandler.post {
                    mAdapter?.notifyDataSetChanged()
                }
            }

        })
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_conversations_list, container, false)
        mAdapter = ConversationRecyclerViewAdapter(mConversationsList, mListener)
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
        fun onConversationSelectedFragmentInteraction(id: String)
    }

    companion object {
        fun newInstance(): ConversationsFragment {
            return ConversationsFragment()
        }
    }
}