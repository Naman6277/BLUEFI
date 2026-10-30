package com.example.data.model

data class Peer(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarIndex: Int = 0,
  val isOnline: Boolean = true,
  val rssi: Int = -65, // in dBm
  val protocol: String = "BT 5.0 (2.4 GHz)", // or "Wi-Fi Direct P2P"
  val hopCount: Int = 1, // 1 = direct hop, >1 = mesh relay up to 2.5 km
  val distanceEstimatedMeters: Double = 8.5,
  val lastSeenTimestamp: Long = System.currentTimeMillis(),
  val lastMessagePreview: String = "",
  val lastMessageTimestamp: Long = 0L,
  val unreadCount: Int = 0,
  val publicKeyHex: String = "3059301306072a8648ce3d020106082a8648ce3d03010703420004"
)
