package com.example.data.mesh

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Data model for over-the-air P2P duplex transmission frame.
 */
data class DuplexPacket(
  val type: String, // "CHAT", "PAIR_REQUEST", "PAIR_ACCEPT", "BEACON", "ACK"
  val packetId: String,
  val senderId: String,
  val senderName: String,
  val receiverId: String, // Peer ID, "BROADCAST", or pairing code
  val pairingCode: String = "",
  val publicKeyHex: String = "",
  val cipherTextHex: String = "",
  val ivHex: String = "",
  val binaryForm: String = "",
  val text: String = "",
  val mediaType: String = "text",
  val mediaContent: String = "",
  val osPlatform: String = "Android/XOS/iOS",
  val senderIp: String = "",
  val isCrossPlatformReady: Boolean = true,
  val timestamp: Long = System.currentTimeMillis()
) {
  fun toJsonString(): String {
    val json = JSONObject()
    json.put("type", type)
    json.put("packetId", packetId)
    json.put("senderId", senderId)
    json.put("senderName", senderName)
    json.put("receiverId", receiverId)
    json.put("pairingCode", pairingCode)
    json.put("publicKeyHex", publicKeyHex)
    json.put("cipherTextHex", cipherTextHex)
    json.put("ivHex", ivHex)
    json.put("binaryForm", binaryForm)
    json.put("text", text)
    json.put("mediaType", mediaType)
    json.put("mediaContent", mediaContent)
    json.put("osPlatform", osPlatform)
    json.put("senderIp", senderIp)
    json.put("isCrossPlatformReady", isCrossPlatformReady)
    json.put("timestamp", timestamp)
    return json.toString()
  }

  companion object {
    fun fromJsonString(str: String): DuplexPacket? {
      return try {
        val json = JSONObject(str)
        DuplexPacket(
          type = json.optString("type", "CHAT"),
          packetId = json.optString("packetId", ""),
          senderId = json.optString("senderId", ""),
          senderName = json.optString("senderName", ""),
          receiverId = json.optString("receiverId", "BROADCAST"),
          pairingCode = json.optString("pairingCode", ""),
          publicKeyHex = json.optString("publicKeyHex", ""),
          cipherTextHex = json.optString("cipherTextHex", ""),
          ivHex = json.optString("ivHex", ""),
          binaryForm = json.optString("binaryForm", ""),
          text = json.optString("text", ""),
          mediaType = json.optString("mediaType", "text"),
          mediaContent = json.optString("mediaContent", ""),
          osPlatform = json.optString("osPlatform", "Android/XOS/iOS"),
          senderIp = json.optString("senderIp", ""),
          isCrossPlatformReady = json.optBoolean("isCrossPlatformReady", true),
          timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}

/**
 * Full-duplex P2P communication transceiver over 2.4 GHz carrier wave / Wi-Fi Direct / Local Ad-Hoc.
 * Listens on port 24242 with Multicast + Broadcast + Unicast UDP support, enabling two-way instantaneous
 * packet transmission and reception simultaneously on both connected devices.
 */
class DuplexP2PTransceiver(
  private val context: Context,
  private val scope: CoroutineScope
) {
  companion object {
    private const val TAG = "DuplexP2PTransceiver"
    const val P2P_PORT = 24242
    const val MULTICAST_GROUP = "239.255.242.42"
    const val MEDIA_TCP_PORT = 24243
  }

  val bluetoothRfcommManager = com.example.data.bluetooth.BluetoothRfcommManager(context)

  private var receiverSocket: DatagramSocket? = null
  private var multicastLock: WifiManager.MulticastLock? = null
  private var listenerJob: Job? = null
  private var mediaServerJob: Job? = null
  private var mediaServerSocket: ServerSocket? = null
  private var rfcommCollectorJob: Job? = null

  private val _incomingPackets = MutableSharedFlow<DuplexPacket>(extraBufferCapacity = 128)
  val incomingPackets: SharedFlow<DuplexPacket> = _incomingPackets.asSharedFlow()

  private var localNodeId: String = ""
  private var localSenderName: String = "Bluefi Node"
  private var beaconJob: Job? = null

  // Known active peer IP addresses for reliable full-duplex unicast fallback
  private val knownPeerIps = ConcurrentHashMap<String, InetAddress>()
  private val allKnownIps = Collections.newSetFromMap(ConcurrentHashMap<InetAddress, Boolean>())

  fun start(nodeId: String, senderName: String = "Bluefi Node") {
    localNodeId = nodeId
    localSenderName = senderName
    acquireMulticastLock()
    startListening()
    startBeaconLoop()
    startMediaServer()

    // Start Bluetooth RFCOMM socket server (BluetoothServerSocket)
    bluetoothRfcommManager.startServer(nodeId, senderName)
    rfcommCollectorJob?.cancel()
    rfcommCollectorJob = scope.launch(Dispatchers.IO) {
      bluetoothRfcommManager.incomingPackets.collect { packet ->
        _incomingPackets.tryEmit(packet)
      }
    }

    // Launch connectedDevice Foreground Service for Android 14+ (API 34-37)
    com.example.data.service.BluefiMeshService.start(context, "2.4 GHz Carrier Active • Connected P2P Socket Channels")
  }

  fun connectBluetoothSocket(deviceAddress: String, onComplete: (Boolean) -> Unit = {}) {
    bluetoothRfcommManager.connectToDevice(deviceAddress, onComplete)
  }

  private fun startMediaServer() {
    if (mediaServerJob?.isActive == true && mediaServerSocket != null && !mediaServerSocket!!.isClosed) return
    mediaServerJob?.cancel()
    mediaServerJob = scope.launch(Dispatchers.IO) {
      try {
        mediaServerSocket?.close()
        mediaServerSocket = ServerSocket(MEDIA_TCP_PORT).apply {
          reuseAddress = true
        }
        Log.i(TAG, "P2P TCP Media server listening on port $MEDIA_TCP_PORT")
        while (isActive) {
          val client = try {
            mediaServerSocket?.accept()
          } catch (_: Exception) {
            null
          } ?: break

          scope.launch(Dispatchers.IO) {
            handleMediaClient(client)
          }
        }
      } catch (e: Exception) {
        Log.d(TAG, "Media server status: ${e.message}")
      }
    }
  }

  private fun handleMediaClient(client: Socket) {
    try {
      val reader = client.getInputStream().bufferedReader()
      val line = reader.readLine() ?: return
      // Request line: GET /video/filename.mp4 HTTP/1.1 or raw filename
      val rawPath = line.substringAfter("GET ").substringBefore(" HTTP").trim().removePrefix("/")
      val fileName = rawPath.substringAfterLast("/")

      val videoDir = File(context.filesDir, "secure_videos")
      val audioDir = File(context.filesDir, "secure_audio")
      var targetFile = File(videoDir, fileName)
      if (!targetFile.exists()) {
        targetFile = File(audioDir, fileName)
      }

      val out = client.getOutputStream()
      if (targetFile.exists() && targetFile.length() > 0) {
        val length = targetFile.length()
        val header = "HTTP/1.1 200 OK\r\nContent-Length: $length\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        targetFile.inputStream().use { input ->
          input.copyTo(out)
        }
        out.flush()
      } else {
        out.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.flush()
      }
      client.close()
    } catch (_: Exception) {
      try { client.close() } catch (_: Exception) {}
    }
  }

  fun downloadMediaFile(senderIp: String, fileName: String, targetFile: File): Boolean {
    if (senderIp.isBlank() || fileName.isBlank()) return false
    return try {
      val socket = Socket()
      socket.connect(InetSocketAddress(senderIp, MEDIA_TCP_PORT), 3000)
      val req = "GET /video/$fileName HTTP/1.1\r\nHost: $senderIp\r\nConnection: close\r\n\r\n"
      socket.getOutputStream().write(req.toByteArray(Charsets.UTF_8))
      socket.getOutputStream().flush()

      val input = socket.getInputStream()
      // Skip HTTP header
      var byte1 = -1
      var byte2 = -1
      var byte3 = -1
      var byte4 = -1
      while (true) {
        byte1 = byte2
        byte2 = byte3
        byte3 = byte4
        byte4 = input.read()
        if (byte4 == -1) break
        if (byte1 == '\r'.code && byte2 == '\n'.code && byte3 == '\r'.code && byte4 == '\n'.code) {
          break
        }
      }

      FileOutputStream(targetFile).use { fos ->
        input.copyTo(fos)
        fos.flush()
      }
      socket.close()
      targetFile.exists() && targetFile.length() > 0
    } catch (e: Exception) {
      Log.d(TAG, "Direct TCP download notice: ${e.message}")
      false
    }
  }

  fun updateSenderInfo(nodeId: String, senderName: String) {
    localNodeId = nodeId
    localSenderName = senderName
  }

  var isBatterySaverMode: Boolean = false

  fun startBeaconLoop() {
    if (beaconJob?.isActive == true) return
    beaconJob = scope.launch(Dispatchers.IO) {
      while (isActive) {
        if (localNodeId.isNotBlank()) {
          val localIp = getLocalIpAddress()
          val beacon = DuplexPacket(
            type = "BEACON",
            packetId = "bcast_${System.currentTimeMillis()}_${(100..999).random()}",
            senderId = localNodeId,
            senderName = localSenderName,
            receiverId = "BROADCAST",
            senderIp = localIp,
            osPlatform = "Android/XOS/iOS",
            timestamp = System.currentTimeMillis()
          )
          broadcastPacket(beacon)
        }
        val interval = if (isBatterySaverMode) 12000L else 4000L
        kotlinx.coroutines.delay(interval)
      }
    }
  }

  fun stop() {
    beaconJob?.cancel()
    beaconJob = null
    listenerJob?.cancel()
    listenerJob = null
    mediaServerJob?.cancel()
    mediaServerJob = null
    rfcommCollectorJob?.cancel()
    rfcommCollectorJob = null
    try {
      receiverSocket?.close()
    } catch (_: Exception) {}
    receiverSocket = null
    try {
      mediaServerSocket?.close()
    } catch (_: Exception) {}
    mediaServerSocket = null
    releaseMulticastLock()
    bluetoothRfcommManager.stop()
  }

  private fun acquireMulticastLock() {
    try {
      val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      multicastLock = wifi?.createMulticastLock("BluefiDuplexLock")?.apply {
        setReferenceCounted(true)
        acquire()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
    }
  }

  private fun releaseMulticastLock() {
    try {
      if (multicastLock?.isHeld == true) {
        multicastLock?.release()
      }
    } catch (_: Exception) {}
  }

  private fun startListening() {
    if (listenerJob?.isActive == true) return

    listenerJob = scope.launch(Dispatchers.IO) {
      try {
        // Use MulticastSocket bound to P2P_PORT to receive both Broadcast and Multicast UDP frames
        val mcastSocket = try {
          MulticastSocket(P2P_PORT).apply {
            reuseAddress = true
            broadcast = true
            try {
              joinGroup(InetAddress.getByName(MULTICAST_GROUP))
            } catch (je: Exception) {
              Log.w(TAG, "joinGroup notice: ${je.message}")
            }
          }
        } catch (_: Exception) {
          DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            bind(InetSocketAddress(P2P_PORT))
          }
        }

        receiverSocket = mcastSocket
        Log.i(TAG, "Full-Duplex P2P socket bound and listening on port $P2P_PORT")

        val buffer = ByteArray(65507)
        val packet = DatagramPacket(buffer, buffer.size)

        while (isActive && !mcastSocket.isClosed) {
          try {
            mcastSocket.receive(packet)
            val senderAddr = packet.address
            if (senderAddr != null && !senderAddr.isLoopbackAddress) {
              allKnownIps.add(senderAddr)
            }

            val dataStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val duplexPacket = DuplexPacket.fromJsonString(dataStr)

            if (duplexPacket != null) {
              // Store sender IP for future direct unicast duplex communication
              if (senderAddr != null && duplexPacket.senderId.isNotBlank()) {
                knownPeerIps[duplexPacket.senderId] = senderAddr
              }
              if (duplexPacket.senderIp.isNotBlank()) {
                try {
                  val parsedIp = InetAddress.getByName(duplexPacket.senderIp)
                  if (!parsedIp.isLoopbackAddress) {
                    allKnownIps.add(parsedIp)
                    knownPeerIps[duplexPacket.senderId] = parsedIp
                  }
                } catch (_: Exception) {}
              }

              // Ignore packets echoed from self
              if (duplexPacket.senderId != localNodeId) {
                Log.d(TAG, "Received DuplexPacket: type=${duplexPacket.type}, sender=${duplexPacket.senderId}, text=${duplexPacket.text.take(15)}")
                _incomingPackets.tryEmit(duplexPacket)
              }
            }
          } catch (e: Exception) {
            if (!isActive || mcastSocket.isClosed) break
            Log.w(TAG, "Packet receive warning: ${e.message}")
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Fatal socket listener error on port $P2P_PORT: ${e.message}")
      }
    }
  }

  /**
   * Registers a direct IP for a peer discovered via QR code or pairing packet.
   */
  fun registerKnownPeerIp(peerId: String, ipAddress: String) {
    if (ipAddress.isBlank()) return
    try {
      val addr = InetAddress.getByName(ipAddress)
      if (!addr.isLoopbackAddress) {
        allKnownIps.add(addr)
        knownPeerIps[peerId] = addr
        Log.i(TAG, "Registered direct peer IP for $peerId: $ipAddress")
      }
    } catch (e: Exception) {
      Log.w(TAG, "registerKnownPeerIp notice: ${e.message}")
    }
  }

  fun getLocalIpAddress(): String {
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val netInterface = interfaces.nextElement()
        if (netInterface.isLoopback || !netInterface.isUp) continue
        for (addr in netInterface.inetAddresses) {
          if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
            return addr.hostAddress ?: ""
          }
        }
      }
    } catch (_: Exception) {}
    return ""
  }

  /**
   * Broadcasts and unicasts a packet across all local network interfaces (2.4 GHz Wi-Fi / Hotspot / Direct / Multicast).
   * Supports immediate full-duplex delivery by delivering to all known peer IP addresses directly.
   */
  fun broadcastPacket(packet: DuplexPacket) {
    scope.launch(Dispatchers.IO) {
      try {
        val packetWithIp = if (packet.senderIp.isBlank()) {
          packet.copy(senderIp = getLocalIpAddress())
        } else {
          packet
        }

        // Prevent UDP "Message too long" (max ~64KB). If video payload is oversized, keep metadata/preview in UDP and serve bytes over TCP
        val safePacket = if (packetWithIp.mediaContent.length > 40000 && packetWithIp.mediaType == "video") {
          val parts = packetWithIp.mediaContent.split("|")
          if (parts.size >= 6) {
            val compactContent = "video_meta|${parts[1]}|${parts[2]}|${parts[3]}|${parts[4]}|"
            packetWithIp.copy(mediaContent = compactContent)
          } else {
            packetWithIp
          }
        } else {
          packetWithIp
        }

        val jsonBytes = safePacket.toJsonString().toByteArray(Charsets.UTF_8)
        val socket = DatagramSocket().apply {
          broadcast = true
        }

        // 1. Send to global broadcast address
        try {
          val globalBcast = DatagramPacket(jsonBytes, jsonBytes.size, InetAddress.getByName("255.255.255.255"), P2P_PORT)
          socket.send(globalBcast)
        } catch (_: Exception) {}

        // 2. Send to all network interface broadcast addresses (e.g., Wi-Fi Direct 192.168.49.255, Hotspot 192.168.43.255)
        try {
          val interfaces = NetworkInterface.getNetworkInterfaces()
          while (interfaces.hasMoreElements()) {
            val netInterface = interfaces.nextElement()
            if (netInterface.isLoopback || !netInterface.isUp) continue

            for (addr in netInterface.interfaceAddresses) {
              val bcast = addr.broadcast
              if (bcast != null) {
                try {
                  val ifacePacket = DatagramPacket(jsonBytes, jsonBytes.size, bcast, P2P_PORT)
                  socket.send(ifacePacket)
                } catch (_: Exception) {}
              }
            }
          }
        } catch (_: Exception) {}

        // 3. Send to Multicast Group 239.255.242.42
        try {
          val multicastAddr = InetAddress.getByName(MULTICAST_GROUP)
          val mcastPacket = DatagramPacket(jsonBytes, jsonBytes.size, multicastAddr, P2P_PORT)
          socket.send(mcastPacket)
        } catch (_: Exception) {}

        // 4. Send direct UNICAST to all discovered peer IP addresses for reliable two-way full duplex
        val targets = HashSet<InetAddress>()
        // Specific target peer IP if known
        val targetIp = knownPeerIps[packet.receiverId]
        if (targetIp != null) {
          targets.add(targetIp)
        }
        targets.addAll(allKnownIps)

        for (destIp in targets) {
          try {
            val directPacket = DatagramPacket(jsonBytes, jsonBytes.size, destIp, P2P_PORT)
            socket.send(directPacket)
          } catch (_: Exception) {}
        }

        socket.close()
        Log.d(TAG, "Sent duplex packet: type=${packet.type}, id=${packet.packetId}, targetPeers=${targets.size}")

        // 5. Broadcast frame to all active Bluetooth RFCOMM sockets (Samsung, Xiaomi, Oppo, etc.)
        bluetoothRfcommManager.broadcastPacket(safePacket)
      } catch (e: Exception) {
        Log.e(TAG, "Error sending packet: ${e.message}")
      }
    }
  }
}
