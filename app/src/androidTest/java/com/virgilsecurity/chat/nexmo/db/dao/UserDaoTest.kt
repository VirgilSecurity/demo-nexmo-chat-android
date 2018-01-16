package com.virgilsecurity.chat.nexmo.db.dao

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.virgilsecurity.chat.nexmo.db.AppDatabase
import com.virgilsecurity.chat.nexmo.db.model.User
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    lateinit var userDao: UserDao
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertedAndRetrievedUsersMatch() {
        val users = listOf(User(UUID.randomUUID().toString(), "Name", "href",
                UUID.randomUUID().toString(), UUID.randomUUID().toString().toByteArray()))
        userDao.insertAll(users)

        val allUsers = userDao.getUsers()
        Assert.assertEquals(users, allUsers)
    }

    @Test
    fun testFindByName() {
        val users = listOf(User(UUID.randomUUID().toString(), "Name1", "href",
                UUID.randomUUID().toString(), UUID.randomUUID().toString().toByteArray()))
        userDao.insertAll(users)

        val user = userDao.findByName("Name1")
        Assert.assertEquals(users[0], user)
    }
}