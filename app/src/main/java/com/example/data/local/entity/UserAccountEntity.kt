package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Multi-account local storage entity supporting up to 5 distinct user profiles permanently on device.
 * Stores encrypted security questions and answers for password recovery.
 */
@Entity(tableName = "user_accounts")
data class UserAccountEntity(
  @PrimaryKey
  val userId: String,
  val username: String,
  val passwordHash: String,
  val avatarIndex: Int = 0,
  val customAvatarUri: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val lastLoginAt: Long = System.currentTimeMillis(),
  val isActiveSession: Boolean = false,
  // Encrypted / hashed security questions and answers for password recovery
  val securityQuestion1: String = "",
  val securityAnswer1: String = "",
  val securityQuestion2: String = "",
  val securityAnswer2: String = "",
  val securityQuestion3: String = "",
  val securityAnswer3: String = ""
)
