package com.virgilsecurity.chat.nexmo

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import com.virgilsecurity.chat.nexmo.adapters.ConversationsRecyclerViewAdapter
import com.virgilsecurity.chat.nexmo.fragments.ContactsFragment
import com.virgilsecurity.chat.nexmo.fragments.ConversationsFragment
import com.virgilsecurity.chat.nexmo.http.model.NexmoUser
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity(), ContactsFragment.OnSelectContactFragmentInteractionListener, ConversationsFragment.OnSelectConversationFragmentInteractionListener {
    private val TAG = "NexmoMain"

    private var drawerLayout: DrawerLayout? = null
    private var toolbar: Toolbar? = null

    private var jwt: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        initNavigationDrawer()

        if (intent.hasExtra("jwt")) {
            jwt = intent.getStringExtra("jwt")
            Log.d(TAG, "JWT is ${jwt}")
        }
        if (savedInstanceState == null) {
            showConversations()
        }
    }

    private fun initNavigationDrawer() {
        // refer the navigation view of the xml
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.getHeaderView(0).findViewById<TextView>(R.id.username).text = NexmoApp.instance.conversationClient.user.name

        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            when (id) {
                R.id.option_contacts -> {
                    showContacts()
                    drawerLayout!!.closeDrawers()
                }
                R.id.option_chats -> {
                    showConversations()
                    drawerLayout!!.closeDrawers()
                }
                R.id.option_logout -> {
                    logout()
                }
            }
            true
        }

        drawerLayout = findViewById(R.id.drawer)
        val actionBarDrawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            override fun onDrawerClosed(v: View?) {
                super.onDrawerClosed(v)
            }

            override fun onDrawerOpened(v: View?) {
                super.onDrawerOpened(v)
            }
        }
        drawerLayout!!.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
    }

    private fun logout() {
        NexmoApp.instance.conversationClient.logout(object: RequestHandler<Any> {
            override fun onSuccess(result: Any?) {
                Log.d(TAG, "Logout complete")
                NexmoApp.instance.logout()
                openLogin()
            }

            override fun onError(apiError: NexmoAPIError?) {
                Log.e(TAG, "Logout error", apiError)
            }

        })
    }

    override fun onContactSelectedFragmentInteraction(item: NexmoUser) {
        val intent = Intent(this, ConversationActivity::class.java)
        intent.putExtra("user_id", item.id)
        intent.putExtra("user_name", item.name)
        intent.putExtra("title", item.name)
        startActivity(intent)
    }

    override fun onConversationSelectedFragmentInteraction(conversation: ConversationsRecyclerViewAdapter.ConversationDto) {
        val intent = Intent(this, ConversationActivity::class.java)
        intent.putExtra("conversation_id", conversation.conversationId)
        intent.putExtra("title", conversation.name)
        startActivity(intent)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun showContacts() {
        title = getString(R.string.title_contacts)
        showFragment(ContactsFragment.newInstance())
    }

    private fun showConversations() {
        title = getString(R.string.title_conversations)
        showFragment(ConversationsFragment.newInstance())
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment, "rageComicList")
                .commit()
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}