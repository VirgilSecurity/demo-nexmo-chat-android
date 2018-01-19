package com.virgilsecurity.chat.nexmo.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.callbacks.OnNewUserRegisteredListener
import com.virgilsecurity.chat.nexmo.callbacks.RegistrationListener
import kotlinx.android.synthetic.main.fragment_registration.*
import com.virgilsecurity.chat.nexmo.virgil.VirgilFacade

class RegistrationFragment : Fragment(), RegistrationListener {

    private val TAG = "NexmoRegistration"
    private var progressDialog: ProgressDialog? = null
    private var registrationTask: RegistrationAsyncTask? = null
    private var mListener: OnNewUserRegisteredListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                next.isEnabled = !p0.isNullOrBlank();
            }
        })

        next.setOnClickListener {
            // Register new User
            registrationTask = RegistrationAsyncTask()
            registrationTask!!.listener = this
            registrationTask!!.execute(username.text.toString().trim())
        }

        agreements.setMovementMethod(LinkMovementMethod.getInstance());

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNewUserRegisteredListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onRegistrationStarted() {
        progressDialog = ProgressDialog.show(activity, "", getString(R.string.registration_in_progress), true)
    }

    override fun onRegistrationFinished(userName: String, jwt: String) {
        Log.d(TAG, "New account ${userName} registered")
        progressDialog?.dismiss()

        mListener?.onNewUserRegistered(userName, jwt)
    }

    override fun onRegistrationError(errorMessage: String) {
        progressDialog?.dismiss()
        NexmoApp.instance.showError(String.format(getString(R.string.registration_failed), errorMessage));
    }

    private inner class RegistrationAsyncTask : AsyncTask<String, Void, String>() {
        var identity: String? = null
        var listener: RegistrationListener? = null
        private var errorMessage: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            if (listener != null) {
                listener!!.onRegistrationStarted()
            }
        }

        // Returns null if registration successful. Error message otherwise
        override fun doInBackground(vararg params: String): String? {
            try {
                identity = params[0]
                val jwt = VirgilFacade.instance.register(identity!!)
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
                    listener!!.onRegistrationFinished(identity!!, jwt!!)
                }
            } else {
                if (listener != null) {
                    listener!!.onRegistrationError(errorMessage!!)
                }
            }
        }
    }

    companion object {

        fun newInstance(): RegistrationFragment {
            return RegistrationFragment()
        }
    }
}