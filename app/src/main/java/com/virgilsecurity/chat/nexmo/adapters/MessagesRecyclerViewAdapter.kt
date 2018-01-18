package com.virgilsecurity.chat.nexmo.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import android.widget.TextView
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.db.model.Message

class MessagesRecyclerViewAdapter(private val myId:String, private val mValues: List<Message>) :
        RecyclerView.Adapter<MessagesRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mConversationNameView.text = mValues[position].text
        if (myId.equals(mValues[position].senderId)) {
            holder.mLeftSpacer.visibility = View.VISIBLE
            holder.mRightSpacer.visibility = View.GONE
            holder.mConversationNameView.setBackgroundResource(R.drawable.my_message_background)
        } else {
            holder.mLeftSpacer.visibility = View.GONE
            holder.mRightSpacer.visibility = View.VISIBLE
            holder.mConversationNameView.setBackgroundResource(R.drawable.member_message_background)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mConversationNameView: TextView
        val mLeftSpacer: Space
        val mRightSpacer: Space
        var mItem: Message? = null

        init {
            mConversationNameView = mView.findViewById(R.id.text)
            mLeftSpacer = mView.findViewById(R.id.left_space)
            mRightSpacer = mView.findViewById(R.id.right_space)
        }

        override fun toString(): String {
            return super.toString() + " '" + mConversationNameView.text + "'"
        }
    }
}