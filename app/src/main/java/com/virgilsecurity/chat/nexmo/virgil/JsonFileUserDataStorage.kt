package com.virgilsecurity.chat.nexmo.virgil


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.virgilsecurity.sdk.crypto.exceptions.KeyStorageException
import com.virgilsecurity.sdk.securechat.UserDataStorage

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.HashMap

class JsonFileUserDataStorage(private val directoryName: String, private val fileName: String) : UserDataStorage {

    private var gson: Gson

    private class Storages : HashMap<String, Entries>()

    private class Entries : HashMap<String, String>()

    init {
        val builder = GsonBuilder()
        this.gson = builder.disableHtmlEscaping().create()
        init()
    }

    override fun addData(storageName: String, key: String, value: String) {
        synchronized(this) {
            val storages = load()
            var entries: Entries? = storages[storageName]
            if (entries == null) {
                entries = Entries()
                storages.put(storageName, entries)
            }
            entries.put(key, value)
            save(storages)
        }
    }

    override fun getAllData(storageName: String): Map<String, String> {
        synchronized(this) {
            val storages = load()
            return storages[storageName] ?: return HashMap()
        }
    }

    override fun getData(storageName: String, key: String): String? {
        synchronized(this) {
            val storages = load()
            val entries = storages[storageName] ?: return null
            return entries[key]
        }
    }

    override fun removeAll(storageName: String) {
        synchronized(this) {
            val storages = load()
            storages.remove(storageName)
            save(storages)
        }
    }

    override fun removeData(storageName: String, key: String) {
        synchronized(this) {
            val storages = load()
            val entries = storages[storageName] ?: return
            entries.remove(key)
            save(storages)
        }
    }

    override fun synchronize() {

    }

    private fun init() {
        val dir = File(this.directoryName)

        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IllegalArgumentException("Not a directory: " + this.directoryName)
            }
        } else {
            dir.mkdirs()
        }
        val file = File(dir, this.fileName)
        if (!file.exists()) {
            save(Storages())
        }
    }

    private fun load(): Storages {
        val file = File(this.directoryName, this.fileName)
        try {
            FileInputStream(file).use { inStream ->
                val os = ByteArrayOutputStream()

                val buffer = ByteArray(4096)
                var n = inStream.read(buffer)
                while (-1 != n) {
                    os.write(buffer, 0, n)
                    n = inStream.read(buffer)
                }

                val bytes = os.toByteArray()
                val string = String(bytes, Charset.forName("UTF-8"))
                return gson.fromJson(string, Storages::class.java)
            }
        } catch (e: Exception) {
            return Storages()
        }

    }

    private fun save(storages: Storages) {
        val file = File(this.directoryName, this.fileName)
        try {
            FileOutputStream(file).use { os ->
                val json = gson.toJson(storages)
                os.write(json.toByteArray(Charset.forName("UTF-8")))
            }
        } catch (e: Exception) {
            throw KeyStorageException(e)
        }

    }
}