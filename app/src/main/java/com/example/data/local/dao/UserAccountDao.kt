package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {

  @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
  fun getAllAccounts(): Flow<List<UserAccountEntity>>

  @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
  suspend fun getAllAccountsDirect(): List<UserAccountEntity>

  @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
  suspend fun getAccountById(userId: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE isActiveSession = 1 LIMIT 1")
  suspend fun getActiveAccount(): UserAccountEntity?

  @Query("SELECT COUNT(*) FROM user_accounts")
  suspend fun getAccountCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAccount(account: UserAccountEntity)

  @Update
  suspend fun updateAccount(account: UserAccountEntity)

  @Query("UPDATE user_accounts SET isActiveSession = 0")
  suspend fun clearActiveSessions()

  @Query("UPDATE user_accounts SET isActiveSession = 1, lastLoginAt = :timestamp WHERE userId = :userId")
  suspend fun setActiveSession(userId: String, timestamp: Long = System.currentTimeMillis())

  @Query("DELETE FROM user_accounts WHERE userId = :userId")
  suspend fun deleteAccount(userId: String)
}
