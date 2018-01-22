# End-to-End Encrypted Nexmo In-App Messaging Demo

## Prerequisites

* Java 7+
* [Android Studio](https://developer.android.com/studio/index.html)

## Register new user

First of all, you should generate a Private Key and create Virgil Card based on it.

```kotlin
// Generate private key
val virgilKey = virgilApi.keys.generate()

// Create Virgil Card
val customFields = HashMap<String, String>()
customFields.put("deviceId", Settings.Secure.ANDROID_ID)
val virgilCard = virgilApi.getCards().create(userName, virgilKey,
    "name", customFields)
```

You need Application Private Key to create Virgil Card. Since you shouldn't store Application Key on mobile devices, Virgil Card should be created on the server side.
Demo Application creates Virgil Card during registration.

```kotlin
val csr = CSR(virgilCard.export())
val response = NexmoApp.instance.serverClient.signup(csr).execute()
var registrationData = response.body()!!
```

registrationData also contains JWT which should be used to login Nexmo with `ConversationClient`.

Your mobile App is the only place where your Private Key is stored. So, you should store Private Key for future use. If you lose your Private Key, you won't be able to decrypt messages sent to you.

```kotlin
NexmoApp.instance.db.userDao().insert(User(registrationData.user.id,
    userName, registrationData.user.href, createdVirgilCard.id, 
    registrationData.user.virgilCard, virgilKey.privateKey.value))
```

Initialize `SecureChat` and generate one-time keys for future use.

```kotlin
crypto = VirgilCrypto()

keyStorage = JsonFileKeyStorage(
    context.getFilesDir().getAbsolutePath(), userName + ".ks")
userDataStorage = JsonFileUserDataStorage(
    context.getFilesDir().getAbsolutePath(), userName + ".ds")

// Configure PFS
var chatContext = SecureChatContext(virgilCard, privateKey,
    crypto, VIRGIL_ACCESS_TOKEN)
chatContext.keyStorage = keyStorage
chatContext.deviceManager = DefaultDeviceManager()
chatContext.userDataStorage = userDataStorage

secureChat = SecureChat(chatContext)
secureChat?.rotateKeys(10)
```

## Login

Obtain Virgil authentication token from the server. See the flow details by the [link](https://github.com/VirgilSecurity/virgil-services-auth).

```kotlin
// Get challenge message
val challengeMessage = this.authClient.getChallengeMessage(cardId)

// Decode encrypted message
val decodedMessage = this.crypto.decrypt(
    ConvertionUtils.base64ToBytes(challengeMessage.encryptedMessage),
    this.privateKey)

// Encrypt decoded message with application public key
val appPublicKey = this.crypto.importPublicKey(
    ConvertionUtils.base64ToBytes(VIRGIL_AUTH_PUBLIC_KEY))

val newEncryptedMessage =
    this.crypto.encrypt(decodedMessage, appPublicKey)

val message = ConvertionUtils.toBase64String(newEncryptedMessage)

// Send acknowledge to auth server
val code = this.authClient.acknowledge(
    challengeMessage.authorizationGrantId, message)

// Obtain access token
val accessTokenResponse = this.authClient.obtainAccessToken(code)
val virgilToken = accessTokenResponse.accessToken
```

Login Nexmo with `ConversationClient`.

```kotlin
val response = NexmoApp.instance.serverClient
    .jwt("Bearer ${virgilToken}").execute()
    
val jwt = response.body()!!.jwt
```

## Load users

Load the list of registered users.

```kotlin
val virgilToken = VirgilFacade.instance.getVirgilToken()
val response = NexmoApp.instance.serverClient
    .getUsers("Bearer ${virgilToken}").execute()
var users = response.body()
```

Start conversation.

```kotlin
conversationClient.newConversation(true, userName,
    object : RequestHandler<Conversation> {

    override fun onError(apiError: NexmoAPIError?) {
        closeWithError("Conversation is not created", apiError)
    }

    override fun onSuccess(result: Conversation?) {
        Log.d(TAG, "Created conversation ${result?.conversationId} for user ${userName}")
        mConversation = result
        mConversation?.invite(userName, object : RequestHandler<Member> {
            override fun onError(apiError: NexmoAPIError?) {
                closeWithError("Can't invite user ${userName} into conversation", apiError)
            }

            override fun onSuccess(result: Member?) {
                Log.d(TAG, "User ${result?.name} invited into conversation")
                mMemberCard = VirgilFacade.instance.virgilApi.cards.find(result?.name).firstOrNull()?.model
                
                // initizlize conversation
                ...
            }
        })
    }
})
```

You need a Virgil Card of the user you are starting conversation with.

```kotlin
val userName = NexmoUtils.getConversationPartner(mConversation!!)?.name
mMemberCard = VirgilFacade.instance.virgilApi.cards.find(userName).firstOrNull()?.model
```

Now you can send and receive messages.

### Sending message

#### Encrypting message

```kotlin
// Get active session
var secureSession = secureChat!!.activeSession(recipientCard.getId());

// If no session, start a new one
if (secureSession == null) {
    secureSession = secureChat!!.startNewSession(recipientCard, null);
}

// Encrypt message text
val encryptedText = secureSession.encrypt(text);
```

#### Send a message to the conversation

You can't decrypt the message which was encrypted by you. Thus, you should store the original message text locally. Use an encrypted text hash code to identify outcome message.

```kotlin
mConversation?.sendText(encryptedMessage,
    object : RequestHandler<Event> {

    override fun onSuccess(result: Event?) {
        // Save message in database
        val hash = encryptedMessage.hashCode().toString()
        val msg = Message(hash, mConversation!!.conversationId, result!!.member.userId, text)
        messageDao.insert(msg)
    }

    override fun onError(apiError: NexmoAPIError?) {
        Log.e(TAG, "Send message error", apiError)
    }
})
```

### Receiving message

#### Decrypting message

Let's identify the message sender first.

```kotlin
if (conversationClient.user.userId.equals(textMessage.member.userId)) {
    // This message was sent by myself. Find in database
    ....
} else {
   // Message from another conversation member
   ...
}
```

If a message is sent by you, just get it from the database by encrypted text hash code.

```kotlin
val hash = textMessage.text.hashCode().toString()
val message = messageDao.getMessage(mConversation!!.conversationId, hash)
val decryptedText = message.text
```

If a message is sent by another member, let's decrypt it with Virgil PFS.

```kotlin
// Loadup user session
var secureSession = secureChat!!.loadUpSession(senderCard, encryptedMessage, null)
val decryptedText = secureSession.decrypt(encryptedMessage)
```
