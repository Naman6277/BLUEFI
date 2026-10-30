package com.example.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Discovered BLE peer metadata from scan results.
 */
data class BluetoothDiscoveredPeer(
  val id: String,
  val name: String,
  val rssi: Int,
  val distanceMeters: Double,
  val lastSeenTimestamp: Long,
  val serviceData: String = "",
  val isBluefiPeer: Boolean = true,
  val hopCount: Int = 1
)

/**
 * Real-time status for Bluetooth hardware, scanning, and advertising.
 */
data class BluetoothDiscoveryState(
  val isBluetoothSupported: Boolean = true,
  val isBluetoothEnabled: Boolean = false,
  val isAdvertisingSupported: Boolean = false,
  val isScanning: Boolean = false,
  val isAdvertising: Boolean = false,
  val activeAdvPayload: String = "",
  val lastScanError: String? = null,
  val lastAdvertiseError: String? = null,
  val totalPacketsObserved: Long = 0L
)

/**
 * Handles Bluetooth device scanning and advertising for peer-to-peer discovery
 * using the Android Bluetooth API (BluetoothLeScanner and BluetoothLeAdvertiser).
 */
class BluetoothManager(private val context: Context) {

  companion object {
    private const val TAG = "BluefiBluetoothMgr"

    // Bluefi Mesh Service UUID (128-bit)
    val SERVICE_UUID: UUID = UUID.fromString("0000BFE1-0000-1000-8000-00805F9B34FB")
    val PARCEL_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)

    // Broadcast protocol prefix: "BF" (Bluefi Frame)
    private const val PROTOCOL_PREFIX = "BF"

    // Maximum discovery search duration (20 seconds) to prevent heavy battery drain
    const val MAX_DISCOVERY_DURATION_MS = 20_000L
  }

  // Use system BluetoothManager via fully qualified name
  private val systemBluetoothManager: android.bluetooth.BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager

  private val bluetoothAdapter: BluetoothAdapter? = systemBluetoothManager?.adapter
  private var bleScanner: BluetoothLeScanner? = null
  private var bleAdvertiser: BluetoothLeAdvertiser? = null

  private val scope = CoroutineScope(Dispatchers.IO)
  private var scanDutyCycleJob: kotlinx.coroutines.Job? = null

  private val _state = MutableStateFlow(
    BluetoothDiscoveryState(
      isBluetoothSupported = bluetoothAdapter != null,
      isBluetoothEnabled = bluetoothAdapter?.isEnabled == true,
      isAdvertisingSupported = bluetoothAdapter?.isMultipleAdvertisementSupported == true
    )
  )
  val state: StateFlow<BluetoothDiscoveryState> = _state.asStateFlow()

  private val _discoveredPeers = MutableStateFlow<List<BluetoothDiscoveredPeer>>(emptyList())
  val discoveredPeers: StateFlow<List<BluetoothDiscoveredPeer>> = _discoveredPeers.asStateFlow()

  var isBatterySaverMode: Boolean = false
    set(value) {
      field = value
      if (_state.value.isScanning) {
        stopScanning()
        startScanning()
      }
    }

  // Scanning callback implementation
  private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
      result?.let { handleScanResult(it) }
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
      results?.forEach { handleScanResult(it) }
    }

    override fun onScanFailed(errorCode: Int) {
      val errorDescription = when (errorCode) {
        SCAN_FAILED_ALREADY_STARTED -> "Scan failed: already started"
        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan failed: app registration failed"
        SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan failed: feature unsupported"
        SCAN_FAILED_INTERNAL_ERROR -> "Scan failed: internal error"
        else -> "Scan failed with error code: $errorCode"
      }
      Log.w(TAG, errorDescription)
      _state.value = _state.value.copy(
        isScanning = false,
        lastScanError = errorDescription
      )
    }
  }

  // Advertising callback implementation
  private val advertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
      Log.i(TAG, "BLE advertising successfully started with settings: $settingsInEffect")
      _state.value = _state.value.copy(
        isAdvertising = true,
        lastAdvertiseError = null
      )
    }

    override fun onStartFailure(errorCode: Int) {
      val errorDescription = when (errorCode) {
        ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising failed: already started"
        ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising failed: data exceeds 31-byte limit"
        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising failed: hardware does not support BLE advertising"
        ADVERTISE_FAILED_INTERNAL_ERROR -> "Advertising failed: internal error"
        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Advertising failed: maximum advertising instances reached"
        else -> "Advertising failed with error code: $errorCode"
      }
      Log.e(TAG, errorDescription)
      _state.value = _state.value.copy(
        isAdvertising = false,
        lastAdvertiseError = errorDescription
      )
    }
  }

  init {
    updateAdapterState()
  }

  /**
   * Refreshes the hardware support and enabled flags.
   */
  fun updateAdapterState() {
    val supported = bluetoothAdapter != null
    val enabled = bluetoothAdapter?.isEnabled == true
    val advSupported = bluetoothAdapter?.isMultipleAdvertisementSupported == true
    _state.value = _state.value.copy(
      isBluetoothSupported = supported,
      isBluetoothEnabled = enabled,
      isAdvertisingSupported = advSupported
    )
  }

  /**
   * Starts device scanning for peer discovery using [BluetoothLeScanner].
   *
   * @param filterByBluefiOnly If true, only scans for packets advertising [SERVICE_UUID].
   */
  fun startScanning(filterByBluefiOnly: Boolean = false) {
    if (!hasScanPermission()) {
      Log.w(TAG, "Cannot start BLE scan: missing BLUETOOTH_SCAN / LOCATION permissions")
      _state.value = _state.value.copy(lastScanError = "Missing Bluetooth Scan permissions")
      return
    }

    if (bluetoothAdapter?.isEnabled != true) {
      Log.w(TAG, "Cannot start BLE scan: Bluetooth is disabled")
      _state.value = _state.value.copy(lastScanError = "Bluetooth is disabled")
      return
    }

    try {
      bleScanner = bluetoothAdapter.bluetoothLeScanner
      if (bleScanner == null) {
        _state.value = _state.value.copy(lastScanError = "Bluetooth LE Scanner is unavailable")
        return
      }
      val scanMode = if (isBatterySaverMode) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY
      val scanSettings = ScanSettings.Builder()
        .setScanMode(scanMode)
        .build()

      val filters = mutableListOf<ScanFilter>()
      if (filterByBluefiOnly) {
        filters.add(
          ScanFilter.Builder()
            .setServiceUuid(PARCEL_UUID)
            .build()
        )
      }

      bleScanner?.startScan(filters, scanSettings, scanCallback)
      _state.value = _state.value.copy(
        isScanning = true,
        lastScanError = null
      )
      Log.i(TAG, "Bluetooth LE scanning started (duty cycle limited to 20s)")

      // DUTY-CYCLED DISCOVERY: Automatically cancel discovery after 20 seconds to eliminate battery drain
      scanDutyCycleJob?.cancel()
      scanDutyCycleJob = scope.launch {
        kotlinx.coroutines.delay(MAX_DISCOVERY_DURATION_MS)
        if (_state.value.isScanning) {
          Log.i(TAG, "Bluetooth discovery reached 20s duty cycle limit. Automatically stopping scan to conserve battery.")
          stopScanning()
        }
      }
    } catch (e: SecurityException) {
      Log.e(TAG, "SecurityException starting scan: ${e.message}")
      _state.value = _state.value.copy(
        isScanning = false,
        lastScanError = "SecurityException: ${e.message}"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Exception starting scan: ${e.message}")
      _state.value = _state.value.copy(
        isScanning = false,
        lastScanError = e.message
      )
    }
  }

  /**
   * Stops active BLE device scanning and cancels ongoing discovery.
   */
  fun stopScanning() {
    scanDutyCycleJob?.cancel()
    scanDutyCycleJob = null

    // Cancel classic Bluetooth adapter discovery if active (eliminates radio power drain)
    try {
      if (bluetoothAdapter?.isDiscovering == true) {
        bluetoothAdapter?.cancelDiscovery()
      }
    } catch (_: Exception) {}

    if (!hasScanPermission()) {
      _state.value = _state.value.copy(isScanning = false)
      return
    }

    try {
      bleScanner?.stopScan(scanCallback)
      _state.value = _state.value.copy(isScanning = false)
      Log.i(TAG, "Bluetooth LE scanning stopped")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException stopping scan: ${e.message}")
      _state.value = _state.value.copy(isScanning = false)
    } catch (e: Exception) {
      Log.w(TAG, "Exception stopping scan: ${e.message}")
      _state.value = _state.value.copy(isScanning = false)
    }
  }

  /**
   * Immediately terminates all ongoing Bluetooth discovery and scanning.
   */
  fun cancelAllDiscovery() {
    stopScanning()
  }

  /**
   * Starts device advertising to make this node discoverable by nearby peers
   * using [BluetoothLeAdvertiser].
   *
   * @param nodeId Unique identifier of the local user node.
   * @param displayName Human-readable display label.
   * @param isEncrypted Whether this node strictly uses E2EE AES-256-GCM.
   */
  fun startAdvertising(
    nodeId: String,
    displayName: String = "",
    isEncrypted: Boolean = true
  ) {
    if (!hasAdvertisePermission()) {
      Log.w(TAG, "Cannot start BLE advertising: missing BLUETOOTH_ADVERTISE permission")
      _state.value = _state.value.copy(lastAdvertiseError = "Missing Bluetooth Advertise permission")
      return
    }

    if (bluetoothAdapter?.isEnabled != true) {
      Log.w(TAG, "Cannot start BLE advertising: Bluetooth is disabled")
      _state.value = _state.value.copy(lastAdvertiseError = "Bluetooth is disabled")
      return
    }

    if (bluetoothAdapter.isMultipleAdvertisementSupported != true) {
      Log.w(TAG, "BLE advertising is not supported on this chipset")
      _state.value = _state.value.copy(lastAdvertiseError = "Hardware does not support BLE advertising")
      return
    }

    try {
      bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
      if (bleAdvertiser == null) {
        _state.value = _state.value.copy(lastAdvertiseError = "Bluetooth LE Advertiser is null")
        return
      }

      val advMode = if (isBatterySaverMode) AdvertiseSettings.ADVERTISE_MODE_LOW_POWER else AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
      val txPower = if (isBatterySaverMode) AdvertiseSettings.ADVERTISE_TX_POWER_LOW else AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
      val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(advMode)
        .setTxPowerLevel(txPower)
        .setConnectable(true)
        .setTimeout(0) // 0 = continuous advertisement until stopped
        .build()

      // Compact payload within BLE legacy 31-byte limit:
      // Format: "BF:" + nodeId (truncated) + ":" + (E for encrypted)
      val cleanId = nodeId.takeLast(8)
      val payloadString = "$PROTOCOL_PREFIX:$cleanId:${if (isEncrypted) "E" else "P"}"
      val serviceDataBytes = payloadString.toByteArray(StandardCharsets.UTF_8)

      val data = AdvertiseData.Builder()
        .setIncludeDeviceName(false) // Save space for UUID and Service Data
        .setIncludeTxPowerLevel(true)
        .addServiceUuid(PARCEL_UUID)
        .addServiceData(PARCEL_UUID, serviceDataBytes)
        .build()

      bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
      _state.value = _state.value.copy(
        activeAdvPayload = payloadString,
        lastAdvertiseError = null
      )
      Log.i(TAG, "Requested BLE advertising with payload: $payloadString")
    } catch (e: SecurityException) {
      Log.e(TAG, "SecurityException starting advertising: ${e.message}")
      _state.value = _state.value.copy(
        isAdvertising = false,
        lastAdvertiseError = "SecurityException: ${e.message}"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Exception starting advertising: ${e.message}")
      _state.value = _state.value.copy(
        isAdvertising = false,
        lastAdvertiseError = e.message
      )
    }
  }

  /**
   * Stops active BLE advertising.
   */
  fun stopAdvertising() {
    if (!hasAdvertisePermission()) {
      _state.value = _state.value.copy(isAdvertising = false)
      return
    }

    try {
      bleAdvertiser?.stopAdvertising(advertiseCallback)
      _state.value = _state.value.copy(
        isAdvertising = false,
        activeAdvPayload = ""
      )
      Log.i(TAG, "Bluetooth LE advertising stopped")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException stopping advertising: ${e.message}")
      _state.value = _state.value.copy(isAdvertising = false)
    } catch (e: Exception) {
      Log.w(TAG, "Exception stopping advertising: ${e.message}")
      _state.value = _state.value.copy(isAdvertising = false)
    }
  }

  /**
   * Processes an incoming BLE scan result and converts it into a [BluetoothDiscoveredPeer].
   */
  private fun handleScanResult(result: ScanResult) {
    val device = result.device
    val rssi = result.rssi
    val scanRecord = result.scanRecord

    // Check if device advertises the Bluefi service UUID or service data
    val hasBluefiService = scanRecord?.serviceUuids?.any { it == PARCEL_UUID } == true
    val serviceDataBytes = scanRecord?.getServiceData(PARCEL_UUID)
    val serviceDataStr = serviceDataBytes?.let { String(it, StandardCharsets.UTF_8) } ?: ""

    val deviceName = getDeviceNameSafe(device, serviceDataStr)
    val estimatedDistance = calculateDistance(rssi, result.txPower.takeIf { it != 127 } ?: -59)

    scope.launch {
      val currentList = _discoveredPeers.value.toMutableList()
      val existingIndex = currentList.indexOfFirst { it.id == device.address }

      val peer = BluetoothDiscoveredPeer(
        id = device.address,
        name = deviceName,
        rssi = rssi,
        distanceMeters = estimatedDistance,
        lastSeenTimestamp = System.currentTimeMillis(),
        serviceData = serviceDataStr,
        isBluefiPeer = hasBluefiService || serviceDataStr.startsWith(PROTOCOL_PREFIX),
        hopCount = 1
      )

      if (existingIndex >= 0) {
        currentList[existingIndex] = peer
      } else {
        currentList.add(peer)
      }

      _discoveredPeers.value = currentList
      _state.value = _state.value.copy(
        totalPacketsObserved = _state.value.totalPacketsObserved + 1
      )
    }
  }

  /**
   * Safely retrieves device name handling Android 12+ permissions.
   */
  private fun getDeviceNameSafe(device: android.bluetooth.BluetoothDevice, fallbackServiceData: String): String {
    if (fallbackServiceData.startsWith(PROTOCOL_PREFIX)) {
      val parts = fallbackServiceData.split(":")
      if (parts.size >= 2 && parts[1].isNotBlank()) {
        return "Node_${parts[1]}"
      }
    }

    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        device.alias ?: device.name ?: "Peer_${device.address.takeLast(4)}"
      } else {
        device.name ?: "Peer_${device.address.takeLast(4)}"
      }
    } catch (_: SecurityException) {
      "Peer_${device.address.takeLast(4)}"
    }
  }

  /**
   * Converts a [BluetoothDiscoveredPeer] into the application's domain [Peer] model.
   */
  fun BluetoothDiscoveredPeer.toDomainPeer(): Peer {
    return Peer(
      id = id,
      username = name.lowercase().replace(" ", "_"),
      displayName = name,
      avatarIndex = (id.hashCode() and 0x7FFFFFFF) % 6,
      isOnline = true,
      rssi = rssi,
      protocol = "BT 5.0 (2.4 GHz)",
      hopCount = hopCount,
      distanceEstimatedMeters = distanceMeters,
      lastSeenTimestamp = lastSeenTimestamp,
      lastMessagePreview = if (isBluefiPeer) "P2P Beacon (Encrypted)" else "BLE Beacon detected",
      publicKeyHex = "04${id.replace(":", "").padEnd(62, 'A').take(64)}"
    )
  }

  /**
   * Estimates distance in meters based on RSSI and reference TxPower.
   */
  fun calculateDistance(rssi: Int, txPower: Int = -59): Double {
    if (rssi == 0) return -1.0
    val ratio = rssi * 1.0 / txPower
    return if (ratio < 1.0) {
      Math.pow(ratio, 10.0)
    } else {
      (0.89976) * Math.pow(ratio, 7.7095) + 0.111
    }
  }

  /**
   * Clears currently cached discovered peers list.
   */
  fun clearDiscoveredPeers() {
    _discoveredPeers.value = emptyList()
  }

  // Permission checks
  fun hasScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun hasAdvertisePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    } else {
      true // On API < 31, BLUETOOTH and BLUETOOTH_ADMIN install permissions suffice
    }
  }

  fun hasConnectPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }
}
