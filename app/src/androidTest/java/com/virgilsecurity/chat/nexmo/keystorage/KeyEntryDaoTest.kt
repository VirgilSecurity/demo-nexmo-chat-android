package com.virgilsecurity.chat.nexmo.keystorage
/*
import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.virgilsecurity.chat.nexmo.db.AppDatabase
import com.virgilsecurity.chat.nexmo.db.dao.KeyEntryDao
import com.virgilsecurity.chat.nexmo.db.model.DBKeyEntry
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyEntryDaoTest {

    lateinit var keyDao: KeyEntryDao
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        keyDao = database.keyEntryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertedAndRetrievedKeysMatch() {
        val keyEntry = DBKeyEntry()
        keyEntry.name = "name"
        keyEntry.value = "value".toByteArray()
        keyDao.insertKey(keyEntry)

        val keyFount = keyDao.findKeyByName("name")
        Assert.assertEquals(keyEntry, keyFount)
    }

}
*/