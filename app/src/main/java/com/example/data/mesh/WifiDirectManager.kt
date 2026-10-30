package com.example.data.mesh

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State model for Wi-Fi Direct P2P hardware and connections.
 */
data class WifiDirectState(
  val isWifiP2pEnabled: Boolean = true,
  val isSearching: Boolean = false,
  val connectedDeviceCount: Int = 0,
  val isGroupOwner: Boolean = false,
  val groupOwnerAddress: String = "",
  val localDeviceName: String = "Bluefi Node",
  val discoveredDevices: List<WifiP2pDevice> = emptyList(),
  val channelStatus: String = "Wi-Fi Direct 2.4/5GHz P2P Ready"
)

/**
 * Robust Wi-Fi Direct Manager using [WifiP2pManager].
 * Handles discovery, peer pairing, group negotiation, and socket IP retrieval across
 * Samsung, Xiaomi, Oppo, Vivo, Motorola, and Transsion devices.
 */
class WifiDirectManager(
  private val context: Context,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
  companion object {
    private const val TAG = "BluefiWifiDirect"
    // Maximum Wi-Fi Direct discovery duration (20 seconds) to prevent heavy battery drain
    const val MAX_DISCOVERY_DURATION_MS = 20_000L
  }

  private val wifiP2pManager: WifiP2pManager? by lazy {
    try {
      context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    } catch (e: Exception) {
      null
    }
  }

  private var channel: WifiP2pManager.Channel? = null
  private var receiverRegistered = false
  private var discoveryDutyCycleJob: Job? = null

  private val _state = MutableStateFlow(WifiDirectState())
  val state: StateFlow<WifiDirectState> = _state.asStateFlow()

  // Listener callback when Wi-Fi Direct establishes an IP link with a group owner
  var onGroupConnectionEstablished: ((groupOwnerIp: String, isOwner: Boolean) -> Unit)? = null

  private val p2pReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      when (action) {
        WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
          val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
          val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
          _state.value = _state.value.copy(
            isWifiP2pEnabled = isEnabled,
            channelStatus = if (isEnabled) "Wi-Fi Direct Ready" else "Wi-Fi Direct Disabled"
          )
        }

        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
          requestPeers()
        }

        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
          @Suppress("DEPRECATION")
          val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
          if (networkInfo?.isConnected == true) {
            requestConnectionInfo()
          } else {
            _state.value = _state.value.copy(
              connectedDeviceCount = 0,
              groupOwnerAddress = "",
              isGroupOwner = false,
              channelStatus = "Wi-Fi Direct Standby"
            )
          }
        }

        WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
          @Suppress("DEPRECATION")
          val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
          if (device != null) {
            _state.value = _state.value.copy(localDeviceName = device.deviceName ?: "Bluefi Node")
          }
        }
      }
    }
  }

  init {
    initializeP2p()
  }

  private fun initializeP2p() {
    try {
      channel = wifiP2pManager?.initialize(context, context.mainLooper) {
        Log.w(TAG, "Wi-Fi P2P Channel disconnected, re-initializing...")
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
      }
      registerReceiver()
    } catch (e: Exception) {
      Log.w(TAG, "Wi-Fi P2P initialization notice: ${e.message}")
    }
  }

  private fun registerReceiver() {
    if (receiverRegistered) return
    try {
      val filter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(p2pReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(p2pReceiver, filter)
      }
      receiverRegistered = true
    } catch (e: Exception) {
      Log.w(TAG, "Error registering P2P receiver: ${e.message}")
    }
  }

  /**
   * Discovers nearby Wi-Fi Direct devices.
   * Includes automatic retry handling for BUSY status (code 2) common on Samsung/Xiaomi.
   */
  @SuppressLint("MissingPermission")
  fun discoverPeers(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
    if (channel == null || !receiverRegistered) {
      initializeP2p()
    }
    val ch = channel ?: return
    try {
      _state.value = _state.value.copy(isSearching = true)

      // DUTY-CYCLED DISCOVERY: Automatically cancel discovery after 20 seconds to eliminate battery drain
      discoveryDutyCycleJob?.cancel()
      discoveryDutyCycleJob = scope.launch {
        delay(MAX_DISCOVERY_DURATION_MS)
        if (_state.value.isSearching) {
          Log.i(TAG, "Wi-Fi Direct discovery reached 20s duty cycle limit. Automatically stopping discovery to conserve battery.")
          stopDiscovery()
        }
      }

      wifiP2pManager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          Log.i(TAG, "Wi-Fi Direct discoverPeers succeeded (duty cycle limited to 20s)")
          _state.value = _state.value.copy(channelStatus = "Scanning 2.4/5GHz Wi-Fi Direct...")
          onSuccess()
        }

        override fun onFailure(reasonCode: Int) {
          Log.w(TAG, "discoverPeers failed with code $reasonCode")
          discoveryDutyCycleJob?.cancel()
          _state.value = _state.value.copy(isSearching = false)
          // Reason 2 = BUSY: Retry once after a brief delay
          if (reasonCode == WifiP2pManager.BUSY) {
            scope.launch {
              delay(1500)
              try {
                channel?.let { retryCh ->
                  wifiP2pManager?.discoverPeers(retryCh, null)
                }
              } catch (_: Exception) {}
            }
          }
          onFailure(reasonCode)
        }
      })
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException in discoverPeers: ${e.message}")
      discoveryDutyCycleJob?.cancel()
      _state.value = _state.value.copy(isSearching = false)
    } catch (e: Exception) {
      Log.w(TAG, "Exception in discoverPeers: ${e.message}")
      discoveryDutyCycleJob?.cancel()
      _state.value = _state.value.copy(isSearching = false)
    }
  }

  /**
   * Fetches latest discovered Wi-Fi Direct peer list.
   */
  @SuppressLint("MissingPermission")
  fun requestPeers() {
    val ch = channel ?: return
    try {
      wifiP2pManager?.requestPeers(ch) { peers: WifiP2pDeviceList? ->
        val list = peers?.deviceList?.toList() ?: emptyList()
        Log.d(TAG, "Discovered Wi-Fi Direct peers: ${list.size}")
        _state.value = _state.value.copy(
          discoveredDevices = list,
          channelStatus = if (list.isNotEmpty()) "${list.size} Wi-Fi Direct nodes in range" else _state.value.channelStatus
        )
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "requestPeers SecurityException: ${e.message}")
    } catch (e: Exception) {
      Log.d(TAG, "requestPeers notice: ${e.message}")
    }
  }

  /**
   * Connects to a target Wi-Fi Direct peer using Push-Button-Configuration (PBC).
   */
  @SuppressLint("MissingPermission")
  fun connectToDevice(deviceAddress: String, onResult: (Boolean) -> Unit = {}) {
    if (channel == null || !receiverRegistered) {
      initializeP2p()
    }
    val ch = channel ?: return
    try {
      val config = WifiP2pConfig().apply {
        this.deviceAddress = deviceAddress
        wps.setup = WpsInfo.PBC
      }

      wifiP2pManager?.connect(ch, config, object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          Log.i(TAG, "Initiated Wi-Fi Direct connection to $deviceAddress")
          // Immediately cancel discovery as soon as socket connection handshake begins
          stopDiscovery()
          onResult(true)
        }

        override fun onFailure(reason: Int) {
          Log.w(TAG, "Wi-Fi Direct connect failed: reason=$reason")
          onResult(false)
        }
      })
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to Wi-Fi Direct device: ${e.message}")
      onResult(false)
    }
  }

  /**
   * Requests connection details to obtain Group Owner IP and update socket endpoints.
   */
  fun requestConnectionInfo() {
    val ch = channel ?: return
    try {
      wifiP2pManager?.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
        if (info != null && info.groupFormed) {
          val ownerIp = info.groupOwnerAddress?.hostAddress ?: ""
          val isOwner = info.isGroupOwner
          Log.i(TAG, "Wi-Fi Direct Group established: owner=$ownerIp, isOwner=$isOwner")

          // Immediately cancel discovery as soon as socket connection is established
          stopDiscovery()

          _state.value = _state.value.copy(
            connectedDeviceCount = 1,
            isGroupOwner = isOwner,
            groupOwnerAddress = ownerIp,
            channelStatus = if (isOwner) "P2P Group Owner (Host: $ownerIp)" else "Connected to Group Owner ($ownerIp)"
          )

          onGroupConnectionEstablished?.invoke(ownerIp, isOwner)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "requestConnectionInfo notice: ${e.message}")
    }
  }

  /**
   * Stops active Wi-Fi Direct discovery.
   */
  fun stopDiscovery() {
    discoveryDutyCycleJob?.cancel()
    discoveryDutyCycleJob = null
    try {
      channel?.let { ch ->
        wifiP2pManager?.stopPeerDiscovery(ch, null)
        _state.value = _state.value.copy(isSearching = false)
      }
    } catch (_: Exception) {}
  }

  /**
   * Disconnects and removes any active Wi-Fi Direct group.
   */
  fun removeGroup() {
    try {
      channel?.let { ch ->
        wifiP2pManager?.removeGroup(ch, null)
      }
    } catch (_: Exception) {}
  }

  /**
   * Clears all registered local Wi-Fi Direct services for Invisible Mode.
   */
  fun clearLocalServices(onResult: (Boolean) -> Unit = {}) {
    val ch = channel ?: return
    try {
      wifiP2pManager?.clearLocalServices(ch, object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          Log.i(TAG, "Wi-Fi Direct clearLocalServices succeeded (Invisible Mode)")
          onResult(true)
        }
        override fun onFailure(reason: Int) {
          Log.w(TAG, "Wi-Fi Direct clearLocalServices failed: reason=$reason")
          onResult(false)
        }
      })
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException in clearLocalServices: ${e.message}")
      onResult(false)
    } catch (e: Exception) {
      Log.w(TAG, "Exception in clearLocalServices: ${e.message}")
      onResult(false)
    }
  }

  /**
   * Registers local Wi-Fi Direct presence service for Visible Mode.
   */
  fun registerLocalService(username: String, deviceId: String, onResult: (Boolean) -> Unit = {}) {
    val ch = channel ?: return
    try {
      val record = HashMap<String, String>().apply {
        put("user", username)
        put("id", deviceId)
        put("node", "bluefi_p2p")
      }
      val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
        "BluefiNode",
        "_bluefi._tcp",
        record
      )
      wifiP2pManager?.addLocalService(ch, serviceInfo, object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          Log.i(TAG, "Wi-Fi Direct local presence service registered (Visible Mode)")
          onResult(true)
        }
        override fun onFailure(reason: Int) {
          Log.w(TAG, "addLocalService failed: $reason")
          onResult(false)
        }
      })
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException in registerLocalService: ${e.message}")
      onResult(false)
    } catch (e: Exception) {
      Log.w(TAG, "Exception in registerLocalService: ${e.message}")
      onResult(false)
    }
  }

  fun cleanup() {
    stopDiscovery()
    removeGroup()
    clearLocalServices()
    if (receiverRegistered) {
      try {
        context.unregisterReceiver(p2pReceiver)
      } catch (e: IllegalArgumentException) {
        Log.d(TAG, "p2pReceiver already unregistered: ${e.message}")
      } catch (e: Exception) {
        Log.d(TAG, "p2pReceiver unregister notice: ${e.message}")
      }
      receiverRegistered = false
    }
  }
}
