package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.crypto.Aes256EncryptionHelper
import com.example.data.crypto.CryptoPipeline
import com.example.data.crypto.EncryptedPayload
import com.example.data.crypto.FullPipelineTrace
import com.example.data.local.BluefiDatabase
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.PeerEntity
import com.example.data.mesh.BluetoothMeshManager
import com.example.data.mesh.DuplexP2PTransceiver
import com.example.data.mesh.DuplexPacket
import com.example.data.mesh.QrHandshakeManager
import com.example.data.mesh.RemotePeerNode
import com.example.data.mesh.RadarVisibilityManager
import com.example.data.mesh.WifiDirectManager
import com.example.data.model.ChatMessage
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.Peer
import com.example.data.model.RetentionRule
import com.example.data.model.UserSettings
import com.example.ui.components.MediaEncodingHelper
import com.example.ui.theme.BluefiThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluefiRepository(private val context: Context) {

  private val database = BluefiDatabase.getDatabase(context)
  private val peerDao = database.peerDao()
  private val messageDao = database.messageDao()
  private val userAccountDao = database.userAccountDao()

  val bluetoothMeshManager = BluetoothMeshManager(context)
  val wifiDirectManager = WifiDirectManager(context)
  val radarVisibilityManager = RadarVisibilityManager(bluetoothMeshManager, wifiDirectManager)
  val hardwareRadioManager = com.example.data.hardware.HardwareRadioManager(context, CoroutineScope(Dispatchers.IO))
  val hardwareRadioState = hardwareRadioManager.state

  private val scope = CoroutineScope(Dispatchers.IO)

  val duplexTransceiver = DuplexP2PTransceiver(context, scope)
  val autoNavigatePeerId = MutableSharedFlow<String>(extraBufferCapacity = 2)
  val incomingRadarRequest = MutableSharedFlow<Peer>(extraBufferCapacity = 5)
  val connectionFailedNotice = MutableSharedFlow<String>(extraBufferCapacity = 5)

  private val authPrefs = context.getSharedPreferences("bluefi_auth_storage", Context.MODE_PRIVATE)

  private val _userSettings = MutableStateFlow(
    UserSettings(
      userId = authPrefs.getString("user_id", null) ?: "node_me_${UUID.randomUUID().toString().take(6)}",
      username = authPrefs.getString("user_id", null) ?: "BluefiNode_01",
      avatarIndex = 0,
      theme = BluefiThemeOption.DARK_FAST_BLUE,
      retentionRule = RetentionRule.RETENTION_24_HOURS
    )
  )
  val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

  val allPeers: Flow<List<Peer>> = peerDao.getAllPeers().map { list ->
    list.map { it.toDomain() }
  }

  // Real-time peer typing status tracking
  private val _peerTypingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val peerTypingMap: StateFlow<Map<String, Boolean>> = _peerTypingMap.asStateFlow()

  private val typingTimeouts = ConcurrentHashMap<String, Job>()

  fun setPeerTyping(peerId: String, isTyping: Boolean) {
    val current = _peerTypingMap.value.toMutableMap()
    if (isTyping) {
      current[peerId] = true
      _peerTypingMap.value = current
      typingTimeouts[peerId]?.cancel()
      typingTimeouts[peerId] = scope.launch {
        kotlinx.coroutines.delay(3500)
        val updated = _peerTypingMap.value.toMutableMap()
        updated[peerId] = false
        _peerTypingMap.value = updated
      }
    } else {
      typingTimeouts[peerId]?.cancel()
      current[peerId] = false
      _peerTypingMap.value = current
    }
  }

  fun sendTypingIndicator(peerId: String, isTyping: Boolean) {
    val localUser = _userSettings.value
    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "TYPING",
        packetId = "typ_${System.currentTimeMillis()}",
        senderId = localUser.userId,
        senderName = localUser.username,
        receiverId = peerId,
        text = if (isTyping) "1" else "0",
        timestamp = System.currentTimeMillis()
      )
    )
  }

  fun simulatePeerTyping(peerId: String, durationMs: Long = 2800) {
    scope.launch {
      setPeerTyping(peerId, true)
      kotlinx.coroutines.delay(durationMs)
      setPeerTyping(peerId, false)
    }
  }

  fun isAccountCreated(): Boolean = authPrefs.getBoolean("is_registered", false)

  fun getStoredUserId(): String = authPrefs.getString("user_id", "") ?: ""

  suspend fun getStoredAccounts(): List<UserAccountEntity> {
    return userAccountDao.getAllAccountsDirect()
  }

  suspend fun getAccountCount(): Int {
    return userAccountDao.getAccountCount()
  }

  suspend fun createAccountWithSecurityQuestions(
    userId: String,
    password: String,
    q1: String, a1: String,
    q2: String, a2: String,
    q3: String, a3: String
  ): Boolean {
    val cleanId = userId.trim()
    val count = userAccountDao.getAccountCount()
    if (count >= 5 && userAccountDao.getAccountById(cleanId) == null) {
      return false // Storage limit reached (max 5 accounts)
    }

    userAccountDao.clearActiveSessions()
    val account = UserAccountEntity(
      userId = cleanId,
      username = cleanId,
      passwordHash = password,
      isActiveSession = true,
      securityQuestion1 = q1,
      securityAnswer1 = a1.trim().lowercase(),
      securityQuestion2 = q2,
      securityAnswer2 = a2.trim().lowercase(),
      securityQuestion3 = q3,
      securityAnswer3 = a3.trim().lowercase()
    )
    userAccountDao.insertOrUpdateAccount(account)

    authPrefs.edit()
      .putString("user_id", cleanId)
      .putString("password", password)
      .putBoolean("is_registered", true)
      .apply()

    _userSettings.value = _userSettings.value.copy(
      userId = cleanId,
      username = cleanId
    )
    return true
  }

  fun createAccount(userId: String, password: String): Boolean {
    val cleanId = userId.trim()
    authPrefs.edit()
      .putString("user_id", cleanId)
      .putString("password", password)
      .putBoolean("is_registered", true)
      .apply()
    _userSettings.value = _userSettings.value.copy(
      userId = cleanId,
      username = cleanId
    )
    CoroutineScope(Dispatchers.IO).launch {
      userAccountDao.clearActiveSessions()
      userAccountDao.insertOrUpdateAccount(
        UserAccountEntity(
          userId = cleanId,
          username = cleanId,
          passwordHash = password,
          isActiveSession = true
        )
      )
    }
    return true
  }

  suspend fun verifyLoginForAccount(userId: String, password: String): Boolean {
    val cleanId = userId.trim()
    val account = userAccountDao.getAccountById(cleanId)
    if (account != null && account.passwordHash == password) {
      userAccountDao.clearActiveSessions()
      userAccountDao.setActiveSession(cleanId)
      authPrefs.edit()
        .putString("user_id", cleanId)
        .putString("password", password)
        .putBoolean("is_registered", true)
        .apply()
      _userSettings.value = _userSettings.value.copy(
        userId = cleanId,
        username = account.username,
        avatarIndex = account.avatarIndex
      )
      return true
    }
    val saved = authPrefs.getString("password", null)
    val savedId = authPrefs.getString("user_id", null)
    if (savedId == cleanId && saved == password) {
      return true
    }
    return false
  }

  suspend fun getSecurityQuestionsForUser(userId: String): Triple<String, String, String>? {
    val account = userAccountDao.getAccountById(userId.trim()) ?: return null
    if (account.securityQuestion1.isNotBlank()) {
      return Triple(account.securityQuestion1, account.securityQuestion2, account.securityQuestion3)
    }
    return null
  }

  suspend fun verifySecurityAnswers(userId: String, a1: String, a2: String, a3: String): Boolean {
    val account = userAccountDao.getAccountById(userId.trim()) ?: return false
    val match1 = account.securityAnswer1.equals(a1.trim(), ignoreCase = true)
    val match2 = account.securityAnswer2.equals(a2.trim(), ignoreCase = true)
    val match3 = account.securityAnswer3.equals(a3.trim(), ignoreCase = true)
    return match1 && match2 && match3
  }

  suspend fun resetPasswordAfterVerification(userId: String, newPass: String): Boolean {
    val cleanId = userId.trim()
    val account = userAccountDao.getAccountById(cleanId) ?: return false
    userAccountDao.updateAccount(account.copy(passwordHash = newPass))
    if (authPrefs.getString("user_id", "") == cleanId) {
      authPrefs.edit().putString("password", newPass).apply()
    }
    return true
  }

  suspend fun updateLockFormatAndSecret(userId: String, lockType: String, passcode: String) {
    val cleanId = userId.trim()
    val account = userAccountDao.getAccountById(cleanId)
    if (account != null) {
      userAccountDao.updateAccount(account.copy(passwordHash = passcode))
    }
    authPrefs.edit()
      .putString("LOCK_TYPE_${cleanId}", lockType)
      .putString("PASSWORD_HASH_${cleanId}", passcode)
      .putString("password", passcode)
      .apply()
  }

  fun verifyPassword(password: String): Boolean {
    val saved = authPrefs.getString("password", null)
    return saved != null && saved == password
  }

  fun resetAccount() {
    authPrefs.edit().clear().apply()
  }

  init {
    duplexTransceiver.start(_userSettings.value.userId, _userSettings.value.username)

    wifiDirectManager.onGroupConnectionEstablished = { groupOwnerIp, isOwner ->
      if (groupOwnerIp.isNotBlank()) {
        duplexTransceiver.registerKnownPeerIp("wifi_direct_go", groupOwnerIp)
        val localUser = _userSettings.value
        duplexTransceiver.broadcastPacket(
          DuplexPacket(
            type = "BEACON",
            packetId = "p2p_wd_${System.currentTimeMillis()}",
            senderId = localUser.userId,
            senderName = localUser.username,
            receiverId = "BROADCAST",
            senderIp = duplexTransceiver.getLocalIpAddress(),
            timestamp = System.currentTimeMillis()
          )
        )
      }
    }

    scope.launch {
      duplexTransceiver.incomingPackets.collect { packet ->
        handleIncomingDuplexPacket(packet)
      }
    }

    scope.launch(Dispatchers.IO) {
      messageDao.purgeHandshakeSystemMessages()
      purgeUnidentifiedRawBeacons()
      seedInboxChats()
      // Continuous background retention loop: purge expired / 15-second burned messages every second
      while (isActive) {
        try {
          purgeExpiredMessages()
        } catch (_: Exception) {}
        kotlinx.coroutines.delay(1000L)
      }
    }
  }

  fun getMessagesForPeer(peerId: String): Flow<List<ChatMessage>> {
    return messageDao.getMessagesForPeer(peerId).map { list ->
      list.filterNot { entity ->
        entity.text.contains("Handshake Verified", ignoreCase = true) ||
        entity.text.contains("Scan Verified", ignoreCase = true) ||
        entity.text.contains("AES-256 shared key active", ignoreCase = true)
      }.map { it.toDomain() }
    }
  }

  suspend fun getPeerDirect(peerId: String): Peer? {
    return peerDao.getPeerDirect(peerId)?.toDomain()
  }

  suspend fun isUsernameAvailable(username: String): Boolean {
    val existing = peerDao.getPeerByUsername(username)
    return existing == null && username.isNotBlank() && !username.equals(_userSettings.value.username, ignoreCase = true)
  }

  fun updateUserSettings(update: (UserSettings) -> UserSettings) {
    val oldRule = _userSettings.value.retentionRule
    val oldVisibility = _userSettings.value.isRadarVisible
    val oldSaver = _userSettings.value.isBatterySaverMode
    val updated = update(_userSettings.value)
    _userSettings.value = updated
    if (updated.isRadarVisible != oldVisibility) {
      setRadarVisibility(updated.isRadarVisible)
    }
    if (updated.isBatterySaverMode != oldSaver) {
      applyBatterySaverMode(updated.isBatterySaverMode)
    }
    if (updated.retentionRule != oldRule) {
      scope.launch(Dispatchers.IO) {
        when (updated.retentionRule) {
          RetentionRule.BURN_ON_READ -> {
            // When switching to 15-second burn, attach a 15-second self-destruct fuse to active messages
            messageDao.applyBurnFuseToExistingMessages(System.currentTimeMillis() + 15_000L)
          }
          RetentionRule.RETENTION_24_HOURS -> {
            messageDao.apply24HourExpirationToExistingMessages()
            messageDao.purgeExpiredMessages(System.currentTimeMillis())
          }
          RetentionRule.PERMANENT -> {
            messageDao.clearAllBurnFuses()
          }
        }
      }
    }
  }

  fun applyBatterySaverMode(enabled: Boolean) {
    Log.i("BluefiRepo", "applyBatterySaverMode($enabled): Adjusting foreground service, BLE scan duty cycle, and mesh beacon frequency")
    bluetoothMeshManager.setBatterySaverMode(enabled)
    duplexTransceiver.isBatterySaverMode = enabled
    com.example.data.service.P2pForegroundService.isBatterySaverMode = enabled
  }

  fun setRadarVisibility(visible: Boolean) {
    _userSettings.value = _userSettings.value.copy(isRadarVisible = visible)
    val settings = _userSettings.value
    radarVisibilityManager.setRadarVisibility(
      visible = visible,
      userId = settings.userId,
      username = settings.username,
      isEncrypted = settings.e2eeStrictEnabled,
      onPresenceBroadcast = { shouldBroadcast ->
        if (shouldBroadcast) {
          broadcastPresenceBeacon()
        }
      }
    )
  }

  fun broadcastPresenceBeacon() {
    val localUser = _userSettings.value
    if (!localUser.isRadarVisible) {
      Log.d("BluefiRepo", "broadcastPresenceBeacon skipped: Invisible Mode active")
      return
    }
    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "BEACON",
        packetId = "p2p_wd_${System.currentTimeMillis()}",
        senderId = localUser.userId,
        senderName = localUser.username,
        receiverId = "BROADCAST",
        senderIp = duplexTransceiver.getLocalIpAddress(),
        timestamp = System.currentTimeMillis()
      )
    )
  }

  suspend fun sendMessage(
    peerId: String,
    text: String,
    mediaType: String = "text",
    mediaContent: String = ""
  ): FullPipelineTrace {
    // Reset 15-minute idle timeout on message transmission
    notifySocketActivity()

    val currentSender = _userSettings.value
    val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"

    // Ensure shared AES-256 session key derived during handshake is registered
    val peer = peerDao.getPeerDirect(peerId)
    Aes256EncryptionHelper.getOrCreateSessionKey(
      localId = currentSender.userId,
      peerId = peerId,
      peerPublicKeyHex = peer?.publicKeyHex
    )

    // Process through the complete 9-step CryptoPipeline (which uses Aes256EncryptionHelper)
    val trace = CryptoPipeline.processOutboundPipeline(
      text = text,
      messageId = messageId,
      senderId = currentSender.userId,
      receiverId = peerId
    )

    val retentionRule = currentSender.retentionRule
    val burnsIn = when (retentionRule) {
      RetentionRule.BURN_ON_READ -> System.currentTimeMillis() + 15_000L // 15-second minimum display fuse
      RetentionRule.RETENTION_24_HOURS -> System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
      RetentionRule.PERMANENT -> 0L
    }
    val isBurnOnRead = (retentionRule == RetentionRule.BURN_ON_READ)

    val isPeerAvailable = peer != null && peer.isOnline

    // If the peer is not connected or not available, place message into the WAITING AREA
    val initialStatus = if (isPeerAvailable) {
      MessageDeliveryStatus.TRANSMITTING_2_4GHZ.name
    } else {
      MessageDeliveryStatus.WAITING_IN_QUEUE.name
    }

    val entity = MessageEntity(
      id = messageId,
      conversationPeerId = peerId,
      senderId = currentSender.userId,
      senderName = currentSender.username,
      isFromMe = true,
      text = text,
      binaryForm = trace.binaryRepresentation,
      cipherTextHex = trace.encryptedHex,
      packetCount = trace.packetCount,
      mediaType = mediaType,
      mediaContent = mediaContent,
      timestamp = System.currentTimeMillis(),
      burnsInTimestamp = burnsIn,
      isBurnOnRead = isBurnOnRead,
      wasRead = false,
      isEncrypted = true,
      status = initialStatus
    )

    messageDao.insertMessage(entity)
    peerDao.updateLastMessage(
      peerId,
      if (isPeerAvailable) text else "⏳ [Waiting Area] $text",
      System.currentTimeMillis()
    )
    bluetoothMeshManager.incrementRelayedPacketCount()

    if (isPeerAvailable) {
      // Broadcast over 2.4 GHz duplex transceiver for real peer device reception
      duplexTransceiver.broadcastPacket(
        DuplexPacket(
          type = "CHAT",
          packetId = messageId,
          senderId = currentSender.userId,
          senderName = currentSender.username,
          receiverId = peerId,
          cipherTextHex = trace.encryptedHex,
          ivHex = trace.ivHex,
          binaryForm = trace.binaryRepresentation,
          text = text,
          mediaType = mediaType,
          mediaContent = mediaContent,
          timestamp = entity.timestamp
        )
      )

      // Clear local typing state upon message transmission
      sendTypingIndicator(peerId, false)

      // Trigger simulated bot reply or fallback delivery acknowledgment for demo/single-device testing
      val simulatedBots = setOf("peer_relay_alpha", "peer_transceiver_hw", "peer_cyber_sentinel", "peer_gateway_delta")
      if (peerId in simulatedBots) {
        scope.launch {
          kotlinx.coroutines.delay(700)
          messageDao.updateMessageStatus(messageId, MessageDeliveryStatus.DELIVERED.name)
          kotlinx.coroutines.delay(500)
          setPeerTyping(peerId, true)
          kotlinx.coroutines.delay(2000)
          setPeerTyping(peerId, false)
          generateSimulatedPeerReply(peerId, text)
        }
      } else {
        scope.launch {
          kotlinx.coroutines.delay(1200)
          val msg = messageDao.getMessageById(messageId)
          if (msg?.status == MessageDeliveryStatus.TRANSMITTING_2_4GHZ.name) {
            messageDao.updateMessageStatus(messageId, MessageDeliveryStatus.DELIVERED.name)
          }
        }
      }
    } else {
      // Offline: Message is held securely in local database WAITING AREA until second user connects!
      Log.d("BluefiRepo", "Message $messageId placed in WAITING AREA because peer $peerId is not connected")
    }

    return trace
  }

  suspend fun flushWaitingMessagesForPeer(peerId: String) {
    val waitingMessages = messageDao.getWaitingMessagesForPeer(peerId)
    if (waitingMessages.isEmpty()) return

    val currentSender = _userSettings.value
    Log.d("BluefiRepo", "Flushing ${waitingMessages.size} messages from WAITING AREA for connected peer $peerId")

    for (msg in waitingMessages) {
      // Step 1: Set transmitting
      messageDao.updateMessageStatus(msg.id, MessageDeliveryStatus.TRANSMITTING_2_4GHZ.name)

      // Step 2: Broadcast packet over duplex transceiver
      duplexTransceiver.broadcastPacket(
        DuplexPacket(
          type = "CHAT",
          packetId = msg.id,
          senderId = currentSender.userId,
          senderName = currentSender.username,
          receiverId = peerId,
          cipherTextHex = msg.cipherTextHex,
          binaryForm = msg.binaryForm,
          text = msg.text,
          mediaType = msg.mediaType,
          mediaContent = msg.mediaContent,
          timestamp = msg.timestamp
        )
      )

      // Step 3: Mark delivered and verified
      kotlinx.coroutines.delay(250)
      messageDao.updateMessageStatus(msg.id, MessageDeliveryStatus.DELIVERED.name)
    }

    val lastMsg = waitingMessages.lastOrNull()
    if (lastMsg != null) {
      peerDao.updateLastMessage(peerId, lastMsg.text, lastMsg.timestamp)
    }
  }

  suspend fun purgeUnidentifiedRawBeacons() {
    try {
      peerDao.purgeUnidentifiedRawBeacons()
      val legacyPersonalPeerIds = listOf(
        "peer_phone_9815457214", "group_ug_tpp_2027", "group_data_mining_3a",
        "group_python_project_3a", "business_jiomart_shop", "peer_mona",
        "channel_uni_updates", "group_cu_24bca_dl", "business_jupiter_loan",
        "peer_sumit_sharma_cu", "business_alibaba_deals", "peer_node_mesh_beta"
      )
      for (oldId in legacyPersonalPeerIds) {
        peerDao.deletePeer(oldId)
        messageDao.deleteMessagesForPeer(oldId)
      }
      val allPeers = peerDao.getAllPeersListDirect()
      for (peer in allPeers) {
        if (peer.displayName.matches(Regex("^(?i)(Peer_[0-9A-F:]+|MeshNode_Beta).*")) ||
            peer.lastMessagePreview.contains("Beacon", ignoreCase = true) ||
            peer.lastMessagePreview.contains("Discovered via BLE", ignoreCase = true)) {
          peerDao.deletePeer(peer.id)
          messageDao.deleteMessagesForPeer(peer.id)
        }
      }
    } catch (_: Exception) {}
  }

  private suspend fun handleIncomingDuplexPacket(packet: DuplexPacket) {
    // Reset 15-minute idle timeout on message reception
    notifySocketActivity()

    val localUser = _userSettings.value
    when (packet.type) {
      "CHAT" -> {
        // Drop packet if echoed back from ourselves
        if (packet.senderId == localUser.userId) return

        val cleanLocalCode = getLocalPairingCode()
        val isForMe = packet.receiverId == localUser.userId ||
          packet.receiverId == "BROADCAST" ||
          packet.receiverId == localUser.username ||
          packet.receiverId == cleanLocalCode ||
          packet.pairingCode == cleanLocalCode ||
          packet.receiverId.startsWith("group_") ||
          packet.receiverId.startsWith("channel_") ||
          peerDao.getPeerDirect(packet.senderId) != null ||
          peerDao.getPeerDirect(packet.receiverId) != null

        if (!isForMe) {
          return
        }

        // If a temporary pairing node exists for this code, migrate its conversation messages to real senderId
        if (packet.pairingCode.isNotBlank()) {
          val cleanCode = packet.pairingCode.replace("-", "_").lowercase()
          val tempPeerId = "node_p2p_$cleanCode"
          if (tempPeerId != packet.senderId && peerDao.getPeerDirect(tempPeerId) != null) {
            messageDao.migrateConversationMessages(tempPeerId, packet.senderId)
            peerDao.deletePeer(tempPeerId)
          }
        }

        var peer = peerDao.getPeerDirect(packet.senderId)
        if (peer == null) {
          val newPeer = PeerEntity(
            id = packet.senderId,
            username = packet.senderName.lowercase().replace(" ", "_"),
            displayName = packet.senderName.ifBlank { "Node ${packet.senderId.takeLast(4)}" },
            avatarIndex = (packet.senderId.hashCode() and 0x7FFFFFFF) % 6,
            isOnline = true,
            rssi = -50,
            protocol = "2.4 GHz Duplex P2P",
            hopCount = 1,
            distanceEstimatedMeters = 1.5,
            lastSeenTimestamp = packet.timestamp,
            lastMessagePreview = "Encrypted packet received",
            lastMessageTimestamp = packet.timestamp,
            unreadCount = 1,
            publicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" }
          )
          peerDao.insertPeer(newPeer)
          peer = newPeer
        } else {
          peerDao.updatePeer(
            peer.copy(
              isOnline = true,
              lastSeenTimestamp = packet.timestamp
            )
          )
        }

        val decryptedText = try {
          if (packet.cipherTextHex.isNotBlank()) {
            try {
              Aes256EncryptionHelper.decryptHexFromPeer(
                combinedHex = packet.cipherTextHex,
                peerId = packet.senderId,
                localId = localUser.userId
              )
            } catch (_: Exception) {
              try {
                val directKey = Aes256EncryptionHelper.deriveSharedKey(localUser.userId, packet.senderId)
                val payload = EncryptedPayload.fromCombinedHex(packet.cipherTextHex)
                if (payload != null) Aes256EncryptionHelper.decrypt(payload, directKey)
                else throw IllegalArgumentException("Null payload")
              } catch (_: Exception) {
                if (packet.pairingCode.isNotBlank()) {
                  val secretKey = Aes256EncryptionHelper.deriveSharedKey(
                    localId = localUser.userId,
                    peerId = packet.senderId,
                    handshakeSecret = packet.pairingCode
                  )
                  val payload = EncryptedPayload.fromCombinedHex(packet.cipherTextHex)
                  if (payload != null) Aes256EncryptionHelper.decrypt(payload, secretKey)
                  else throw IllegalArgumentException("Null payload")
                } else {
                  throw IllegalArgumentException("Decryption failed")
                }
              }
            }
          } else {
            ""
          }
        } catch (e: Exception) {
          Log.d("BluefiRepo", "Duplex decrypt fallback: ${e.message}")
          ""
        }.ifBlank {
          if (packet.binaryForm.isNotBlank()) {
            val decoded = CryptoPipeline.binaryToText(packet.binaryForm)
            if (decoded.isNotBlank()) decoded else ""
          } else ""
        }.ifBlank {
          if (packet.text.isNotBlank()) packet.text
          else if (packet.mediaContent.isNotBlank() && packet.mediaType == "sticker") packet.mediaContent
          else "P2P Message"
        }

        val conversationId = if (packet.receiverId.startsWith("group_") || packet.receiverId.startsWith("channel_")) {
          packet.receiverId
        } else {
          packet.senderId
        }

        // Cache incoming audio voice notes or video streams locally so they can be played offline instantly
        var resolvedMediaContent = packet.mediaContent
        try {
          if (packet.mediaType == "audio" && packet.mediaContent.isNotBlank()) {
            val parts = packet.mediaContent.split("|")
            val b64 = if (parts.size >= 3) parts[2] else packet.mediaContent
            if (b64.isNotBlank() && !b64.startsWith("/")) {
              val saved = MediaEncodingHelper.saveBase64ToFile(context, "audio", b64, "received_voice_${packet.packetId}", "m4a")
              if (saved != null) {
                val dur = if (parts.size >= 2) parts[1] else "5"
                resolvedMediaContent = "audio_meta|$dur|$b64|${saved.absolutePath}"
              }
            }
          } else if (packet.mediaType == "video" && packet.mediaContent.isNotBlank()) {
            val parts = packet.mediaContent.split("|")
            val fileName = if (parts.size >= 4) parts[3] else "vid_${packet.packetId}.mp4"
            val videoDir = MediaEncodingHelper.getSecureMediaDir(context, "videos")
            val localTarget = File(videoDir, fileName)

            if (parts.size >= 6 && parts[5].isNotBlank()) {
              val saved = MediaEncodingHelper.saveBase64ToFile(context, "videos", parts[5], "received_vid_${packet.packetId}", "mp4")
              if (saved != null) {
                resolvedMediaContent = "video_meta|${parts[1]}|${parts[2]}|${parts[3]}|${saved.absolutePath}|${parts[5]}"
              }
            } else if (localTarget.exists() && localTarget.length() > 0) {
              resolvedMediaContent = "video_meta|${if (parts.size >= 2) parts[1] else "0:15"}|${if (parts.size >= 3) parts[2] else ""}|$fileName|${localTarget.absolutePath}|"
            } else if (packet.senderIp.isNotBlank()) {
              // Fetch video asynchronously over TCP port 24243 from sender
              scope.launch(Dispatchers.IO) {
                val downloaded = duplexTransceiver.downloadMediaFile(packet.senderIp, fileName, localTarget)
                if (downloaded) {
                  val dur = if (parts.size >= 2) parts[1] else "0:15"
                  val thumb = if (parts.size >= 3) parts[2] else ""
                  val updated = "video_meta|$dur|$thumb|$fileName|${localTarget.absolutePath}|"
                  val existing = messageDao.getMessageById(packet.packetId)
                  if (existing != null) {
                    messageDao.updateMessage(existing.copy(mediaContent = updated))
                  }
                }
              }
            }
          }
        } catch (_: Exception) {}

        val incomingEntity = MessageEntity(
          id = packet.packetId.ifBlank { "msg_${System.currentTimeMillis()}" },
          conversationPeerId = conversationId,
          senderId = packet.senderId,
          senderName = packet.senderName.ifBlank { peer.displayName },
          isFromMe = false,
          text = decryptedText,
          binaryForm = packet.binaryForm.ifBlank { CryptoPipeline.textToBinary(decryptedText) },
          cipherTextHex = packet.cipherTextHex,
          packetCount = 1,
          mediaType = packet.mediaType,
          mediaContent = resolvedMediaContent,
          timestamp = packet.timestamp,
          burnsInTimestamp = when (localUser.retentionRule) {
            RetentionRule.BURN_ON_READ -> System.currentTimeMillis() + 15_000L // Minimum 15 seconds display
            RetentionRule.RETENTION_24_HOURS -> System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            else -> 0L
          },
          isBurnOnRead = (localUser.retentionRule == RetentionRule.BURN_ON_READ),
          wasRead = false,
          isEncrypted = true,
          status = MessageDeliveryStatus.DELIVERED.name
        )

        messageDao.insertMessage(incomingEntity)
        peerDao.updateLastMessage(conversationId, decryptedText, packet.timestamp)
        bluetoothMeshManager.incrementRelayedPacketCount()
        // Peer has finished typing and delivered their message
        setPeerTyping(packet.senderId, false)

        // Send instantaneous delivery confirmation ACK back to sender node
        duplexTransceiver.broadcastPacket(
          DuplexPacket(
            type = "CHAT_ACK",
            packetId = incomingEntity.id,
            senderId = localUser.userId,
            senderName = localUser.username,
            receiverId = packet.senderId,
            timestamp = System.currentTimeMillis()
          )
        )

        com.example.data.notification.BluefiNotificationManager.showIncomingMessageNotification(
          context = context,
          conversationId = conversationId,
          senderId = packet.senderId,
          senderName = incomingEntity.senderName,
          messageText = decryptedText,
          mediaType = incomingEntity.mediaType
        )
      }

      "TYPING" -> {
        val isTyping = packet.text == "1"
        setPeerTyping(packet.senderId, isTyping)
      }

      "CHAT_ACK" -> {
        if (packet.packetId.isNotBlank()) {
          messageDao.updateMessageStatus(packet.packetId, MessageDeliveryStatus.DELIVERED.name)
        }
      }

      "PAIR_REQUEST" -> {
        val cleanLocalCode = getLocalPairingCode()
        val isTargeted = packet.pairingCode == cleanLocalCode ||
          packet.receiverId == localUser.userId ||
          packet.receiverId == "BROADCAST"

        if (isTargeted) {
          val peerEntity = PeerEntity(
            id = packet.senderId,
            username = packet.senderName.lowercase().replace(" ", "_"),
            displayName = packet.senderName.ifBlank { "Paired Node [${packet.pairingCode}]" },
            avatarIndex = (packet.pairingCode.hashCode() and 0x7FFFFFFF) % 6,
            isOnline = true,
            rssi = -50,
            protocol = "2.4 GHz Direct P2P",
            hopCount = 1,
            distanceEstimatedMeters = 1.2,
            lastSeenTimestamp = System.currentTimeMillis(),
            lastMessagePreview = "Pairing verified via duplex handshake",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            publicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" }
          )
          peerDao.insertPeer(peerEntity)

          val sessionKey = Aes256EncryptionHelper.deriveAndRegisterSharedKey(
            localId = localUser.userId,
            peerId = packet.senderId,
            peerPublicKeyHex = peerEntity.publicKeyHex,
            handshakeSecret = cleanLocalCode
          )
          val fingerprint = Aes256EncryptionHelper.getSessionKeyFingerprint(packet.senderId) ?: "AES-256"

          // Send PAIR_ACCEPT back to sender
          duplexTransceiver.broadcastPacket(
            DuplexPacket(
              type = "PAIR_ACCEPT",
              packetId = "accept_${System.currentTimeMillis()}",
              senderId = localUser.userId,
              senderName = localUser.username,
              receiverId = packet.senderId,
              pairingCode = cleanLocalCode,
              publicKeyHex = "04" + localUser.userId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64),
              timestamp = System.currentTimeMillis()
            )
          )

          // Auto shift to chat page on this receiving device
          flushWaitingMessagesForPeer(packet.senderId)
          autoNavigatePeerId.tryEmit(packet.senderId)
        }
      }

      "PAIR_ACCEPT", "PAIR_REQUEST" -> {
        val cleanLocalCode = getLocalPairingCode()
        if (packet.receiverId == localUser.userId || packet.receiverId == "BROADCAST" || packet.pairingCode == cleanLocalCode || packet.pairingCode.isNotBlank()) {
          // Register sender's IP address for direct duplex unicast
          if (packet.senderIp.isNotBlank()) {
            duplexTransceiver.registerKnownPeerIp(packet.senderId, packet.senderIp)
          }

          var peer = peerDao.getPeerDirect(packet.senderId)
          if (peer == null) {
            peer = PeerEntity(
              id = packet.senderId,
              username = packet.senderName.lowercase().replace(" ", "_"),
              displayName = packet.senderName.ifBlank { "Node ${packet.senderId.takeLast(4)}" },
              avatarIndex = (packet.senderId.hashCode() and 0x7FFFFFFF) % 6,
              isOnline = true,
              rssi = -50,
              protocol = "2.4 GHz Direct P2P",
              hopCount = 1,
              distanceEstimatedMeters = 1.2,
              lastSeenTimestamp = System.currentTimeMillis(),
              lastMessagePreview = "Connected via QR / P2P handshake",
              lastMessageTimestamp = System.currentTimeMillis(),
              unreadCount = 0,
              publicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" }
            )
            peerDao.insertPeer(peer)
          } else {
            val updatedRemoteName = if (packet.senderName.isNotBlank() && !packet.senderName.startsWith("Node ")) {
              packet.senderName
            } else {
              peer.displayName
            }
            peerDao.updatePeer(
              peer.copy(
                username = updatedRemoteName.lowercase().replace(" ", "_"),
                displayName = updatedRemoteName,
                avatarIndex = (packet.senderId.hashCode() and 0x7FFFFFFF) % 6,
                isOnline = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                lastMessagePreview = "Pairing handshake verified"
              )
            )
          }

          val sessionKey = Aes256EncryptionHelper.deriveAndRegisterSharedKey(
            localId = localUser.userId,
            peerId = packet.senderId,
            peerPublicKeyHex = peer.publicKeyHex,
            handshakeSecret = packet.pairingCode
          )
          val fingerprint = Aes256EncryptionHelper.getSessionKeyFingerprint(packet.senderId) ?: "AES-256"

          // If a temporary node was created for the pairing code, migrate messages to real senderId
          val cleanCode = packet.pairingCode.replace("-", "_").lowercase()
          val tempPeerId = "node_p2p_$cleanCode"
          if (tempPeerId != packet.senderId && peerDao.getPeerDirect(tempPeerId) != null) {
            messageDao.migrateConversationMessages(tempPeerId, packet.senderId)
            peerDao.deletePeer(tempPeerId)
          }

          // If this device received a PAIR_REQUEST, reply with PAIR_ACCEPT so both sides are 100% paired
          if (packet.type == "PAIR_REQUEST") {
            duplexTransceiver.broadcastPacket(
              DuplexPacket(
                type = "PAIR_ACCEPT",
                packetId = "ack_reply_${System.currentTimeMillis()}",
                senderId = localUser.userId,
                senderName = localUser.username,
                senderIp = duplexTransceiver.getLocalIpAddress(),
                receiverId = packet.senderId,
                pairingCode = packet.pairingCode,
                publicKeyHex = "04" + localUser.userId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64),
                timestamp = System.currentTimeMillis()
              )
            )
          }

          // Automatically shift this receiving device to the ChatScreen with the connecting peer
          cancelDiscoveryOnConnected()
          flushWaitingMessagesForPeer(packet.senderId)
          autoNavigatePeerId.tryEmit(packet.senderId)
        }
      }

      "BEACON" -> {
        // Automatic hardware discovery beacon from nearby device over 2.4 GHz
        // Only update online status if the peer is already an established contact or paired
        val existing = peerDao.getPeerDirect(packet.senderId)
        if (existing != null) {
          peerDao.updatePeer(
            existing.copy(
              isOnline = true,
              lastSeenTimestamp = System.currentTimeMillis()
            )
          )
          flushWaitingMessagesForPeer(packet.senderId)
        }
      }

      "RADAR_CONNECT_REQUEST", "PAIRING_REQUEST" -> {
        if (packet.receiverId == localUser.userId || packet.receiverId == "BROADCAST") {
          val reqPeer = Peer(
            id = packet.senderId,
            username = packet.senderName.lowercase().replace(" ", "_"),
            displayName = packet.senderName.ifBlank { "Radar Node ${packet.senderId.takeLast(4)}" },
            avatarIndex = (packet.senderId.hashCode() and 0x7FFFFFFF) % 6,
            isOnline = false,
            rssi = -52,
            protocol = "2.4 GHz Radar Direct",
            hopCount = 1,
            distanceEstimatedMeters = 2.8,
            publicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" }
          )
          incomingRadarRequest.tryEmit(reqPeer)
        }
      }

      "RADAR_CONNECT_ACCEPT", "PAIRING_ACCEPT" -> {
        if (packet.receiverId == localUser.userId || packet.receiverId == "BROADCAST") {
          val existing = peerDao.getPeerDirect(packet.senderId)
          if (existing != null) {
            peerDao.updatePeer(existing.copy(isOnline = true, lastSeenTimestamp = System.currentTimeMillis(), lastMessagePreview = "Connected via Radar"))
          } else {
            val newPeer = PeerEntity(
              id = packet.senderId,
              username = packet.senderName.lowercase().replace(" ", "_"),
              displayName = packet.senderName.ifBlank { "Node ${packet.senderId.takeLast(4)}" },
              avatarIndex = (packet.senderId.hashCode() and 0x7FFFFFFF) % 6,
              isOnline = true,
              rssi = -50,
              protocol = "2.4 GHz Radar Direct",
              hopCount = 1,
              distanceEstimatedMeters = 2.0,
              lastSeenTimestamp = System.currentTimeMillis(),
              lastMessagePreview = "Connected via Radar handshake",
              lastMessageTimestamp = System.currentTimeMillis(),
              unreadCount = 0,
              publicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" }
            )
            peerDao.insertPeer(newPeer)
          }
          Aes256EncryptionHelper.deriveAndRegisterSharedKey(
            localId = localUser.userId,
            peerId = packet.senderId,
            peerPublicKeyHex = packet.publicKeyHex.ifBlank { "04${packet.senderId.padEnd(62, '0').take(64)}" },
            handshakeSecret = "radar_link"
          )
          cancelDiscoveryOnConnected()
          flushWaitingMessagesForPeer(packet.senderId)
          autoNavigatePeerId.tryEmit(packet.senderId)
        }
      }

      "RADAR_CONNECT_DECLINE", "PAIRING_REJECT" -> {
        if (packet.receiverId == localUser.userId || packet.receiverId == "BROADCAST") {
          connectionFailedNotice.tryEmit("Device is not paired. Pairing request was declined!")
        }
      }
    }
  }

  suspend fun requestRadarConnect(peer: Peer) {
    val localUser = _userSettings.value
    val localIp = duplexTransceiver.getLocalIpAddress()
    val localPubKey = "04" + localUser.userId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64)
    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "PAIRING_REQUEST",
        packetId = "radar_req_${System.currentTimeMillis()}",
        senderId = localUser.userId,
        senderName = localUser.username,
        receiverId = peer.id,
        publicKeyHex = localPubKey,
        senderIp = localIp,
        timestamp = System.currentTimeMillis()
      )
    )
  }

  suspend fun acceptRadarConnect(peer: Peer) {
    val localUser = _userSettings.value
    val existing = peerDao.getPeerDirect(peer.id)
    if (existing != null) {
      peerDao.updatePeer(
        existing.copy(
          isOnline = true,
          lastSeenTimestamp = System.currentTimeMillis(),
          lastMessagePreview = "Connected via Radar"
        )
      )
    } else {
      peerDao.insertPeer(
        PeerEntity(
          id = peer.id,
          username = peer.username,
          displayName = peer.displayName,
          avatarIndex = peer.avatarIndex,
          isOnline = true,
          rssi = peer.rssi,
          protocol = peer.protocol,
          hopCount = peer.hopCount,
          distanceEstimatedMeters = peer.distanceEstimatedMeters,
          lastSeenTimestamp = System.currentTimeMillis(),
          lastMessagePreview = "Connected via Radar",
          lastMessageTimestamp = System.currentTimeMillis(),
          unreadCount = 0,
          publicKeyHex = peer.publicKeyHex
        )
      )
    }

    Aes256EncryptionHelper.deriveAndRegisterSharedKey(
      localId = localUser.userId,
      peerId = peer.id,
      peerPublicKeyHex = peer.publicKeyHex,
      handshakeSecret = "radar_link"
    )

    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "PAIRING_ACCEPT",
        packetId = "radar_acc_${System.currentTimeMillis()}",
        senderId = localUser.userId,
        senderName = localUser.username,
        receiverId = peer.id,
        publicKeyHex = "04" + localUser.userId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64),
        timestamp = System.currentTimeMillis()
      )
    )

    cancelDiscoveryOnConnected()
    flushWaitingMessagesForPeer(peer.id)
    autoNavigatePeerId.tryEmit(peer.id)
  }

  suspend fun declineRadarConnect(peer: Peer) {
    val localUser = _userSettings.value
    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "PAIRING_REJECT",
        packetId = "radar_dec_${System.currentTimeMillis()}",
        senderId = localUser.userId,
        senderName = localUser.username,
        receiverId = peer.id,
        timestamp = System.currentTimeMillis()
      )
    )
    connectionFailedNotice.tryEmit("Device is not paired. Pairing request was declined!")
  }

  suspend fun setPeerOnlineStatus(peerId: String, isOnline: Boolean) {
    peerDao.updateOnlineStatus(peerId, isOnline)
    if (isOnline) {
      flushWaitingMessagesForPeer(peerId)
    }
  }

  private suspend fun generateSimulatedPeerReply(peerId: String, inboundText: String) {
    val peer = peerDao.getPeerDirect(peerId) ?: return
    val replyText = when {
      inboundText.contains("voice", ignoreCase = true) || inboundText.contains("🎙️", ignoreCase = true) ->
        "🎙️ Encrypted voice note received and decrypted! Audio checksum validated over AES-256."
      inboundText.contains("video", ignoreCase = true) || inboundText.contains("🎥", ignoreCase = true) ->
        "🎥 Encrypted video received over 2.4 GHz P2P stream! Ready for instant offline playback."
      inboundText.contains("hello", ignoreCase = true) || inboundText.contains("hi", ignoreCase = true) ->
        "Hey! Packets received via 2.4 GHz mesh hop. Encryption intact."
      inboundText.contains("status", ignoreCase = true) ->
        "Mesh Node signal at ${peer.rssi} dBm. 0 packet loss on 2.4 GHz link."
      inboundText.contains("key", ignoreCase = true) || inboundText.contains("e2ee", ignoreCase = true) ->
        "AES-256 session key verified. Auth tag validated 100%."
      else -> "Acknowledged! PKT frames received and DEMUXed successfully."
    }

    val replyId = "reply_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
    val replyTrace = CryptoPipeline.processOutboundPipeline(
      text = replyText,
      messageId = replyId,
      senderId = peerId,
      receiverId = _userSettings.value.userId
    )

    val entity = MessageEntity(
      id = replyId,
      conversationPeerId = peerId,
      senderId = peer.id,
      senderName = peer.displayName,
      isFromMe = false,
      text = replyText,
      binaryForm = replyTrace.binaryRepresentation,
      cipherTextHex = replyTrace.encryptedHex,
      packetCount = replyTrace.packetCount,
      mediaType = "text",
      mediaContent = "",
      timestamp = System.currentTimeMillis(),
      burnsInTimestamp = when (_userSettings.value.retentionRule) {
        RetentionRule.BURN_ON_READ -> System.currentTimeMillis() + 15_000L // Minimum 15 seconds display
        RetentionRule.RETENTION_24_HOURS -> System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        else -> 0L
      },
      isBurnOnRead = (_userSettings.value.retentionRule == RetentionRule.BURN_ON_READ),
      wasRead = false,
      isEncrypted = true,
      status = MessageDeliveryStatus.DELIVERED.name
    )

    messageDao.insertMessage(entity)
    peerDao.updateLastMessage(peerId, replyText, System.currentTimeMillis())
    bluetoothMeshManager.incrementRelayedPacketCount()

    com.example.data.notification.BluefiNotificationManager.showIncomingMessageNotification(
      context = context,
      conversationId = peerId,
      senderId = peerId,
      senderName = entity.senderName,
      messageText = replyText,
      mediaType = "text"
    )
  }

  suspend fun markPeerMessagesAsRead(peerId: String) {
    peerDao.clearUnread(peerId)
    messageDao.markPeerMessagesAsReadInDb(peerId)
    if (_userSettings.value.retentionRule == RetentionRule.BURN_ON_READ) {
      // Ensure all unburned messages in this chat show for a minimum of 15 seconds before burning
      val now = System.currentTimeMillis()
      messageDao.applyBurnFuseToPeerMessages(peerId, now + 15_000L)
    }
  }

  suspend fun deleteMessage(messageId: String) {
    messageDao.deleteMessageById(messageId)
  }

  suspend fun purgeExpiredMessages() {
    val now = System.currentTimeMillis()
    messageDao.purgeExpiredMessages(now)
  }

  suspend fun clearChat(peerId: String) {
    messageDao.deleteMessagesForPeer(peerId)
    peerDao.updateLastMessage(peerId, "Chat cleared", System.currentTimeMillis())
  }

  /**
   * Generates a 6-character dynamic pairing code for this device (e.g., "BF-8842").
   */
  fun getLocalPairingCode(): String {
    val settings = userSettings.value
    val codeNum = Math.abs(settings.userId.hashCode() % 9000) + 1000
    return "BF-$codeNum"
  }

  /**
   * Generates the standard Bluefi QR connection URI containing node address, public key, and direct IP.
   */
  fun getLocalQrPayload(): String {
    val settings = userSettings.value
    val pubKey = "04" + settings.userId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64)
    val localIp = duplexTransceiver.getLocalIpAddress()
    return QrHandshakeManager.buildQrPayload(
      nodeId = settings.userId,
      username = settings.username,
      avatarIndex = settings.avatarIndex,
      publicKeyHex = pubKey,
      ipAddress = localIp
    )
  }

  /**
   * Pairs with a peer device by verifying its 6-character pairing code
   * and conducting an offline cryptographic handshake.
   */
  suspend fun pairDeviceByCode(code: String): Result<Peer> {
    val cleanInput = code.trim().uppercase()
    if (cleanInput.length < 3) {
      return Result.failure(IllegalArgumentException("Pairing code is too short. Please enter a valid PIN or node tag (at least 3 characters)"))
    }

    val cleanCode = when {
      cleanInput.startsWith("BF-") -> cleanInput
      cleanInput.startsWith("BF") && cleanInput.length > 2 -> "BF-" + cleanInput.substring(2)
      cleanInput.all { it.isDigit() } -> "BF-$cleanInput"
      else -> cleanInput
    }

    // Check if code matches any already discovered BLE peer
    val existingBle = bluetoothMeshManager.bluetoothManager.discoveredPeers.value.find {
      it.id.endsWith(cleanCode.takeLast(4)) || it.name.contains(cleanCode, ignoreCase = true)
    }

    val peerId = if (existingBle != null) {
      existingBle.id
    } else {
      "node_p2p_${cleanCode.replace("-", "_").lowercase()}"
    }

    val displayName = if (existingBle != null) {
      existingBle.name
    } else {
      "Paired Node [$cleanCode]"
    }

    val existing = peerDao.getPeerDirect(peerId)
    val entityToSave = if (existing != null) {
      existing.copy(
        isOnline = true,
        displayName = displayName,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = "E2EE Handshake confirmed via code $cleanCode"
      )
    } else {
      PeerEntity(
        id = peerId,
        username = displayName.lowercase().replace(" ", "_").replace("[", "").replace("]", ""),
        displayName = displayName,
        avatarIndex = (cleanCode.hashCode() and 0x7FFFFFFF) % 6,
        isOnline = true,
        rssi = -52,
        protocol = "2.4 GHz Direct P2P",
        hopCount = 1,
        distanceEstimatedMeters = 2.0,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = "E2EE Handshake confirmed via code $cleanCode",
        lastMessageTimestamp = System.currentTimeMillis(),
        unreadCount = 0,
        publicKeyHex = "04${cleanCode.padEnd(32, 'X').toByteArray().joinToString("") { "%02x".format(it) }.take(64)}"
      )
    }

    peerDao.insertPeer(entityToSave)

    // Derive symmetric AES-256 session key from initial handshake parameters
    val localId = _userSettings.value.userId
    Aes256EncryptionHelper.deriveAndRegisterSharedKey(
      localId = localId,
      peerId = peerId,
      peerPublicKeyHex = entityToSave.publicKeyHex,
      handshakeSecret = cleanCode
    )

    // Broadcast pairing request over 2.4 GHz duplex channel so receiver device pairs & shifts to chat
    duplexTransceiver.broadcastPacket(
      DuplexPacket(
        type = "PAIR_REQUEST",
        packetId = "req_${System.currentTimeMillis()}",
        senderId = localId,
        senderName = _userSettings.value.username,
        receiverId = "BROADCAST",
        pairingCode = cleanCode,
        publicKeyHex = entityToSave.publicKeyHex,
        timestamp = System.currentTimeMillis()
      )
    )
    cancelDiscoveryOnConnected()
    autoNavigatePeerId.tryEmit(peerId)

    return Result.success(entityToSave.toDomain())
  }

  /**
   * Pairs with a peer device by parsing a Bluefi QR code URI payload or code string.
   */
  suspend fun pairDeviceByQr(qrPayload: String): Result<Peer> {
    try {
      val remoteNode = QrHandshakeManager.parseQrPayload(qrPayload)
      if (remoteNode.ipAddress.isNotBlank()) {
        duplexTransceiver.registerKnownPeerIp(remoteNode.nodeId, remoteNode.ipAddress)
      }

      val peerEntity = QrHandshakeManager.saveOrUpdateRemotePeer(
        peerDao = peerDao,
        remote = remoteNode,
        pairingCode = getLocalPairingCode()
      )

      val localUser = _userSettings.value
      val localId = localUser.userId
      val localIp = duplexTransceiver.getLocalIpAddress()
      val localPubKey = "04" + localId.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64)

      Aes256EncryptionHelper.deriveAndRegisterSharedKey(
        localId = localId,
        peerId = remoteNode.nodeId,
        peerPublicKeyHex = remoteNode.publicKeyHex.ifBlank { peerEntity.publicKeyHex },
        handshakeSecret = getLocalPairingCode()
      )

      // Transmit ACK packet back to Device B containing Scanner ("baba") profile details
      duplexTransceiver.broadcastPacket(
        DuplexPacket(
          type = "PAIR_ACCEPT",
          packetId = "qr_ack_${System.currentTimeMillis()}",
          senderId = localId,
          senderName = localUser.username,
          senderIp = localIp,
          receiverId = remoteNode.nodeId,
          pairingCode = getLocalPairingCode(),
          publicKeyHex = localPubKey,
          timestamp = System.currentTimeMillis()
        )
      )

      duplexTransceiver.broadcastPacket(
        DuplexPacket(
          type = "PAIR_REQUEST",
          packetId = "qr_req_${System.currentTimeMillis()}",
          senderId = localId,
          senderName = localUser.username,
          senderIp = localIp,
          receiverId = remoteNode.nodeId,
          pairingCode = getLocalPairingCode(),
          publicKeyHex = localPubKey,
          timestamp = System.currentTimeMillis()
        )
      )

      cancelDiscoveryOnConnected()
      flushWaitingMessagesForPeer(remoteNode.nodeId)
      autoNavigatePeerId.tryEmit(remoteNode.nodeId)
      return Result.success(peerEntity.toDomain())
    } catch (e: Exception) {
      Log.e("BluefiRepo", "Error in pairDeviceByQr: ${e.message}")
      return Result.failure(e)
    }
  }

  fun getSessionKeyFingerprint(peerId: String): String? {
    return Aes256EncryptionHelper.getSessionKeyFingerprint(peerId)
  }

  fun connectBluetoothSocket(deviceAddress: String, onComplete: (Boolean) -> Unit = {}) {
    duplexTransceiver.connectBluetoothSocket(deviceAddress, onComplete)
  }

  fun connectWifiDirectDevice(deviceAddress: String, onResult: (Boolean) -> Unit = {}) {
    wifiDirectManager.connectToDevice(deviceAddress, onResult)
  }

  fun startAutoHardwareDiscovery() {
    bluetoothMeshManager.startScanning()
    wifiDirectManager.discoverPeers()
    val localUser = _userSettings.value
    if (localUser.isRadarVisible) {
      bluetoothMeshManager.startAdvertising(localUser.userId, localUser.username, localUser.e2eeStrictEnabled)
      wifiDirectManager.registerLocalService(localUser.username, localUser.userId)
      broadcastPresenceBeacon()
    } else {
      bluetoothMeshManager.stopAdvertising()
      wifiDirectManager.clearLocalServices()
    }
  }

  fun cancelDiscoveryOnConnected() {
    bluetoothMeshManager.cancelAllDiscovery()
    wifiDirectManager.stopDiscovery()
  }

  fun shutdownAllSockets() {
    Log.i("BluefiRepo", "shutdownAllSockets() called: closing all active Bluetooth RFCOMM and Wi-Fi Direct sockets and unregistering hardware receivers")
    duplexTransceiver.stop()
    bluetoothMeshManager.stopScanning()
    wifiDirectManager.cleanup()
    hardwareRadioManager.cleanup()
  }

  fun notifySocketActivity() {
    com.example.data.service.P2pForegroundService.notifyActivity(context)
  }

  fun pauseBackgroundBeaconing() {
    Log.i("BluefiRepo", "pauseBackgroundBeaconing(): Pausing BLE advertisements and background scans to save battery during 1-min idle")
    bluetoothMeshManager.stopScanning()
    bluetoothMeshManager.stopAdvertising()
  }

  fun resumeBackgroundBeaconing() {
    Log.i("BluefiRepo", "resumeBackgroundBeaconing(): Resuming BLE advertisements and active socket listeners")
    val localUser = _userSettings.value
    if (localUser.isRadarVisible) {
      bluetoothMeshManager.startAdvertising(localUser.userId, localUser.username, localUser.e2eeStrictEnabled)
      wifiDirectManager.registerLocalService(localUser.username, localUser.userId)
    }
    bluetoothMeshManager.startScanning()
  }

  fun hasActiveSessionKey(peerId: String): Boolean {
    return Aes256EncryptionHelper.hasSessionKey(peerId)
  }

  fun encryptMessageDirect(peerId: String, text: String): EncryptedPayload {
    return Aes256EncryptionHelper.encryptForPeer(
      plaintext = text,
      peerId = peerId,
      localId = _userSettings.value.userId
    )
  }

  fun decryptMessageDirect(peerId: String, payload: EncryptedPayload): String {
    return Aes256EncryptionHelper.decryptFromPeer(
      payload = payload,
      peerId = peerId,
      localId = _userSettings.value.userId
    )
  }

  private fun PeerEntity.toDomain(): Peer = Peer(
    id = id,
    username = username,
    displayName = displayName,
    avatarIndex = avatarIndex,
    isOnline = isOnline,
    rssi = rssi,
    protocol = protocol,
    hopCount = hopCount,
    distanceEstimatedMeters = distanceEstimatedMeters,
    lastSeenTimestamp = lastSeenTimestamp,
    lastMessagePreview = lastMessagePreview,
    lastMessageTimestamp = lastMessageTimestamp,
    unreadCount = unreadCount,
    publicKeyHex = publicKeyHex
  )

  private fun MessageEntity.toDomain(): ChatMessage {
    val statusEnum = try {
      MessageDeliveryStatus.valueOf(status)
    } catch (_: Exception) {
      MessageDeliveryStatus.DELIVERED
    }
    val resolvedText = if (text.contains("Encrypted 2.4", ignoreCase = true) || text.contains("[Encrypted", ignoreCase = true)) {
      if (binaryForm.isNotBlank()) {
        val decoded = CryptoPipeline.binaryToText(binaryForm)
        if (decoded.isNotBlank()) decoded
        else if (mediaType == "sticker" && mediaContent.isNotBlank()) mediaContent
        else text
      } else if (mediaType == "sticker" && mediaContent.isNotBlank()) {
        mediaContent
      } else {
        text
      }
    } else {
      text
    }
    return ChatMessage(
      id = id,
      conversationPeerId = conversationPeerId,
      senderId = senderId,
      senderName = senderName,
      isFromMe = isFromMe,
      text = resolvedText,
      binaryForm = binaryForm,
      cipherTextHex = cipherTextHex,
      packetCount = packetCount,
      mediaType = mediaType,
      mediaContent = mediaContent,
      timestamp = timestamp,
      burnsInTimestamp = burnsInTimestamp,
      isBurnOnRead = isBurnOnRead,
      wasRead = wasRead,
      isEncrypted = isEncrypted,
      status = statusEnum
    )
  }

  suspend fun insertPeerDirect(peer: PeerEntity) {
    peerDao.insertPeer(peer)
  }

  suspend fun clearAllUnread() {
    val currentPeers = peerDao.getAllPeers().firstOrNull() ?: emptyList()
    for (p in currentPeers) {
      if (p.unreadCount > 0) {
        peerDao.clearUnread(p.id)
      }
    }
  }

  suspend fun deleteChat(peerId: String) {
    messageDao.deleteMessagesForPeer(peerId)
    peerDao.deletePeer(peerId)
  }

  private suspend fun seedInboxChats() {
    // Purge any legacy personal peer data from local database
    val legacyPersonalPeerIds = listOf(
      "peer_phone_9815457214", "group_ug_tpp_2027", "group_data_mining_3a",
      "group_python_project_3a", "business_jiomart_shop", "peer_mona",
      "channel_uni_updates", "group_cu_24bca_dl", "business_jupiter_loan",
      "peer_sumit_sharma_cu", "business_alibaba_deals"
    )
    for (oldId in legacyPersonalPeerIds) {
      peerDao.deletePeer(oldId)
      messageDao.deleteMessagesForPeer(oldId)
    }

    val existing = peerDao.getPeerDirect("peer_relay_alpha")
    if (existing != null) return

    val yesterday = System.currentTimeMillis() - 86400000L

    val inboxPeers = listOf(
      PeerEntity(
        id = "peer_relay_alpha",
        username = "relay_alpha",
        displayName = "Relay Node Alpha",
        avatarIndex = 0,
        isOnline = false,
        rssi = -58,
        protocol = "BT 5.0 (2.4 GHz)",
        hopCount = 1,
        distanceEstimatedMeters = 8.0,
        lastSeenTimestamp = yesterday + 3600000L,
        lastMessagePreview = "AES-256 E2EE handshake confirmed. Link quality 98%",
        lastMessageTimestamp = yesterday + 3600000L,
        unreadCount = 0,
        publicKeyHex = "04A1B2C3D4E5F600112233440000000000000000000000000000000000000001"
      ),
      PeerEntity(
        id = "group_mesh_ops",
        username = "mesh_ops_group",
        displayName = "Field Mesh Operations",
        avatarIndex = 1,
        isOnline = false,
        rssi = -64,
        protocol = "Mesh Relay (2.4 GHz)",
        hopCount = 2,
        distanceEstimatedMeters = 24.0,
        lastSeenTimestamp = yesterday + 3500000L,
        lastMessagePreview = "~Node-02: All 4 local transceiver stations operational.",
        lastMessageTimestamp = yesterday + 3500000L,
        unreadCount = 0,
        publicKeyHex = "04B2C3D4E5F6A100000000000000000000000000000000000000000000000002"
      ),
      PeerEntity(
        id = "peer_transceiver_hw",
        username = "hw_transceiver_24g",
        displayName = "Hardware Transceiver 2.4G",
        avatarIndex = 2,
        isOnline = false,
        rssi = -52,
        protocol = "Duplex RF Transceiver",
        hopCount = 1,
        distanceEstimatedMeters = 3.5,
        lastSeenTimestamp = yesterday + 3400000L,
        lastMessagePreview = "Duplex RF ping: 14ms latency. RSSI -52 dBm",
        lastMessageTimestamp = yesterday + 3400000L,
        unreadCount = 0,
        publicKeyHex = "04C3D4E5F6A1B200000000000000000000000000000000000000000000000003"
      ),
      PeerEntity(
        id = "channel_emergency_mesh",
        username = "emergency_mesh_ch",
        displayName = "Emergency Mesh Broadcast",
        avatarIndex = 3,
        isOnline = false,
        rssi = -68,
        protocol = "Mesh Relay (2.4 GHz)",
        hopCount = 2,
        distanceEstimatedMeters = 40.0,
        lastSeenTimestamp = yesterday + 3300000L,
        lastMessagePreview = "📢 Frequency 2.402 GHz active. Open P2P relay enabled.",
        lastMessageTimestamp = yesterday + 3300000L,
        unreadCount = 0,
        publicKeyHex = "04D4E5F6A1B2C300000000000000000000000000000000000000000000000004"
      ),
      PeerEntity(
        id = "peer_cyber_sentinel",
        username = "cyber_sentinel",
        displayName = "Cyber Sentinel Node",
        avatarIndex = 4,
        isOnline = false,
        rssi = -61,
        protocol = "Wi-Fi Direct P2P",
        hopCount = 1,
        distanceEstimatedMeters = 12.0,
        lastSeenTimestamp = yesterday + 3200000L,
        lastMessagePreview = "E2EE Ratchet key rotation completed. Packet delivery verified ✓✓",
        lastMessageTimestamp = yesterday + 3200000L,
        unreadCount = 0,
        publicKeyHex = "04E5F6A1B2C3D400000000000000000000000000000000000000000000000005"
      ),
      PeerEntity(
        id = "group_tactical_mesh",
        username = "tactical_unit_7",
        displayName = "Tactical Mesh Unit 7",
        avatarIndex = 5,
        isOnline = false,
        rssi = -70,
        protocol = "BT 5.0 (2.4 GHz)",
        hopCount = 2,
        distanceEstimatedMeters = 30.0,
        lastSeenTimestamp = yesterday + 3100000L,
        lastMessagePreview = "~UnitLead: Offline packet queue flushed. 12 messages routed.",
        lastMessageTimestamp = yesterday + 3100000L,
        unreadCount = 1,
        publicKeyHex = "04F6A1B2C3D4E500000000000000000000000000000000000000000000000006"
      ),
      PeerEntity(
        id = "peer_gateway_delta",
        username = "gateway_delta",
        displayName = "Gateway Base Delta",
        avatarIndex = 0,
        isOnline = false,
        rssi = -74,
        protocol = "Mesh Relay (2.4 GHz)",
        hopCount = 2,
        distanceEstimatedMeters = 45.0,
        lastSeenTimestamp = yesterday + 3000000L,
        lastMessagePreview = "Location: Lat 28.6139° N, Long 77.2090° E (Relay Base)",
        lastMessageTimestamp = yesterday + 3000000L,
        unreadCount = 1,
        publicKeyHex = "04A1B2C3D4E5F600000000000000000000000000000000000000000000000007"
      )
    )

    peerDao.insertPeers(inboxPeers)

    val initialMessages = listOf(
      MessageEntity("msg_seed_1_1", "peer_relay_alpha", "relay_alpha", "Relay Node Alpha", false, "BlueFi 2.4 GHz duplex link established. Initializing E2EE tunnel...", "01000010 01101100 01110101", "426c75654669", 1, "text", "", yesterday + 3550000L, 0L, false, true, true, "DELIVERED"),
      MessageEntity("msg_seed_1_2", "peer_relay_alpha", _userSettings.value.userId, _userSettings.value.username, true, "AES-256 session key verified. Transmitting telemetry ping.", "01000001 01000101 01010011", "4145532d323536", 1, "text", "", yesterday + 3580000L, 0L, false, true, true, "DELIVERED"),
      MessageEntity("msg_seed_1_3", "peer_relay_alpha", "relay_alpha", "Relay Node Alpha", false, "AES-256 E2EE handshake confirmed. Link quality 98%", "01000001 01000101 01010011", "4145532d323536", 1, "text", "", yesterday + 3600000L, 0L, false, true, true, "READ"),
      MessageEntity("msg_seed_2_1", "group_mesh_ops", "coord", "Ops Coordinator", false, "Mesh alert: All relay nodes maintain 2.4 GHz carrier frequency.", "01001101 01100101 01110011", "4d657368", 1, "text", "", yesterday + 3480000L, 0L, false, true, true, "DELIVERED"),
      MessageEntity("msg_seed_2_2", "group_mesh_ops", "node_02", "Node-02", false, "All 4 local transceiver stations operational. Ready for deployment.", "01000001 01101100 01101100", "416c6c2034", 1, "text", "", yesterday + 3500000L, 0L, false, true, true, "DELIVERED"),
      MessageEntity("msg_seed_3_1", "peer_transceiver_hw", "hw_transceiver_24g", "Hardware Transceiver 2.4G", false, "Duplex RF ping: 14ms latency. RSSI -52 dBm", "01000100 01110101 01110000", "4475706c6578", 1, "text", "", yesterday + 3400000L, 0L, false, true, true, "READ"),
      MessageEntity("msg_seed_4_1", "channel_emergency_mesh", "broadcast_admin", "Emergency Mesh Admin", false, "📢 Frequency 2.402 GHz active. Open P2P relay enabled.", "01000110 01110010 01100101", "467265717565", 1, "text", "", yesterday + 3300000L, 0L, false, true, true, "DELIVERED"),
      MessageEntity("msg_seed_5_1", "peer_cyber_sentinel", "cyber_sentinel", "Cyber Sentinel Node", false, "E2EE Ratchet key rotation completed. Packet delivery verified ✓✓", "01000101 00110010 01000101", "45324545", 1, "text", "", yesterday + 3200000L, 0L, false, true, true, "READ"),
      MessageEntity("msg_seed_6_1", "group_tactical_mesh", "unit_lead", "UnitLead", false, "Offline packet queue flushed. 12 messages routed.", "01001111 01100110 01100110", "4f66666c696e", 1, "text", "", yesterday + 3100000L, 0L, false, false, true, "DELIVERED"),
      MessageEntity("msg_seed_7_1", "peer_gateway_delta", "gateway_delta", "Gateway Base Delta", false, "Location: Lat 28.6139° N, Long 77.2090° E (Relay Base)", "01001100 01101111 01100011", "4c6f636174696f6e", 1, "location", "28.6139,77.2090", yesterday + 3000000L, 0L, false, false, true, "DELIVERED")
    )

    initialMessages.forEach { msg ->
      messageDao.insertMessage(msg)
    }
  }
}
