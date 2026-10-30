package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BluefiApplication
import com.example.data.crypto.CryptoPipeline
import com.example.data.crypto.FullPipelineTrace
import com.example.data.mesh.MeshNetworkStats
import com.example.data.mesh.WifiDirectState
import com.example.data.model.ChatMessage
import com.example.data.model.Peer
import com.example.data.model.RetentionRule
import com.example.data.model.UserSettings
import com.example.data.repository.BluefiRepository
import com.example.ui.theme.BluefiThemeOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BluefiViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: BluefiRepository = (application as BluefiApplication).repository

  val userSettings: StateFlow<UserSettings> = repository.userSettings
  val allPeers: StateFlow<List<Peer>> = repository.allPeers
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val networkStats: StateFlow<MeshNetworkStats> = repository.bluetoothMeshManager.networkStats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeshNetworkStats())

  val wifiDirectState: StateFlow<WifiDirectState> = repository.wifiDirectManager.state
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WifiDirectState())

  // Authentication State
  // App starts locked for data privacy. User must enter password on every app open.
  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  val autoNavigatePeerId = repository.autoNavigatePeerId

  private val _authFeedbackMessage = MutableStateFlow<String?>(null)
  val authFeedbackMessage: StateFlow<String?> = _authFeedbackMessage.asStateFlow()

  private val _isAuthSuccess = MutableStateFlow(false)
  val isAuthSuccess: StateFlow<Boolean> = _isAuthSuccess.asStateFlow()

  private val _loginError = MutableStateFlow<String?>(null)
  val loginError: StateFlow<String?> = _loginError.asStateFlow()

  private val _showAuthPopup = MutableStateFlow(false)
  val showAuthPopup: StateFlow<Boolean> = _showAuthPopup.asStateFlow()

  private val _authPopupMessage = MutableStateFlow("")
  val authPopupMessage: StateFlow<String> = _authPopupMessage.asStateFlow()

  private val _authPopupSuccess = MutableStateFlow(false)
  val authPopupSuccess: StateFlow<Boolean> = _authPopupSuccess.asStateFlow()

  fun dismissAuthPopup() {
    _showAuthPopup.value = false
    _authFeedbackMessage.value = null
    _authPopupMessage.value = ""
    _loginError.value = null
  }

  // Real-time hardware radio monitoring (Bluetooth & Wi-Fi & P2P Mesh Connectivity)
  val hardwareRadioState = repository.hardwareRadioState

  private val _showRadioAlertPopup = MutableStateFlow(false)
  val showRadioAlertPopup: StateFlow<Boolean> = _showRadioAlertPopup.asStateFlow()

  fun dismissRadioAlertPopup() {
    _showRadioAlertPopup.value = false
  }

  fun checkHardwareRadios() {
    repository.hardwareRadioManager.updateState()
    val current = repository.hardwareRadioState.value
    if (!current.isBluetoothEnabled || !current.isWifiEnabled) {
      _showRadioAlertPopup.value = true
    } else {
      _showRadioAlertPopup.value = false
    }
  }

  fun openBluetoothSettings() {
    repository.hardwareRadioManager.openBluetoothSettings()
  }

  fun openWifiSettings() {
    repository.hardwareRadioManager.openWifiSettings()
  }

  fun turnOnBluetoothInternally() {
    repository.hardwareRadioManager.enableBluetoothInternally()
    repository.hardwareRadioManager.updateState()
    _showRadioAlertPopup.value = false
    repository.bluetoothMeshManager.startScanning()
  }

  fun turnOnWifiInternally() {
    repository.hardwareRadioManager.enableWifiInternally()
    repository.hardwareRadioManager.updateState()
    _showRadioAlertPopup.value = false
    repository.wifiDirectManager.discoverPeers()
  }

  fun turnOnBothRadiosInternally() {
    repository.hardwareRadioManager.enableBothRadiosDirectly()
    repository.hardwareRadioManager.updateState()
    _showRadioAlertPopup.value = false
    repository.wifiDirectManager.discoverPeers()
    repository.bluetoothMeshManager.startScanning()
  }

  fun requestXosBatteryExemption() {
    repository.hardwareRadioManager.requestXosBatteryExemption()
  }

  fun isBatteryOptimizationExempted(): Boolean {
    return repository.hardwareRadioManager.isBatteryOptimizationExempted()
  }

  fun requestBatteryOptimizationExemption() {
    repository.hardwareRadioManager.requestBatteryOptimizationExemption()
  }

  fun startAutoHardwareDiscovery() {
    repository.startAutoHardwareDiscovery()
  }

  fun connectBluetoothSocket(deviceAddress: String, onComplete: (Boolean) -> Unit = {}) {
    repository.connectBluetoothSocket(deviceAddress, onComplete)
  }

  fun connectWifiDirectDevice(deviceAddress: String, onResult: (Boolean) -> Unit = {}) {
    repository.connectWifiDirectDevice(deviceAddress, onResult)
  }

  // Active Peer for single chat
  private val _selectedPeerId = MutableStateFlow<String?>(null)
  val selectedPeerId: StateFlow<String?> = _selectedPeerId.asStateFlow()

  private val _selectedPeerMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val selectedPeerMessages: StateFlow<List<ChatMessage>> = _selectedPeerMessages.asStateFlow()

  // Split Chat state (allows simultaneous dual-peer chatting)
  private val _splitLeftPeerId = MutableStateFlow<String?>(null)
  val splitLeftPeerId: StateFlow<String?> = _splitLeftPeerId.asStateFlow()

  private val _splitRightPeerId = MutableStateFlow<String?>(null)
  val splitRightPeerId: StateFlow<String?> = _splitRightPeerId.asStateFlow()

  private val _splitLeftMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val splitLeftMessages: StateFlow<List<ChatMessage>> = _splitLeftMessages.asStateFlow()

  private val _splitRightMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val splitRightMessages: StateFlow<List<ChatMessage>> = _splitRightMessages.asStateFlow()

  // Pipeline Inspector
  private val _activeTrace = MutableStateFlow<FullPipelineTrace?>(null)
  val activeTrace: StateFlow<FullPipelineTrace?> = _activeTrace.asStateFlow()

  private val _inspectingMessage = MutableStateFlow<ChatMessage?>(null)
  val inspectingMessage: StateFlow<ChatMessage?> = _inspectingMessage.asStateFlow()

  init {
    // Observe single chat messages with flatMapLatest so switching peers never hangs
    @OptIn(ExperimentalCoroutinesApi::class)
    viewModelScope.launch {
      _selectedPeerId
        .flatMapLatest { id ->
          if (id != null) {
            repository.markPeerMessagesAsRead(id)
            repository.getMessagesForPeer(id)
          } else {
            flowOf(emptyList())
          }
        }
        .collect { messages ->
          _selectedPeerMessages.value = messages
        }
    }

    // Observe Split Chat left peer messages
    @OptIn(ExperimentalCoroutinesApi::class)
    viewModelScope.launch {
      _splitLeftPeerId
        .flatMapLatest { id ->
          if (id != null) {
            repository.getMessagesForPeer(id)
          } else {
            flowOf(emptyList())
          }
        }
        .collect { messages ->
          _splitLeftMessages.value = messages
        }
    }

    // Observe Split Chat right peer messages
    @OptIn(ExperimentalCoroutinesApi::class)
    viewModelScope.launch {
      _splitRightPeerId
        .flatMapLatest { id ->
          if (id != null) {
            repository.getMessagesForPeer(id)
          } else {
            flowOf(emptyList())
          }
        }
        .collect { messages ->
          _splitRightMessages.value = messages
        }
    }

    // Initialize pipeline demo trace
    runPipelineDemo("Hello from Bluefi Mesh!")

    // Observe incoming radar connection requests
    viewModelScope.launch {
      repository.incomingRadarRequest.collect { peer ->
        _incomingRadarConnection.value = peer
      }
    }

    // Observe radar connection failure notices
    viewModelScope.launch {
      repository.connectionFailedNotice.collect { notice ->
        _connectionFailedNotice.value = notice
        _pairingProgressPeer.value = null
      }
    }
    viewModelScope.launch {
      repository.autoNavigatePeerId.collect { peerId ->
        if (peerId.isNotBlank()) {
          _pairingProgressPeer.value = null
          _selectedPeerId.value = peerId
        }
      }
    }
  }

  fun isAccountRegistered(): Boolean = repository.isAccountCreated()

  fun getRegisteredUserId(): String = repository.getStoredUserId()

    fun createAccount(
    userId: String,
    pass: String,
    q1: String = "", a1: String = "",
    q2: String = "", a2: String = "",
    q3: String = "", a3: String = ""
  ): Boolean {
    val cleanId = userId.trim()
    if (cleanId.isBlank() || pass.isBlank()) {
      _authFeedbackMessage.value = "Password Sahi likh MahaRaj "
      _authPopupMessage.value = "Password Sahi likh MahaRaj "
      _isAuthSuccess.value = false
      _authPopupSuccess.value = false
      _showAuthPopup.value = true
      return false
    }
    viewModelScope.launch {
      val success = repository.createAccountWithSecurityQuestions(cleanId, pass, q1, a1, q2, a2, q3, a3)
      if (!success) {
        _authFeedbackMessage.value = "Storage limit reached (max 5 accounts)."
        _authPopupMessage.value = "Storage limit reached (max 5 accounts). Overwrite an existing profile."
        _isAuthSuccess.value = false
        _authPopupSuccess.value = false
        _showAuthPopup.value = true
      } else {
        _authFeedbackMessage.value = "Hello Boss!"
        _authPopupMessage.value = "Hello Boss!"
        _isAuthSuccess.value = true
        _authPopupSuccess.value = true
        _showAuthPopup.value = true
        _loginError.value = null
      }
    }
    return true
  }

  fun loginToAccount(userId: String, pass: String, callback: (Boolean) -> Unit) {
    viewModelScope.launch {
      val ok = repository.verifyLoginForAccount(userId, pass)
      if (ok) {
        _authFeedbackMessage.value = "Hello Boss!"
        _authPopupMessage.value = "Hello Boss!"
        _isAuthSuccess.value = true
        _authPopupSuccess.value = true
        _showAuthPopup.value = true
        _loginError.value = null
      } else {
        _authFeedbackMessage.value = "Password Sahi likh MahaRaj "
        _authPopupMessage.value = "Password Sahi likh MahaRaj "
        _isAuthSuccess.value = false
        _authPopupSuccess.value = false
        _showAuthPopup.value = true
      }
      callback(ok)
    }
  }

  suspend fun getSecurityQuestions(userId: String) = repository.getSecurityQuestionsForUser(userId)

  suspend fun verifySecurityAnswers(userId: String, a1: String, a2: String, a3: String) =
    repository.verifySecurityAnswers(userId, a1, a2, a3)

  suspend fun resetPasswordWithAnswers(userId: String, newPass: String) =
    repository.resetPasswordAfterVerification(userId, newPass)

  fun updateLockFormatAndSecret(userId: String, lockType: String, passcode: String) {
    viewModelScope.launch {
      repository.updateLockFormatAndSecret(userId, lockType, passcode)
    }
  }

  fun verifyLoginPassword(pass: String): Boolean {
    if (repository.verifyPassword(pass)) {
      _authFeedbackMessage.value = "Hello Boss!"
      _authPopupMessage.value = "Hello Boss!"
      _isAuthSuccess.value = true
      _authPopupSuccess.value = true
      _showAuthPopup.value = true
      _loginError.value = null
      return true
    } else {
      _authFeedbackMessage.value = "Password Sahi likh MahaRaj "
      _authPopupMessage.value = "Password Sahi likh MahaRaj "
      _isAuthSuccess.value = false
      _authPopupSuccess.value = false
      _showAuthPopup.value = true
      _loginError.value = "Password Sahi likh MahaRaj "
      return false
    }
  }

  fun grantAccess() {
    _showAuthPopup.value = false
    _isLoggedIn.value = true
    checkHardwareRadios()
  }

  fun clearAuthFeedback() {
    _authFeedbackMessage.value = null
  }

  fun lockApp() {
    _isLoggedIn.value = false
    _authFeedbackMessage.value = null
    _loginError.value = null
  }

  fun resetAccount() {
    repository.resetAccount()
    _isLoggedIn.value = false
    _authFeedbackMessage.value = null
    _loginError.value = null
  }

  fun logout() {
    lockApp()
  }

  fun selectPeer(peerId: String) {
    _selectedPeerId.value = peerId
    viewModelScope.launch {
      repository.markPeerMessagesAsRead(peerId)
    }
  }

  fun setSplitPeers(leftId: String?, rightId: String?) {
    _splitLeftPeerId.value = leftId
    _splitRightPeerId.value = rightId
  }

  fun sendMessage(
    peerId: String,
    text: String,
    mediaType: String = "text",
    mediaContent: String = ""
  ) {
    if (text.isBlank() && mediaContent.isBlank()) return
    viewModelScope.launch {
      val trace = repository.sendMessage(peerId, text, mediaType, mediaContent)
      _activeTrace.value = trace
    }
  }

  val peerTypingMap: StateFlow<Map<String, Boolean>> = repository.peerTypingMap

  fun isPeerTyping(peerId: String): Flow<Boolean> {
    return repository.peerTypingMap.map { it[peerId] == true }
  }

  fun notifyLocalTyping(peerId: String, isTyping: Boolean) {
    repository.sendTypingIndicator(peerId, isTyping)
  }

  fun simulatePeerTyping(peerId: String) {
    repository.simulatePeerTyping(peerId)
  }

  fun openMessageInspector(message: ChatMessage) {
    _inspectingMessage.value = message
    // If trace matches message text, keep it; otherwise run trace
    val trace = CryptoPipeline.processOutboundPipeline(
      text = message.text.ifBlank { "Media: ${message.mediaType}" },
      messageId = message.id,
      senderId = message.senderId,
      receiverId = message.conversationPeerId
    )
    _activeTrace.value = trace
  }

  fun closeMessageInspector() {
    _inspectingMessage.value = null
  }

  fun runPipelineDemo(sampleText: String) {
    val trace = CryptoPipeline.processOutboundPipeline(
      text = sampleText,
      messageId = "demo_pkt_01",
      senderId = userSettings.value.userId,
      receiverId = "peer_node_abc"
    )
    _activeTrace.value = trace
  }

  fun updateProfile(newUsername: String, avatarIndex: Int) {
    repository.updateUserSettings {
      it.copy(
        username = newUsername.ifBlank { it.username },
        avatarIndex = avatarIndex
      )
    }
  }

  fun setTheme(theme: BluefiThemeOption) {
    repository.updateUserSettings { it.copy(theme = theme) }
  }

  fun setRetentionRule(rule: RetentionRule) {
    repository.updateUserSettings { it.copy(retentionRule = rule) }
  }

  fun deleteMessage(messageId: String) {
    viewModelScope.launch {
      repository.deleteMessage(messageId)
    }
  }

  fun toggleBluetoothScan(scan: Boolean) {
    if (scan) {
      repository.bluetoothMeshManager.startScanning()
    } else {
      repository.bluetoothMeshManager.stopScanning()
    }
  }

  fun toggleBluetoothAdvertising(advertise: Boolean) {
    if (advertise) {
      val settings = userSettings.value
      repository.bluetoothMeshManager.startAdvertising(
        nodeId = settings.userId,
        displayName = settings.username,
        isEncrypted = settings.e2eeStrictEnabled
      )
    } else {
      repository.bluetoothMeshManager.stopAdvertising()
    }
  }

  val bluetoothDiscoveryState = repository.bluetoothMeshManager.bluetoothDiscoveryState
  val isRadarVisible: StateFlow<Boolean> = repository.radarVisibilityManager.isRadarVisible

  fun setRadarVisibility(visible: Boolean) {
    repository.setRadarVisibility(visible)
  }

  fun toggleWifiDirectDiscovery(discover: Boolean) {
    if (discover) {
      repository.wifiDirectManager.discoverPeers()
    } else {
      repository.wifiDirectManager.stopDiscovery()
    }
  }

  fun toggleRelayNode(active: Boolean) {
    repository.updateUserSettings { it.copy(isRelayNodeActive = active) }
  }

  fun updateUserSettings(update: (UserSettings) -> UserSettings) {
    repository.updateUserSettings(update)
  }

  fun toggleBatterySaver(enabled: Boolean) {
    repository.updateUserSettings { it.copy(isBatterySaverMode = enabled) }
  }

  fun clearChat(peerId: String) {
    viewModelScope.launch {
      repository.clearChat(peerId)
    }
  }

  fun getLocalPairingCode(): String = repository.getLocalPairingCode()

  fun getLocalQrPayload(): String = repository.getLocalQrPayload()

  fun getSessionKeyFingerprint(peerId: String): String? = repository.getSessionKeyFingerprint(peerId)

  fun hasActiveSessionKey(peerId: String): Boolean = repository.hasActiveSessionKey(peerId)

  fun pairDeviceByCode(code: String, onResult: (Result<Peer>) -> Unit) {
    viewModelScope.launch {
      val result = repository.pairDeviceByCode(code)
      onResult(result)
    }
  }

  fun pairDeviceByQr(qrPayload: String, onResult: (Result<Peer>) -> Unit) {
    viewModelScope.launch {
      val result = repository.pairDeviceByQr(qrPayload)
      onResult(result)
    }
  }

  fun markAllChatsAsRead() {
    viewModelScope.launch {
      repository.clearAllUnread()
    }
  }

  fun deleteChat(peerId: String) {
    viewModelScope.launch {
      repository.deleteChat(peerId)
    }
  }

  fun createNewChat(displayName: String, initialMessage: String = "") {
    viewModelScope.launch {
      val cleanName = displayName.trim()
      if (cleanName.isBlank()) return@launch
      val cleanId = "peer_" + cleanName.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(20) + "_" + System.currentTimeMillis().toString().takeLast(4)
      val newPeer = com.example.data.local.entity.PeerEntity(
        id = cleanId,
        username = cleanName.lowercase().replace(" ", "_"),
        displayName = cleanName,
        avatarIndex = (cleanId.hashCode() and 0x7FFFFFFF) % 6,
        isOnline = true,
        rssi = -65,
        protocol = "BT 5.0 (2.4 GHz)",
        hopCount = 1,
        distanceEstimatedMeters = 5.0,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = if (initialMessage.isNotBlank()) initialMessage else "New conversation started",
        lastMessageTimestamp = System.currentTimeMillis(),
        unreadCount = 0,
        publicKeyHex = "04" + cleanId.hashCode().toString(16).padEnd(62, 'F').take(64)
      )
      repository.insertPeerDirect(newPeer)
      if (initialMessage.isNotBlank()) {
        repository.sendMessage(cleanId, initialMessage)
      }
    }
  }

  // Radar Connection State & Actions
  private val _incomingRadarConnection = MutableStateFlow<Peer?>(null)
  val incomingRadarConnection: StateFlow<Peer?> = _incomingRadarConnection.asStateFlow()

  private val _pairingProgressPeer = MutableStateFlow<Peer?>(null)
  val pairingProgressPeer: StateFlow<Peer?> = _pairingProgressPeer.asStateFlow()

  private val _connectionFailedNotice = MutableStateFlow<String?>(null)
  val connectionFailedNotice: StateFlow<String?> = _connectionFailedNotice.asStateFlow()

  fun clearConnectionFailedNotice() {
    _connectionFailedNotice.value = null
  }

  fun dismissIncomingRadarConnection() {
    _incomingRadarConnection.value = null
  }

  fun cancelPairingRequest() {
    _pairingProgressPeer.value = null
  }

  fun requestRadarConnect(peer: Peer) {
    viewModelScope.launch {
      _pairingProgressPeer.value = peer
      repository.requestRadarConnect(peer)
    }
  }

  fun acceptRadarConnect(peer: Peer) {
    viewModelScope.launch {
      repository.acceptRadarConnect(peer)
      _incomingRadarConnection.value = null
      _pairingProgressPeer.value = null
      _selectedPeerId.value = peer.id
    }
  }

  fun retryOrFlushWaitingMessages(peerId: String) {
    viewModelScope.launch {
      repository.flushWaitingMessagesForPeer(peerId)
    }
  }

  fun verifyAndConnectPeer(peerId: String) {
    viewModelScope.launch {
      repository.setPeerOnlineStatus(peerId, true)
      repository.flushWaitingMessagesForPeer(peerId)
    }
  }

  fun simulateNearbyPhonePeer() {
    viewModelScope.launch {
      val randomSuffix = (1000..9999).random()
      val samplePhone = com.example.data.local.entity.PeerEntity(
        id = "phone_user_bluefi_$randomSuffix",
        username = "phone_alex_bluefi",
        displayName = "Alex (Bluefi Phone User)",
        avatarIndex = 3,
        isOnline = false,
        rssi = -46,
        protocol = "2.4 GHz Radar Direct",
        hopCount = 1,
        distanceEstimatedMeters = 2.4,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = "Discovered via Bluefi RF Radar",
        lastMessageTimestamp = System.currentTimeMillis(),
        unreadCount = 0,
        publicKeyHex = "04" + "alexphone".toByteArray().joinToString("") { "%02x".format(it) }.padEnd(62, 'F').take(64)
      )
      repository.insertPeerDirect(samplePhone)
    }
  }

  fun declineRadarConnect(peer: Peer) {
    viewModelScope.launch {
      repository.declineRadarConnect(peer)
      _incomingRadarConnection.value = null
      _pairingProgressPeer.value = null
      _connectionFailedNotice.value = "Device is not paired. Pairing request was declined!"
    }
  }
}
