package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey val id: String,
  val conversationPeerId: String,
  val senderId: String,
  val senderName: String,
  val isFromMe: Boolean,
  val text: String,
  val binaryForm: String,
  val cipherTextHex: String,
  val packetCount: Int,
  val mediaType: String,
  val mediaContent: String,
  val timestamp: Long,
  val burnsInTimestamp: Long,
  val isBurnOnRead: Boolean,
  val wasRead: Boolean,
  val isEncrypted: Boolean,
  val status: String
)
