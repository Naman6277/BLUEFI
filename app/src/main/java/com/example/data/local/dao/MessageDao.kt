package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
  @Query("SELECT * FROM messages WHERE conversationPeerId = :peerId ORDER BY timestamp ASC")
  fun getMessagesForPeer(peerId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 50")
  fun getRecentMessages(): Flow<List<MessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Update
  suspend fun updateMessage(message: MessageEntity)

  @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
  suspend fun getMessageById(messageId: String): MessageEntity?

  @Query("UPDATE messages SET status = :status WHERE id = :messageId")
  suspend fun updateMessageStatus(messageId: String, status: String)

  @Query("SELECT * FROM messages WHERE conversationPeerId = :peerId AND status = 'WAITING_IN_QUEUE' ORDER BY timestamp ASC")
  suspend fun getWaitingMessagesForPeer(peerId: String): List<MessageEntity>

  @Query("SELECT COUNT(*) FROM messages WHERE conversationPeerId = :peerId AND status = 'WAITING_IN_QUEUE'")
  fun getWaitingMessageCountForPeer(peerId: String): Flow<Int>

  @Query("DELETE FROM messages WHERE text LIKE '%Handshake Verified%' OR text LIKE '%Scan Verified%' OR text LIKE '%AES-256 shared key active%'")
  suspend fun purgeHandshakeSystemMessages()

  @Query("DELETE FROM messages WHERE id = :messageId")
  suspend fun deleteMessageById(messageId: String)

  @Query("DELETE FROM messages WHERE conversationPeerId = :peerId")
  suspend fun deleteMessagesForPeer(peerId: String)

  @Query("UPDATE messages SET conversationPeerId = :toPeerId WHERE conversationPeerId = :fromPeerId")
  suspend fun migrateConversationMessages(fromPeerId: String, toPeerId: String)

  @Query("DELETE FROM messages WHERE burnsInTimestamp > 0 AND burnsInTimestamp <= :currentTime")
  suspend fun purgeExpiredMessages(currentTime: Long)

  @Query("UPDATE messages SET burnsInTimestamp = :burnTimestamp, isBurnOnRead = 1 WHERE (burnsInTimestamp == 0 OR burnsInTimestamp > :burnTimestamp)")
  suspend fun applyBurnFuseToExistingMessages(burnTimestamp: Long)

  @Query("UPDATE messages SET burnsInTimestamp = :burnTimestamp, isBurnOnRead = 1 WHERE conversationPeerId = :peerId AND (burnsInTimestamp == 0 OR burnsInTimestamp < :burnTimestamp)")
  suspend fun applyBurnFuseToPeerMessages(peerId: String, burnTimestamp: Long)

  @Query("UPDATE messages SET burnsInTimestamp = 0, isBurnOnRead = 0")
  suspend fun clearAllBurnFuses()

  @Query("UPDATE messages SET burnsInTimestamp = timestamp + 86400000, isBurnOnRead = 0")
  suspend fun apply24HourExpirationToExistingMessages()

  @Query("UPDATE messages SET wasRead = 1 WHERE conversationPeerId = :peerId")
  suspend fun markPeerMessagesAsReadInDb(peerId: String)

  @Query("DELETE FROM messages WHERE isBurnOnRead = 1 AND wasRead = 1 AND isFromMe = 0")
  suspend fun purgeBurnOnReadMessages()

  @Query("DELETE FROM messages")
  suspend fun deleteAllMessages()
}
