package com.example.data.mesh

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Radar Visibility & Privacy Manager.
 * Controls whether this device is discoverable on nearby radars.
 *
 * When "Invisible Mode" is ENABLED:
 *  - Stops Bluetooth LE advertising (stopAdvertising())
 *  - Clears Wi-Fi Direct local services (clearLocalServices())
 *  - Suppresses outgoing presence BEACON broadcasts
 *  - User profile, avatar, and device name do NOT appear on other nearby phones' radar screens
 *  - User can still view nearby devices (passive discovery continues)
 *
 * When "Visible to Nearby Devices" (Default) is ENABLED:
 *  - Broadcasts local Wi-Fi Direct and Bluetooth metadata safely with user consent
 *  - Resumes Bluetooth LE advertising (startAdvertising())
 *  - Registers local Wi-Fi Direct presence service (registerLocalService())
 */
class RadarVisibilityManager(
  private val bluetoothMeshManager: BluetoothMeshManager,
  private val wifiDirectManager: WifiDirectManager
) {
  private val TAG = "RadarVisibilityMgr"

  private val _isRadarVisible = MutableStateFlow(true)
  val isRadarVisible: StateFlow<Boolean> = _isRadarVisible.asStateFlow()

  /**
   * Applies radar visibility change across Bluetooth LE and Wi-Fi Direct hardware.
   */
  fun setRadarVisibility(
    visible: Boolean,
    userId: String,
    username: String,
    isEncrypted: Boolean = true,
    onPresenceBroadcast: ((Boolean) -> Unit)? = null
  ) {
    _isRadarVisible.value = visible
    Log.i(TAG, "Radar Visibility updated: ${if (visible) "VISIBLE TO NEARBY DEVICES" else "INVISIBLE MODE (GHOST)"}")

    if (!visible) {
      // 1. INVISIBLE MODE: Stop Bluetooth advertising
      bluetoothMeshManager.stopAdvertising()
      Log.i(TAG, "Bluetooth advertising stopped for Invisible Mode")

      // 2. Unregister local Wi-Fi Direct services
      wifiDirectManager.clearLocalServices()
      Log.i(TAG, "Wi-Fi Direct local services cleared for Invisible Mode")

      // 3. Prevent user's profile and device broadcast from being transmitted
      onPresenceBroadcast?.invoke(false)
    } else {
      // 1. VISIBLE MODE: Broadcast local Bluetooth metadata safely with user consent
      bluetoothMeshManager.startAdvertising(
        nodeId = userId,
        displayName = username,
        isEncrypted = isEncrypted
      )
      Log.i(TAG, "Bluetooth advertising started with consent for Visible Mode")

      // 2. Broadcast local Wi-Fi Direct metadata safely with user consent
      wifiDirectManager.registerLocalService(
        username = username,
        deviceId = userId
      )
      Log.i(TAG, "Wi-Fi Direct presence service registered for Visible Mode")

      // 3. Notify presence broadcast
      onPresenceBroadcast?.invoke(true)
    }
  }

  fun isVisible(): Boolean = _isRadarVisible.value
}
