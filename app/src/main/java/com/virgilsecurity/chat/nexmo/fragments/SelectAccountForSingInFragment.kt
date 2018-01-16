package com.virgilsecurity.chat.nexmo.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.adapters.AvatarAdapter
import com.virgilsecurity.chat.nexmo.callbacks.LoginListener
import com.virgilsecurity.chat.nexmo.callbacks.OnSelectUserListener
import com.virgilsecurity.chat.nexmo.db.model.User
import com.virgilsecurity.chat.nexmo.virgil.VirgilFacade
import kotlinx.android.synthetic.main.fragment_select_account_for_sing_in.*

class SelectAccountForSingInFragment : Fragment(), LoginListener {
    private val TAG = "NexmoSelectAccount"

    private var progressDialog: ProgressDialog? = null
    private var mListener: OnSelectUserListener? = null
    private var loginTask: LoginAsyncTask? = null
    var adapter: AvatarAdapter? = null
    var usersList = ArrayList<User>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectUserListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnSelectUserListener")
        }
    }

    override fun onStart() {
        super.onStart()
        LoadUsersAsyncTask().execute()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater?.inflate(R.layout.fragment_select_account_for_sing_in, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        adapter = AvatarAdapter(activity, usersList)
        users.adapter = adapter
        users.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, pos, l ->
            loginTask = LoginAsyncTask()
            loginTask!!.listener = this
            loginTask!!.execute((adapter!!.getItem(pos) as User).name)
        }

        create_account.setOnClickListener {
            mListener?.onCreateNewUser()
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onLoginStarted() {
        progressDialog = ProgressDialog.show(activity, "", getString(R.string.login_in_progress), true)
    }

    override fun onLoginFinished(userName: String, jwt: String) {
        Log.d(TAG, "Account ${userName} login complete")
        progressDialog?.dismiss()

        mListener?.onSelectUser(userName, jwt)
    }

    override fun onLoginError(errorMessage: String) {
        progressDialog?.dismiss()
        NexmoApp.instance.showError(String.format(getString(R.string.registration_failed), errorMessage));
    }

    private inner class LoadUsersAsyncTask : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p0: Void?): Boolean {
            val users = NexmoApp.instance.db.userDao().getUsers()
            usersList.clear()
            usersList.addAll(users)

            return true
        }

        override fun onPostExecute(result: Boolean) {
            adapter!!.notifyDataSetChanged()
        }
    }

    private inner class LoginAsyncTask : AsyncTask<String, Void, String>() {
        var identity: String? = null
        var listener: LoginListener? = null
        private var errorMessage: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            if (listener != null) {
                listener!!.onLoginStarted()
            }
        }

        override fun doInBackground(vararg params: String): String? {
            try {
                identity = params[0]
                val jwt = VirgilFacade.instance.login(identity!!)
                return jwt
            } catch (e: Exception) {
                Log.e(TAG,"Registration failed", e)
                errorMessage = e.message
            }

            return null
        }

        override fun onPostExecute(jwt: String?) {
            super.onPostExecute(jwt)
            if (errorMessage == null) {
                if (listener != null) {
                    listener!!.onLoginFinished(identity!!, jwt!!)
                }
            } else {
                if (listener != null) {
                    listener!!.onLoginError(errorMessage!!)
                }
            }
        }
    }



    companion object {
        fun newInstance(): SelectAccountForSingInFragment {
            return SelectAccountForSingInFragment()
        }
    }
}
