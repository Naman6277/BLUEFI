package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
  @Query("SELECT * FROM peers ORDER BY isOnline DESC, lastMessageTimestamp DESC")
  fun getAllPeers(): Flow<List<PeerEntity>>

  @Query("SELECT * FROM peers WHERE id = :peerId LIMIT 1")
  fun getPeerById(peerId: String): Flow<PeerEntity?>

  @Query("SELECT * FROM peers WHERE id = :peerId LIMIT 1")
  suspend fun getPeerDirect(peerId: String): PeerEntity?

  @Query("SELECT * FROM peers WHERE username = :username LIMIT 1")
  suspend fun getPeerByUsername(username: String): PeerEntity?

  @Query("SELECT * FROM peers")
  suspend fun getAllPeersListDirect(): List<PeerEntity>

  @Query("DELETE FROM peers WHERE lastMessagePreview LIKE '%Beacon%' OR lastMessagePreview LIKE '%Discovered via BLE%' OR displayName LIKE 'Peer_%' OR displayName LIKE 'peer_%' OR displayName LIKE 'MeshNode_%' OR id LIKE 'peer_node_mesh_beta%'")
  suspend fun purgeUnidentifiedRawBeacons()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPeer(peer: PeerEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPeers(peers: List<PeerEntity>)

  @Update
  suspend fun updatePeer(peer: PeerEntity)

  @Query("UPDATE peers SET lastMessagePreview = :preview, lastMessageTimestamp = :time WHERE id = :peerId")
  suspend fun updateLastMessage(peerId: String, preview: String, time: Long)

  @Query("UPDATE peers SET isOnline = :isOnline WHERE id = :peerId")
  suspend fun updateOnlineStatus(peerId: String, isOnline: Boolean)

  @Query("UPDATE peers SET unreadCount = 0 WHERE id = :peerId")
  suspend fun clearUnread(peerId: String)

  @Query("DELETE FROM peers WHERE id = :peerId")
  suspend fun deletePeer(peerId: String)

  @Query("DELETE FROM peers WHERE id IN (:peerIds)")
  suspend fun deletePeersByIds(peerIds: List<String>)

  @Query("DELETE FROM peers")
  suspend fun deleteAllPeers()
}
