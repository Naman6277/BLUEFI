package com.example.data.model

data class ChatMessage(
  val id: String,
  val conversationPeerId: String,
  val senderId: String,
  val senderName: String,
  val isFromMe: Boolean,
  val text: String,
  val binaryForm: String,
  val cipherTextHex: String,
  val packetCount: Int = 1,
  val packets: List<MeshPacket> = emptyList(),
  val mediaType: String = "text", // "text", "sticker", "image"
  val mediaContent: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val burnsInTimestamp: Long = 0L, // 0 = no timer or calculated based on rule
  val isBurnOnRead: Boolean = false,
  val wasRead: Boolean = false,
  val isEncrypted: Boolean = true,
  val status: MessageDeliveryStatus = MessageDeliveryStatus.DELIVERED
)

enum class MessageDeliveryStatus {
  MUXING,
  TRANSMITTING_2_4GHZ,
  RELAYED_MESH,
  WAITING_IN_QUEUE,
  DELIVERED,
  READ
}
