package com.virgilsecurity.chat.nexmo.db.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.virgilsecurity.sdk.storage.KeyEntry

@Entity(tableName = "key_entries")
class DBKeyEntry : KeyEntry {
    @PrimaryKey
    private var name: String? = null
    private var value: ByteArray? = null
    private var metadata: MutableMap<String, String> = HashMap()

    override fun setValue(value: ByteArray?) {
        this.value = value
    }

    override fun setName(name: String?) {
        this.name = name
    }

    override fun getMetadata(): MutableMap<String, String> {
        return metadata
    }

    override fun getName(): String? {
        return this.name
    }

    override fun getValue(): ByteArray? {
        return this.value
    }
}