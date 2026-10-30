package com.example.data.model

data class MeshPacket(
  val packetId: String,
  val messageId: String,
  val senderId: String,
  val receiverId: String,
  val packetIndex: Int,
  val totalPackets: Int,
  val payloadHex: String,
  val payloadBinary: String,
  val checksumHex: String,
  val hopCount: Int = 1,
  val relayNodes: List<String> = emptyList(),
  val carrierWave: String = "2.4 GHz RF (BT/Wi-Fi Direct)",
  val timestamp: Long = System.currentTimeMillis()
)
