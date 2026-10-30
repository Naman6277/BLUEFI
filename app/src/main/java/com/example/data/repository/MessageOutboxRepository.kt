package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.MessageEntity
import com.example.data.model.ChatMessage
import com.example.data.model.MessageDeliveryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * MessageOutboxRepository:
 * Manages the Store-and-Forward Outbox "Waiting Area" queue in local Room DB.
 *
 * States:
 * - STATUS_WAITING (In Waiting Area / Outbox)
 * - STATUS_SENT (Delivered to peer socket)
 * - STATUS_DELIVERED (Peer acknowledged receipt)
 *
 * Queueing Logic:
 * 1. Inserts outgoing message marked as STATUS_WAITING.
 * 2. Attempts transmission over active socket.
 * 3. On ACK/success -> updates status to STATUS_SENT / STATUS_DELIVERED.
 * 4. On disconnect/failure -> remains in STATUS_WAITING (never deleted or dropped).
 *
 * Auto-Sync on Reconnect:
 * Whenever P2P socket connects, queries all STATUS_WAITING messages for that peer,
 * sequentially transmits them, and updates to STATUS_SENT.
 */
class MessageOutboxRepository(
  private val messageDao: MessageDao
) {
  companion object {
    private const val TAG = "MessageOutboxRepo"
    const val STATUS_WAITING = "WAITING_IN_QUEUE"
    const val STATUS_SENT = "DELIVERED"
    const val STATUS_DELIVERED = "READ"
  }

  suspend fun queueMessageInOutbox(message: MessageEntity) {
    val queued = message.copy(status = STATUS_WAITING)
    messageDao.insertMessage(queued)
    Log.d(TAG, "Queued message ${message.id} in Outbox Waiting Area for peer ${message.conversationPeerId}")
  }

  suspend fun markMessageSent(messageId: String) {
    messageDao.updateMessageStatus(messageId, STATUS_SENT)
    Log.d(TAG, "Marked message $messageId as STATUS_SENT")
  }

  suspend fun markMessageDelivered(messageId: String) {
    messageDao.updateMessageStatus(messageId, STATUS_DELIVERED)
    Log.d(TAG, "Marked message $messageId as STATUS_DELIVERED")
  }

  suspend fun getWaitingMessagesForPeer(peerId: String): List<MessageEntity> {
    return messageDao.getWaitingMessagesForPeer(peerId)
  }

  fun getWaitingCountForPeer(peerId: String): Flow<Int> {
    return messageDao.getWaitingMessageCountForPeer(peerId)
  }

  /**
   * Automatic Outbox Flushing on Reconnection:
   * Sequentially flushes all queued messages from the Waiting Area over the restored socket.
   */
  suspend fun flushOutboxOnPeerConnected(
    peerId: String,
    transmitter: suspend (MessageEntity) -> Boolean
  ): Int {
    val pending = getWaitingMessagesForPeer(peerId)
    if (pending.isEmpty()) {
      Log.d(TAG, "Outbox Waiting Area is empty for peer $peerId")
      return 0
    }

    Log.i(TAG, "Flushing ${pending.size} messages from Waiting Area for peer $peerId")
    var flushedCount = 0

    for (msg in pending) {
      val success = try {
        transmitter(msg)
      } catch (e: Exception) {
        Log.e(TAG, "Error transmitting queued message ${msg.id}: ${e.message}")
        false
      }

      if (success) {
        markMessageSent(msg.id)
        flushedCount++
      } else {
        Log.w(TAG, "Transmission failed for message ${msg.id}. Keeping in Waiting Area.")
        break // Stop and preserve order if socket drops again
      }
    }

    return flushedCount
  }
}
