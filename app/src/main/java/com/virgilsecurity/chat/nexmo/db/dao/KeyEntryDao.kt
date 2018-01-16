package com.virgilsecurity.chat.nexmo.db.dao

import android.arch.persistence.room.*
import com.virgilsecurity.chat.nexmo.db.model.DBKeyEntry

@Dao interface KeyEntryDao {

    @Query("SELECT * FROM key_entries")
    fun getAllKeys(): List<DBKeyEntry>

    @Query("SELECT * FROM key_entries WHERE name = :name")
    fun findKeyByName(name: String): DBKeyEntry

    @Query("SELECT count(*) FROM key_entries WHERE name = :name")
    fun count(name: String): Int

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertKey(key: DBKeyEntry)

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertAll(products: MutableList<DBKeyEntry>) : Unit

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateKey(key: DBKeyEntry)

    @Query("DELETE FROM key_entries WHERE name = :name")
    fun deleteKey(name: String)
}
