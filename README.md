# End-to-End Encrypted Nexmo Android In-App Messaging App Demo

This readme walks you through the steps to bring the E2EE Nexmo In-App Messaging Android app to life. It also attempts to explain the key changes to the original [Nexmo code](https://github.com/Nexmo/messaging-demo-android) on GitHub.

## What is End-to-End Encryption?

First, let’s start with a quick refresher of what E2EE (End-to-End Encryption) is and how it works. E2EE is simple: when you type in a chat message, it gets encrypted on your mobile device (or in your browser) and gets decrypted only when your chat partner receives it and wants to display it in chat window.

![Virgil Chat](https://github.com/VirgilSecurity/chat-back4app-android/blob/master/img/chat_example.png)
*Note: image needs to be updated, it's directly referred from Back4App project*

The message remains encrypted while it travels over Wi-Fi and the Internet, through the cloud / web server, into a database, and on the way back to your chat partner. In other words, none of the networks or servers have a clue of what the two of you are chatting about.

![Virgil Chat Server](https://github.com/VirgilSecurity/chat-back4app-android/blob/master/img/chat_example_server.png)
*Note: image needs to be updated, it's directly referred from Back4App project*

What’s difficult in End-to-End Encryption is the task of managing the encryption keys in a way that only the users involved in the chat can access them and nobody else. And when I write “nobody else”, I really mean it: even insiders of your cloud provider or even you, the developer, are out; [no accidental mistakes][_mistakes] or legally enforced peeking are possible. Writing crypto, especially for multiple platforms is hard: generating true random numbers, picking the right algorithms, and choosing the right encryption modes are just a few examples that make most developers wave their hands in the air and end up just NOT doing it.

Virgil's End-to-End Encryption tech enables Nexmo developers to ignore all these annoying details and quickly and simply End-to-End Encrypt their users' In-App chat messages.

**For an intro, this is how we’ll upgrade the Nexmo Android app to be End-to-End Encrypted:**
1. During sign-up: we’ll generate the individual private & public keys for new users (remember: the recipient's public key encrypts messages and the matching recipient's private key decrypts them).
1. Before sending messages, we’ll encrypt chat messages with the recipient's ever-changing public keys. Virgil's Perfect Forward Secrecy is the technology behind revolving encryption keys for every message: to make sure that future conversations are not compromised with a key that's accidentally leaked.
1. After receiving messages, we’ll decrypt chat messages with the recipient's ever-changing private keys.

![Virgil E2EE](https://github.com/VirgilSecurity/chat-back4app-android/blob/master/img/virgil_main.png)
*Note: image needs to be updated, it's directly referred from Back4App project*

We’ll publish the users’ public keys to Virgil’s Cards Service so that chat users are able to look up each other and able to encrypt messages for each other.  The private keys will stay on the user devices.

**OK, enough talking: let’s start doing!**

- We’ll start by guiding you through the Android app’s setup,
- Then, we’ll make you add the E2EE code and explain what each code block does.

# Let's get set up!

## Prerequisites

* Java 7+
* [Android Studio](https://developer.android.com/studio/index.html)
* [Application API server](https://github.com/VirgilSecurity/demo-nexmo-server)

## Sign up for Nexmo & Virgil accounts

- Sign up for your Nexmo account
- Any other steps here?
- Sign up for a [Virgil Security account][_virgil_account]
- Create a new app & token

## Install Application API server

Application API server is already installed and available by the [link](https://auth-nexmo.virgilsecurity.com/)

## Import Project in Android Studio:
  - File -> New -> Project from Version Control -> Git
  - Git Repository URL: https://github.com/VirgilSecurity/demo-nexmo-android
  - Check out the “master” branch

### Configure mobile application

Open VirgilFacade class and define constants from the table below

| Constant name | Description |
| --- | --- |
| VIRGIL_ACCESS_TOKEN | Your's Virgil Application access token. You should generate this token on the [dashboard](https://developer.virgilsecurity.com/account/dashboard/) or use the existing one |
| VIRGIL_APP_PUBLIC_KEY | Your's Virgil Application public key as Base64-encoded string |
| VIRGIL_AUTH_PUBLIC_KEY | Virgil Authentication server public key as Base64-encoded string |
| AUTH_SERVER_URL | Application API server URL |

# Code overview

## Register new users

Two important terms here:

  - **Virgil Key** – this is what we call a user's private key. Remember, private keys can decrypt data that was encrypted using the matching public key.
  - **Virgil Card** – Virgil Сards carry the user’s public key. Virgil cards are published to Virgil’s Cards Service (imagine this service is like a telephone book) for other users to retrieve them: Alice needs to retrieve Bob’s public key in order to encrypt a message for Bob using that key. 

In the E2EE version of the In-App Messaging app, we'll generate a Private Key for every user at signup time. We'll then generate the user's public key and publish it in a form of a new Virgil Card for the user, so that other users can find it and encrypt messages for us.

```kotlin
// Generate private key
val virgilKey = virgilApi.keys.generate()

// Create Virgil Card
val customFields = HashMap<String, String>()
customFields.put("deviceId", Settings.Secure.ANDROID_ID)
val virgilCard = virgilApi.getCards().create(userName, virgilKey,
    "name", customFields)
```

To create a Virgil Card, you'll need your Virgil Application's Private Key (otherwise, anybody can publish cards for your app without your control). Since you shouldn't store this key on mobile devices, we'll keep it in your web app and make your web app verify the users before card creation.

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

Upon login, we obtain a Virgil authentication token from the server. See the flow details by the [link](https://github.com/VirgilSecurity/virgil-services-auth).

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

Let's load the list of registered users.

```kotlin
val virgilToken = VirgilFacade.instance.getVirgilToken()
val response = NexmoApp.instance.serverClient
    .getUsers("Bearer ${virgilToken}").execute()
var users = response.body()
```

Start a conversation.

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

You'll need a Virgil Card of the user you are starting conversation with.

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

You can't decrypt message that you encrypted. Therefore, you should store the original message locally. To ensure that the message isn't tampered, create a hash code from the encrypted text.

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

If it's your own message, just get it from the database by the encrypted text hash code.

```kotlin
val hash = textMessage.text.hashCode().toString()
val message = messageDao.getMessage(mConversation!!.conversationId, hash)
val decryptedText = message.text
```

If the message is sent by somebody else, let's decrypt it.

```kotlin
// Loadup user session
var secureSession = secureChat!!.loadUpSession(senderCard, encryptedMessage, null)
val decryptedText = secureSession.decrypt(encryptedMessage)
```
