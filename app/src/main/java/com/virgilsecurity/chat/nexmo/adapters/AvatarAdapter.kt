package com.virgilsecurity.chat.nexmo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.virgilsecurity.chat.nexmo.R
import com.virgilsecurity.chat.nexmo.db.model.User
import kotlinx.android.synthetic.main.avatar_layout.view.*


class AvatarAdapter: BaseAdapter {

    var usersList = ArrayList<User>()
    var context: Context? = null

    constructor(context: Context, usersList: ArrayList<User>): super() {
        this.context = context
        this.usersList = usersList
    }

    override fun getCount(): Int {
        return usersList.size
    }

    override fun getItem(position: Int): Any? {
        return usersList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val user = usersList[position]

        var inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var avatarView = inflator.inflate(R.layout.avatar_layout, null)
        avatarView.avatar.setImageResource(R.drawable.avatar)
        avatarView.username.text = user.name

        return avatarView
    }
}