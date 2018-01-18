package com.virgilsecurity.chat.nexmo.virgil

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.virgilsecurity.chat.nexmo.NexmoApp
import com.virgilsecurity.chat.nexmo.db.model.User
import com.virgilsecurity.chat.nexmo.exceptions.GetVirgilTokenException
import com.virgilsecurity.chat.nexmo.http.model.CSR
import com.virgilsecurity.sdk.client.VirgilAuthClient
import com.virgilsecurity.sdk.client.VirgilClientContext
import com.virgilsecurity.sdk.client.model.CardModel
import com.virgilsecurity.sdk.highlevel.VirgilApi
import com.virgilsecurity.sdk.highlevel.VirgilApiImpl
import java.net.URL
import com.virgilsecurity.sdk.crypto.Crypto
import com.virgilsecurity.sdk.crypto.VirgilCrypto
import com.virgilsecurity.sdk.highlevel.VirgilApiContext
import com.virgilsecurity.sdk.securechat.UserDataStorage
import com.virgilsecurity.sdk.securechat.keystorage.KeyStorage
import java.util.*
import com.virgilsecurity.sdk.crypto.exceptions.DecryptionException
import com.virgilsecurity.sdk.crypto.exceptions.EncryptionException
import com.virgilsecurity.sdk.utils.ConvertionUtils
import com.virgilsecurity.sdk.crypto.PrivateKey
import com.virgilsecurity.sdk.device.DefaultDeviceManager
import com.virgilsecurity.sdk.securechat.SecureChat
import com.virgilsecurity.sdk.securechat.SecureChatContext


class VirgilFacade {
    private val TAG = "NexmoVirgilFacade"

    private val context: Context
    val crypto: Crypto
    val virgilApi: VirgilApi
    private val authClient: VirgilAuthClient

    private var keyStorage: KeyStorage? = null
    private var userDataStorage: UserDataStorage? = null
    private var privateKey: PrivateKey? = null

    private var identity: String? = null
    private var virgilCard: CardModel? = null

    private var secureChat: SecureChat? = null

    constructor() {
        context = NexmoApp.instance.applicationContext

        crypto = VirgilCrypto()
        val apiCtx = VirgilApiContext(VIRGIL_ACCESS_TOKEN)
        apiCtx.crypto = crypto
        virgilApi = VirgilApiImpl(apiCtx)

        val ctx = VirgilClientContext(VIRGIL_ACCESS_TOKEN)
        ctx.authServiceURL = URL(AUTH_SERVER_URL)
        authClient = VirgilAuthClient(ctx)
    }

    fun register(userName: String): String {
        // Generate Virgil Card
        val virgilKey = virgilApi.keys.generate()
        val customFields = HashMap<String, String>()
        customFields.put("deviceId", Settings.Secure.ANDROID_ID)
        val virgilCard = virgilApi.getCards().create(userName, virgilKey,
                "name", customFields)

        // Register user
        val csr = CSR(virgilCard.export())
        val response = NexmoApp.instance.serverClient.signup(csr).execute()
        if (!response.isSuccessful) {
            throw Exception(response.message())
        }
        var registrationData = response.body()!!

        //TODO Verify Virgil Card which is returned by server
        val createdVirgilCard = virgilApi.cards.importCard(registrationData.user.virgilCard)

        // Store user
        NexmoApp.instance.db.userDao().insert(User(registrationData.user.id, userName,
                registrationData.user.href, createdVirgilCard.id, registrationData.user.virgilCard,
                virgilKey.privateKey.value))

        // Initialize facade with current user credentials
        init(userName, createdVirgilCard.model, virgilKey.privateKey)

        // Return Virgil authentication token
        return registrationData.jwt
    }

    fun login(userName: String): String {
        var user = NexmoApp.instance.db.userDao().findByName(userName)
        this.privateKey = this.crypto.importPrivateKey(user.virgilKey)

        // Obtain Virgil authentication token
        val virgilToken = getVirgilToken(userName, user.virgilCardId)

        // Get JWT
        val response = NexmoApp.instance.serverClient.jwt("Bearer ${virgilToken}").execute()
        if (!response.isSuccessful) {
            throw Error(response.message())
        }
        // Initialize facade with current user credentials
        init(userName, user.virgilCard, user.virgilKey)

        val jwt = response.body()!!.jwt

        // Return Virgil authentication token
        return jwt
    }

    fun getVirgilToken(): String? {
        this.identity?.let {
            return getVirgilToken(this.identity!!, this.virgilCard!!.id)
        }
        return null
    }

    fun encrypt(text: String, recipientCard: CardModel): String {
        var secureSession = secureChat!!.activeSession(recipientCard.getId());
        if (secureSession == null) {
            secureSession = secureChat!!.startNewSession(recipientCard, null);
        }
        val encryptedText = secureSession.encrypt(text);
        return encryptedText
    }

    fun decrypt(encryptedMessage: String, senderCard: CardModel): String {
        var secureSession = secureChat!!.loadUpSession(senderCard, encryptedMessage, null);
        return secureSession.decrypt(encryptedMessage);
    }

    @Throws(GetVirgilTokenException::class)
    private fun getVirgilToken(identity: String, cardId: String): String {
        val challengeMessage = this.authClient.getChallengeMessage(cardId)
        try {
            val decodedMessage = this.crypto.decrypt(ConvertionUtils.base64ToBytes(challengeMessage.encryptedMessage), this.privateKey)
            Log.d(TAG, "Decoded message: " + ConvertionUtils.toString(decodedMessage))

            val appPublicKey = this.crypto.importPublicKey(ConvertionUtils.base64ToBytes(VIRGIL_AUTH_PUBLIC_KEY))
            val newEncryptedMessage = this.crypto.encrypt(decodedMessage, appPublicKey)
            val message = ConvertionUtils.toBase64String(newEncryptedMessage)

            val code = this.authClient.acknowledge(challengeMessage.authorizationGrantId, message)
            val accessTokenResponse = this.authClient.obtainAccessToken(code)
            val accessToken = accessTokenResponse.accessToken
            Log.d(TAG, "Virgil token: " + accessToken)

            return accessToken
        } catch (e: EncryptionException) {
            Log.e(TAG, "Getting Virgil Token failed", e)
            throw GetVirgilTokenException()
        } catch (e: DecryptionException) {
            Log.e(TAG, "Getting Virgil Token failed", e)
            throw GetVirgilTokenException()
        }
    }

    private fun init(userName: String, exportedVirgilCard: String, virgilKeyData: ByteArray) {
        init(userName, virgilApi.cards.importCard(exportedVirgilCard).model,
                crypto.importPrivateKey(virgilKeyData))
    }

    private fun init(userName: String, virgilCard: CardModel, virgilKey: PrivateKey) {
        this.identity = userName
        this.virgilCard = virgilCard
        this.privateKey = virgilKey

        this.keyStorage = JsonFileKeyStorage(context.getFilesDir().getAbsolutePath(),
                userName + ".ks")
        this.userDataStorage = JsonFileUserDataStorage(context.getFilesDir().getAbsolutePath(),
                userName + ".ds")

        // Configure PFS
        var chatContext = SecureChatContext(this.virgilCard, this.privateKey, this.crypto,
                VIRGIL_ACCESS_TOKEN)
        chatContext.keyStorage = keyStorage
        chatContext.deviceManager = DefaultDeviceManager()
        chatContext.userDataStorage = userDataStorage

        secureChat = SecureChat(chatContext)
        secureChat?.rotateKeys(10)
    }

    companion object {
        private val VIRGIL_ACCESS_TOKEN = "AT.dfe131ba7b75056f0f59e3ab8cb08386a509854fd1acbd5d03c413648aaf40f0"
        private val VIRGIL_APP_PUBLIC_KEY = "MCowBQYDK2VwAyEAU1rir/Eb7W8tnRqbO4DqSjRbJWoSQ4l0isKyoi6KS8o="
        private val VIRGIL_AUTH_PUBLIC_KEY = "MCowBQYDK2VwAyEA2aEygm7kH5aUMTN9mi3ggTIwaw0H4Q7qje6g8znXSIg="
        private val AUTH_SERVER_URL = "https://auth-nexmo.virgilsecurity.com/"

        var instance: VirgilFacade = VirgilFacade()
            private set
    }

}