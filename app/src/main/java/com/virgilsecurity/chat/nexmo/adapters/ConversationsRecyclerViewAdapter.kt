package com.virgilsecurity.chat.nexmo.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.nexmo.sdk.conversation.client.Conversation
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.fragments.ConversationsFragment
import com.virgilsecurity.chat.nexmo.utils.NexmoUtils

class ConversationsRecyclerViewAdapter(private val mValues: List<ConversationDto>, private val mListener: ConversationsFragment.OnSelectConversationFragmentInteractionListener?) : RecyclerView.Adapter<ConversationsRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_conversation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mConversationNameView.text = mValues[position].name

        holder.mView.setOnClickListener {
            mListener?.onConversationSelectedFragmentInteraction(holder.mItem!!)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mConversationNameView: TextView
        var mItem: ConversationDto? = null

        init {
            mConversationNameView = mView.findViewById(R.id.name)
        }

        override fun toString(): String {
            return super.toString() + " '" + mConversationNameView.text + "'"
        }
    }

    data class ConversationDto(
            val conversationId: String,
            val name: String
    )
}