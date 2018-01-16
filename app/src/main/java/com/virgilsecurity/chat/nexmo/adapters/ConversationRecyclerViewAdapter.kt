package com.virgilsecurity.chat.nexmo.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.nexmo.sdk.conversation.client.Conversation
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.fragments.ConversationsFragment

class ConversationRecyclerViewAdapter(private val mValues: List<Conversation>, private val mListener: ConversationsFragment.OnSelectConversationFragmentInteractionListener?) : RecyclerView.Adapter<ConversationRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_conversation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mConversationNameView.text = mValues[position].displayName

        holder.mView.setOnClickListener {
            mListener?.onConversationSelectedFragmentInteraction(holder.mItem!!.conversationId)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mConversationNameView: TextView
        var mItem: Conversation? = null

        init {
            mConversationNameView = mView.findViewById(R.id.name)
        }

        override fun toString(): String {
            return super.toString() + " '" + mConversationNameView.text + "'"
        }
    }
}