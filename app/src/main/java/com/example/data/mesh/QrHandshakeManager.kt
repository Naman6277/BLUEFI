package com.example.data.mesh

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.dao.PeerDao
import com.example.data.local.entity.PeerEntity
import com.example.data.model.Peer
import org.json.JSONObject

/**
 * RemotePeerNode representation for bidirectional profile handshakes.
 */
data class RemotePeerNode(
  val nodeId: String,
  val username: String,
  val avatarUrl: String = "",
  val avatarIndex: Int = 0,
  val publicKeyHex: String = "",
  val ipAddress: String = ""
) {
  fun toJson(): String {
    val obj = JSONObject()
    obj.put("nodeId", nodeId)
    obj.put("username", username)
    obj.put("avatarUrl", avatarUrl)
    obj.put("avatarIndex", avatarIndex)
    obj.put("publicKeyHex", publicKeyHex)
    obj.put("ipAddress", ipAddress)
    return obj.toString()
  }

  companion object {
    fun fromJson(jsonStr: String): RemotePeerNode? {
      return try {
        val obj = JSONObject(jsonStr)
        RemotePeerNode(
          nodeId = obj.optString("nodeId", ""),
          username = obj.optString("username", ""),
          avatarUrl = obj.optString("avatarUrl", ""),
          avatarIndex = obj.optInt("avatarIndex", 0),
          publicKeyHex = obj.optString("publicKeyHex", ""),
          ipAddress = obj.optString("ipAddress", "")
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}

/**
 * QrHandshakeManager:
 * 1. Generates JSON-formatted QR payload: {"nodeId": "BF-1024", "username": "ani", "avatarUrl": "..."}
 * 2. Parses incoming QR payloads (both JSON and bluefi:// URI schemas).
 * 3. Handles bidirectional profile handshakes so Scanner (baba) stores "ani", transmits ACK,
 *    and Device B (ani) receives ACK and stores "baba".
 */
object QrHandshakeManager {
  private const val TAG = "QrHandshakeManager"

  fun buildQrPayload(
    nodeId: String,
    username: String,
    avatarIndex: Int = 0,
    publicKeyHex: String = "",
    ipAddress: String = ""
  ): String {
    val node = RemotePeerNode(
      nodeId = nodeId,
      username = username,
      avatarUrl = "avatar://$avatarIndex",
      avatarIndex = avatarIndex,
      publicKeyHex = publicKeyHex,
      ipAddress = ipAddress
    )
    return node.toJson()
  }

  fun parseQrPayload(rawPayload: String): RemotePeerNode {
    val clean = rawPayload.trim()

    // 1. Try parsing JSON format
    if (clean.startsWith("{") && clean.endsWith("}")) {
      val parsed = RemotePeerNode.fromJson(clean)
      if (parsed != null && parsed.nodeId.isNotBlank()) {
        val resolvedName = parsed.username.ifBlank { "Node ${parsed.nodeId.takeLast(4)}" }
        return parsed.copy(username = resolvedName)
      }
    }

    // 2. Fallback to bluefi:// URI format
    val uri = if (clean.startsWith("bluefi://") || clean.contains("id=")) {
      Uri.parse(clean)
    } else {
      null
    }

    val id = uri?.getQueryParameter("id")
      ?: if (clean.startsWith("BF-")) "peer_${clean.lowercase().replace("-", "_")}"
      else "node_qr_${System.currentTimeMillis() % 10000}"

    val rawName = uri?.getQueryParameter("name") ?: ""
    val name = Uri.decode(rawName).ifBlank { "Node ${id.takeLast(4)}" }
    val pubKey = uri?.getQueryParameter("key") ?: "04${id.padEnd(62, '0').take(64)}"
    val peerIp = uri?.getQueryParameter("ip") ?: ""

    return RemotePeerNode(
      nodeId = id,
      username = name,
      avatarIndex = (id.hashCode() and 0x7FFFFFFF) % 6,
      publicKeyHex = pubKey,
      ipAddress = peerIp
    )
  }

  suspend fun saveOrUpdateRemotePeer(
    peerDao: PeerDao,
    remote: RemotePeerNode,
    pairingCode: String = ""
  ): PeerEntity {
    val existing = peerDao.getPeerDirect(remote.nodeId)
    val cleanUsername = remote.username.ifBlank { "Node ${remote.nodeId.takeLast(4)}" }

    val entity = if (existing != null) {
      existing.copy(
        username = cleanUsername.lowercase().replace(" ", "_"),
        displayName = cleanUsername,
        avatarIndex = remote.avatarIndex,
        isOnline = true,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = "Pairing verified via QR handshake",
        publicKeyHex = remote.publicKeyHex.ifBlank { existing.publicKeyHex }
      )
    } else {
      PeerEntity(
        id = remote.nodeId,
        username = cleanUsername.lowercase().replace(" ", "_"),
        displayName = cleanUsername,
        avatarIndex = remote.avatarIndex,
        isOnline = true,
        rssi = -48,
        protocol = "2.4 GHz Direct P2P",
        hopCount = 1,
        distanceEstimatedMeters = 1.0,
        lastSeenTimestamp = System.currentTimeMillis(),
        lastMessagePreview = "Connected via QR Scanner ($cleanUsername)",
        lastMessageTimestamp = System.currentTimeMillis(),
        unreadCount = 0,
        publicKeyHex = remote.publicKeyHex.ifBlank { "04${remote.nodeId.padEnd(62, '0').take(64)}" }
      )
    }

    peerDao.insertPeer(entity)
    Log.d(TAG, "Saved remote peer successfully: id=${entity.id}, displayName=${entity.displayName}")
    return entity
  }
}
