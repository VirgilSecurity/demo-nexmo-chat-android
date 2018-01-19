package com.virgilsecurity.chat.nexmo.fragments

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.adapters.ContactsRecyclerViewAdapter
import com.virgilsecurity.chat.nexmo.http.model.NexmoUser
import com.virgilsecurity.chat.nexmo.virgil.VirgilFacade

class ContactsFragment : Fragment() {
    private val TAG = "NexmoContacts"

    private var mListener: OnSelectContactFragmentInteractionListener? = null
    private var mUsersList = ArrayList<NexmoUser>()
    private var mAdapter: ContactsRecyclerViewAdapter? = null
    private var mHandler: Handler = Handler()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectContactFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        AsyncTask.execute {
            val virgilToken = VirgilFacade.instance.getVirgilToken()
            val response = NexmoApp.instance.serverClient.getUsers("Bearer ${virgilToken}").execute()
            if (response.isSuccessful) {
                var users = response.body()
                users?.let {
                    Log.d(TAG, "${users.size} users found")
                    // Add all users except myself
                    mUsersList.clear()
                    val myId = NexmoApp.instance.conversationClient.user?.userId
                    users.forEach {
                        if (!myId.equals(it.id)) {
                            mUsersList.add(it)
                        }
                    }
                    // Sort users by name
                    mUsersList.sortWith(compareBy(NexmoUser::name))

                    // Show users in list
                    mHandler.post {
                        mAdapter?.notifyDataSetChanged()
                    }
                }
            } else {
                Log.e(TAG, response.message())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_contacts_list, container, false)
        mAdapter = ContactsRecyclerViewAdapter(mUsersList, mListener)
        var recyclerView = view.findViewById<RecyclerView>(R.id.contacts)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = mAdapter

        return view
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnSelectContactFragmentInteractionListener {
        fun onContactSelectedFragmentInteraction(item: NexmoUser)
    }

    companion object {
        fun newInstance(): ContactsFragment {
            return ContactsFragment()
        }
    }
}
