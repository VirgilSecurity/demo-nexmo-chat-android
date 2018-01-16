package com.virgilsecurity.chat.nexmo.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.virgilsecurity.chat.nexmo.R
import kotlinx.android.synthetic.main.fragment_no_accounts.*
import com.virgilsecurity.chat.nexmo.callbacks.OnCreateNewUserListener

class NoAccountsFragment : Fragment() {

    private var mListener: OnCreateNewUserListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_no_accounts, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        create_account.setOnClickListener {
            mListener?.onCreateNewUser()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCreateNewUserListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {

        fun newInstance(): NoAccountsFragment {
            return NoAccountsFragment()
        }
    }
}
