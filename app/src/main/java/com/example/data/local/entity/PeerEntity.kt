package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
  @PrimaryKey val id: String,
  val username: String,
  val displayName: String,
  val avatarIndex: Int,
  val isOnline: Boolean,
  val rssi: Int,
  val protocol: String,
  val hopCount: Int,
  val distanceEstimatedMeters: Double,
  val lastSeenTimestamp: Long,
  val lastMessagePreview: String,
  val lastMessageTimestamp: Long,
  val unreadCount: Int,
  val publicKeyHex: String
)
