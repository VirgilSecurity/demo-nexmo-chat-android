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
import android.widget.Toast
import com.virgilsecurity.chat.nexmo.fragments.ContactsFragment
import com.virgilsecurity.chat.nexmo.fragments.ConversationsFragment
import com.virgilsecurity.chat.nexmo.http.model.NexmoUser

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
                R.id.option_settings -> {
                    Toast.makeText(applicationContext, "Settings", Toast.LENGTH_SHORT).show()
                    drawerLayout!!.closeDrawers()
                }
            }
            true
        }
        val header = navigationView.getHeaderView(0)
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

    override fun onContactSelectedFragmentInteraction(item: NexmoUser) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user", item.id)
        startActivity(intent)
    }

    override fun onConversationSelectedFragmentInteraction(id: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("conversation", id)
        startActivity(intent)
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
}