package com.virgilsecurity.chat.nexmo

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.nexmo.sdk.conversation.client.User
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import com.virgilsecurity.chat.nexmo.callbacks.OnCreateNewUserListener
import com.virgilsecurity.chat.nexmo.callbacks.OnNewUserRegisteredListener
import com.virgilsecurity.chat.nexmo.callbacks.OnSelectUserListener
import com.virgilsecurity.chat.nexmo.fragments.NoAccountsFragment
import com.virgilsecurity.chat.nexmo.fragments.RegistrationFragment
import com.virgilsecurity.chat.nexmo.fragments.SelectAccountForSingInFragment

class LoginActivity : FragmentActivity(), OnCreateNewUserListener, OnSelectUserListener,
        OnNewUserRegisteredListener {
    private val TAG = "LoginActivity"
    private var noAccountsFragment: Fragment? = null
    private var selectAccountFragment: Fragment? = null
    private var registrationFragment: Fragment? = null
    private var registrationShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()
        showStartScreen()
    }

    override fun onBackPressed() {
        if (registrationShown) {
            showStartScreen()
        } else {
            super.onBackPressed()
        }
    }

    private fun showStartScreen() {
        AsyncTask.execute {
            if (NexmoApp.instance.db.userDao().getUsers().isEmpty()) {
                showNoAccountsFragment()
            } else {
                showSelectAccountFragment()
            }
        }
    }

    override fun onCreateNewUser() {
        Log.d(TAG, "Create new account")
        showRegistrationFragment()
    }

    override fun onSelectUser(userName: String, jwt: String) {
        Log.d(TAG, "Select account: ${userName}")
        loginNexmo(userName, jwt)
    }

    override fun onNewUserRegistered(userName: String, jwt: String) {
        Log.d(TAG, "Registered account: ${userName}")
        loginNexmo(userName, jwt)
    }

    private fun openMainActivity(userName: String, jwt: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", userName)
        intent.putExtra("jwt", jwt)
        startActivity(intent)
        finish()
    }

    private fun loginNexmo(userName: String, jwt: String) {
        val conversationClient = NexmoApp.instance.conversationClient
        val token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE1MTYwODQ3OTcsImp0aSI6IjExYzg4NjYwLWZhODgtMTFlNy05ZGRmLTYxYjRiZTBlOTY2YyIsInN1YiI6ImE1IiwiZXhwIjoiMTUxNjE3MTE5NyIsImFjbCI6eyJwYXRocyI6eyIvdjEvc2Vzc2lvbnMvKioiOnt9LCIvdjEvdXNlcnMvKioiOnt9LCIvdjEvY29udmVyc2F0aW9ucy8qKiI6e319fSwiYXBwbGljYXRpb25faWQiOiJhMGRlNTZjZS0wYjYxLTQxNzctYjdkOS1jMDg1OGEwNmE4MTcifQ.b3pM2HXtwKhao-sg3uymJR5Qk8uKujgrlO4kKf30tdRhTQX_j7gMfGwoayOuStqgPveiitr-3F62JkrsZBwWW4knUt6B4FzwrnPPnjfef4nW-3Oe_CdJlTrq1f6gow6RPjeAz-sYlvBSTIDH9ZjapxhCAbNoTE-bwoTQNPSvb6y0cFWqYKuxc4_n83b64gzq8dm_C4IyKSbrGIXKovMnB1rX9barEw6iuSTlFkxqhlaZcTJwhVPPyms2VQd7w5FV-o5SvFoMmnMLQbGoRFM-hGstL5EXTxi4v_uo6nnxVqfpLC0vTCg8Aqb5_vyjgop-3YnrmWtAecnDAvE4Hyefyw"
        conversationClient.login(token, object: RequestHandler<User> {
            override fun onSuccess(result: User?) {
                Log.d(TAG, "Logged in to Nexmo")
                openMainActivity(userName, jwt)
            }

            override fun onError(apiError: NexmoAPIError?) {
                Log.e(TAG, "Login to Nexmo failed. ${apiError}", apiError)
                NexmoApp.instance.showError("Login to Nexmo failed")
            }
        })
    }

    private fun showNoAccountsFragment() {
        if (noAccountsFragment == null) {
            noAccountsFragment = NoAccountsFragment.newInstance()
        }
        registrationShown = false
        showFragment(noAccountsFragment!!)
    }

    private fun showSelectAccountFragment() {
        if (selectAccountFragment == null) {
            selectAccountFragment = SelectAccountForSingInFragment.newInstance()
        }
        registrationShown = false
        showFragment(selectAccountFragment!!)
    }

    private fun showRegistrationFragment() {
        if (registrationFragment == null) {
            registrationFragment = RegistrationFragment.newInstance()
        }
        registrationShown = true
        showFragment(registrationFragment!!)
    }

    private fun showFragment(newFragment: Fragment) {
        Log.d(TAG, "Switch to fragment ${newFragment.javaClass.canonicalName}")
        var fragmentTransaction = supportFragmentManager
                .beginTransaction()
        fragmentTransaction.replace(R.id.content, newFragment, "rageComicList")
                .commit()
    }

}
