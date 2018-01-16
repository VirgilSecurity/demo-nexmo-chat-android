package com.virgilsecurity.chat.nexmo.adapters

import com.virgilsecurity.chat.nexmo.R
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.virgilsecurity.chat.nexmo.fragments.ContactsFragment.OnSelectContactFragmentInteractionListener
import com.virgilsecurity.chat.nexmo.http.model.NexmoUser

class ContactsRecyclerViewAdapter(private val mValues: List<NexmoUser>, private val mListener: OnSelectContactFragmentInteractionListener?) : RecyclerView.Adapter<ContactsRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_contact_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mUserNameView.text = mValues[position].name

        holder.mView.setOnClickListener {
            mListener?.onContactSelectedFragmentInteraction(holder.mItem!!)
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mUserNameView: TextView
        var mItem: NexmoUser? = null

        init {
            mUserNameView = mView.findViewById(R.id.username)
        }

        override fun toString(): String {
            return super.toString() + " '" + mUserNameView.text + "'"
        }
    }
}
