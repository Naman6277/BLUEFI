package com.example.data.model

import com.example.ui.theme.BluefiThemeOption

enum class RetentionRule(val label: String) {
  RETENTION_24_HOURS("Delete after 24 hours"),
  BURN_ON_READ("15-second burn (self-destruct)"),
  PERMANENT("Permanent local store")
}

data class UserSettings(
  val userId: String = "node_local_01",
  val username: String = "CyberGhost",
  val avatarIndex: Int = 1,
  val customAvatarUri: String = "",
  val theme: BluefiThemeOption = BluefiThemeOption.DARK_FAST_BLUE,
  val retentionRule: RetentionRule = RetentionRule.RETENTION_24_HOURS,
  val isBluetoothMeshEnabled: Boolean = true,
  val isWifiDirectEnabled: Boolean = true,
  val isRadarVisible: Boolean = true, // Radar Privacy: true = Visible to Nearby Devices, false = Invisible Mode (Ghost Mode)
  val isRelayNodeActive: Boolean = true, // Acts as relay node extending mesh up to 2.5km
  val rfFrequencyWave: String = "2.4 GHz ISM",
  val firewallProtected: Boolean = true,
  val e2eeStrictEnabled: Boolean = true,
  val mediaStorageBaseFolder: String = "/Bluefi/Chats",
  val isBatterySaverMode: Boolean = false
)
