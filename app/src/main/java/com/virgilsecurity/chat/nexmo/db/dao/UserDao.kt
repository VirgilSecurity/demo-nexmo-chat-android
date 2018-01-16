package com.virgilsecurity.chat.nexmo.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.virgilsecurity.chat.nexmo.db.model.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY user_name")
    fun getUsers() : List<User>

    @Query("SELECT * FROM users WHERE user_name = :userName")
    fun findByName(userName: String) : User

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<User>)
}