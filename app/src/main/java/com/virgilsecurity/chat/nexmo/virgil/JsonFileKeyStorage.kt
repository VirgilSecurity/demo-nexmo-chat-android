package com.virgilsecurity.chat.nexmo.virgil


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.virgilsecurity.sdk.crypto.exceptions.KeyEntryAlreadyExistsException
import com.virgilsecurity.sdk.crypto.exceptions.KeyEntryNotFoundException
import com.virgilsecurity.sdk.crypto.exceptions.KeyStorageException
import com.virgilsecurity.sdk.securechat.keystorage.KeyAttrs
import com.virgilsecurity.sdk.securechat.keystorage.KeyStorage
import com.virgilsecurity.sdk.storage.KeyEntry
import com.virgilsecurity.sdk.storage.VirgilKeyEntry
import com.virgilsecurity.sdk.utils.ConvertionUtils
import com.virgilsecurity.sdk.utils.StringUtils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

class JsonFileKeyStorage : KeyStorage {

    private var directoryName: String? = null

    private var fileName: String? = null

    /**
     * @param gson the gson to set
     */
    private lateinit var gson: Gson

    private class ByteArrayToBase64TypeAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
            return ConvertionUtils.base64ToBytes(json.asString)
        }

        override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(ConvertionUtils.toBase64String(src))
        }
    }

    private class ClassTypeAdapter : TypeAdapter<Class<*>>() {
        @Throws(IOException::class)
        override fun read(jsonReader: JsonReader): Class<*>? {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull()
                return null
            }
            var clazz: Class<*>? = null
            try {
                clazz = Class.forName(jsonReader.nextString())
            } catch (exception: ClassNotFoundException) {
                throw IOException(exception)
            }

            return clazz
        }

        @Throws(IOException::class)
        override fun write(jsonWriter: JsonWriter, clazz: Class<*>?) {
            if (clazz == null) {
                jsonWriter.nullValue()
                return
            }
            jsonWriter.value(clazz.name)
        }
    }

    private class ClassTypeAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
            return if (!Class::class.java.isAssignableFrom(typeToken.rawType)) {
                null
            } else ClassTypeAdapter() as TypeAdapter<T>
        }
    }

    private class Entries : HashMap<String, VirgilKeyEntry>() {
        companion object {
            private val serialVersionUID = 261773342073013945L
        }

    }

    /**
     * Create a new instance of `VirgilKeyStorage`
     *
     * @param directoryName The directory name which contains key storage file.
     * @param fileName      The key storage file name.
     */
    constructor(directoryName: String, fileName: String) {
        this.directoryName = directoryName
        this.fileName = fileName

        init()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.virgilsecurity.sdk.securechat.keystorage.KeyStorage#delete(java.util.
     * List)
     */
    override fun delete(keyNames: List<String>) {
        synchronized(this) {
            val entries = load()
            for (keyName in keyNames) {
                entries.remove(keyName)
            }
            save(entries)
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.virgilsecurity.sdk.crypto.KeyStore#delete(java.lang.String)
     */
    override fun delete(keyName: String) {
        synchronized(this) {
            val entries = load()
            if (!entries.containsKey(keyName)) {
                throw KeyEntryNotFoundException()
            }
            entries.remove(keyName)
            save(entries)
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.virgilsecurity.sdk.crypto.KeyStore#exists(java.lang.String)
     */
    override fun exists(keyName: String?): Boolean {
        if (keyName == null) {
            return false
        }
        synchronized(this) {
            val entries = load()
            return entries.containsKey(keyName)
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.virgilsecurity.sdk.securechat.keystorage.KeyStorage#getAllKeysAttrs()
     */
    override fun getAllKeysAttrs(): List<KeyAttrs> {
        var entries: Entries? = null
        synchronized(this) {
            entries = load()
        }
        val keyAttrs = ArrayList<KeyAttrs>(entries!!.size)
        for ((name, value) in entries!!) {
            var creationDate: Date? = null
            if (value.metadata != null) {
                val creationDateStr = value.metadata[CREATION_DATE_META_KEY]
                if (!StringUtils.isBlank(creationDateStr)) {
                    creationDate = gson.fromJson(creationDateStr, Date::class.java)
                }
            }
            if (creationDate == null) {
                creationDate = Date()
            }
            keyAttrs.add(KeyAttrs(name, creationDate))
        }
        return keyAttrs
    }

    private fun init() {
        // Initialize Gson
        val builder = GsonBuilder()
        this.gson = builder.registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayToBase64TypeAdapter())
                .registerTypeAdapterFactory(ClassTypeAdapterFactory()).disableHtmlEscaping()
                .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create()

        // Initialize keystore
        val dir = File(this.directoryName!!)

        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IllegalArgumentException("Not a directory: " + this.directoryName!!)
            }
        } else {
            dir.mkdirs()
        }
        val file = File(dir, this.fileName!!)
        if (!file.exists()) {
            save(Entries())
        }
    }

    private fun load(): Entries {
        val file = File(this.directoryName, this.fileName!!)
        try {
            FileInputStream(file).use { inStream ->
                val os = ByteArrayOutputStream()

                val buffer = ByteArray(4096)
                var n = inStream.read(buffer)
                while (-1 != n ) {
                    os.write(buffer, 0, n)
                    n = inStream.read(buffer)
                }

                val bytes = os.toByteArray()

                return gson.fromJson(String(bytes, Charset.forName("UTF-8")), Entries::class.java)
            }
        } catch (e: Exception) {
            throw KeyStorageException(e)
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.virgilsecurity.sdk.crypto.KeyStore#load(java.lang.String)
     */
    override fun load(keyName: String): KeyEntry {
        synchronized(this) {
            val entries = load()
            if (!entries.containsKey(keyName)) {
                throw KeyEntryNotFoundException()
            }
            val entry = entries[keyName]
            entry!!.setName(keyName)
            return entry
        }
    }

    /**
     * @param entries
     */
    private fun save(entries: Entries) {
        val file = File(this.directoryName, this.fileName!!)
        try {
            FileOutputStream(file).use { os ->
                val json = gson.toJson(entries)
                os.write(json.toByteArray(Charset.forName("UTF-8")))
            }
        } catch (e: Exception) {
            throw KeyStorageException(e)
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.virgilsecurity.sdk.crypto.KeyStore#store(com.virgilsecurity.sdk.
     * crypto.KeyEntry)
     */
    override fun store(keyEntry: KeyEntry) {
        val name = keyEntry.name
        val creationDateStr = gson.toJson(Date())

        synchronized(this) {
            val entries = load()
            if (entries.containsKey(name)) {
                throw KeyEntryAlreadyExistsException()
            }
            if (!keyEntry.metadata.containsKey(CREATION_DATE_META_KEY)) {
                keyEntry.metadata.put(CREATION_DATE_META_KEY, creationDateStr)
            }
            entries.put(name, keyEntry as VirgilKeyEntry)
            save(entries)
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.virgilsecurity.sdk.securechat.keystorage.KeyStorage#store(java.util.
     * List)
     */
    override fun store(keyEntries: List<KeyEntry>) {
        val creationDateStr = gson.toJson(Date())
        synchronized(this) {
            val entries = load()
            for (keyEntry in keyEntries) {
                if (!keyEntry.metadata.containsKey(CREATION_DATE_META_KEY)) {
                    keyEntry.metadata.put(CREATION_DATE_META_KEY, creationDateStr)
                }
                entries.put(keyEntry.name, keyEntry as VirgilKeyEntry)
            }
            save(entries)
        }
    }

    companion object {

        private val CREATION_DATE_META_KEY = "created_at"
    }
}