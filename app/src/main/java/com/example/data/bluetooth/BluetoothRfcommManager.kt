package com.example.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.mesh.DuplexPacket
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles Bluetooth RFCOMM socket server and client connections using explicit, leak-free worker threads:
 * - AcceptThread: Listens for incoming socket connections via BluetoothServerSocket.
 * - ConnectThread: Initiates outgoing client connections to remote devices with OEM fallbacks.
 * - ConnectedThread: Streams duplex line-delimited JSON packets over established BluetoothSockets.
 *
 * Thread Safety & Leak Prevention:
 * - Every worker thread defines a cancel() method that explicitly closes its socket, sets isRunning = false,
 *   and interrupts the thread so blocking calls (read() / accept()) terminate immediately.
 * - Sockets, streams, and threads are cleaned up without memory or handle leaks when the app is swiped away.
 */
class BluetoothRfcommManager(
  private val context: Context
) {
  companion object {
    private const val TAG = "BluefiRfcomm"
    private const val SOCKET_SERVICE_NAME = "BluefiRfcommSecure"

    // Standard Bluefi RFCOMM Service UUID
    val RFCOMM_UUID: UUID = UUID.fromString("0000BFE1-0000-1000-8000-00805F9B34FB")
    // Fallback standard Serial Port Profile (SPP) UUID supported universally
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  }

  private val bluetoothAdapter: BluetoothAdapter? by lazy {
    try {
      val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
      bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    } catch (e: Exception) {
      null
    }
  }

  @Volatile
  private var acceptThread: AcceptThread? = null

  @Volatile
  private var connectThread: ConnectThread? = null

  // Active connected worker threads indexed by remote device MAC address
  private val connectedThreads = ConcurrentHashMap<String, ConnectedThread>()

  private val _incomingPackets = MutableSharedFlow<DuplexPacket>(extraBufferCapacity = 64)
  val incomingPackets: SharedFlow<DuplexPacket> = _incomingPackets.asSharedFlow()

  @Volatile
  private var localNodeId: String = ""
  @Volatile
  private var localSenderName: String = ""

  /**
   * Starts the background AcceptThread to listen for incoming Bluetooth RFCOMM sockets.
   * Reuses existing thread if already active to prevent duplicate socket listeners.
   */
  @Synchronized
  fun startServer(nodeId: String, senderName: String) {
    localNodeId = nodeId
    localSenderName = senderName

    if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
      Log.d(TAG, "BluetoothAdapter disabled or unavailable, deferring AcceptThread start")
      return
    }

    // Reuse existing thread if already alive and listening
    if (acceptThread != null && acceptThread!!.isAlive && acceptThread!!.isRunning) {
      Log.d(TAG, "AcceptThread is already running and listening; reusing existing thread")
      return
    }

    // Terminate any previous thread cleanly
    acceptThread?.cancel()
    acceptThread = null

    try {
      acceptThread = AcceptThread().apply { start() }
      Log.i(TAG, "AcceptThread started for incoming RFCOMM sockets")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start AcceptThread: ${e.message}")
    }
  }

  /**
   * Connects to a target Bluetooth device.
   * Cancels ongoing connection attempts before starting a new ConnectThread.
   */
  @Synchronized
  fun connectToDevice(deviceAddress: String, onComplete: (Boolean) -> Unit = {}) {
    if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
      onComplete(false)
      return
    }

    // If already connected to this remote device, reuse the existing connection
    val existing = connectedThreads[deviceAddress]
    if (existing != null && existing.isRunning) {
      Log.i(TAG, "Already connected to $deviceAddress; reusing active ConnectedThread")
      onComplete(true)
      return
    }

    // Cancel existing ConnectThread if running
    connectThread?.cancel()
    connectThread = null

    try {
      val device = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
      connectThread = ConnectThread(device, onComplete).apply { start() }
    } catch (e: Exception) {
      Log.e(TAG, "Error initiating ConnectThread to $deviceAddress: ${e.message}")
      onComplete(false)
    }
  }

  /**
   * Registers an established socket connection and starts a ConnectedThread.
   */
  @Synchronized
  private fun connected(socket: BluetoothSocket, deviceAddress: String) {
    // Immediately cancel discovery as soon as a socket connection is established (Power Efficiency)
    try {
      if (bluetoothAdapter?.isDiscovering == true) {
        bluetoothAdapter?.cancelDiscovery()
      }
    } catch (_: Exception) {}

    // Cancel any previous ConnectedThread for this remote peer
    connectedThreads[deviceAddress]?.cancel()
    connectedThreads.remove(deviceAddress)

    try {
      val thread = ConnectedThread(socket, deviceAddress)
      connectedThreads[deviceAddress] = thread
      thread.start()
      Log.i(TAG, "ConnectedThread active for $deviceAddress (Total active sockets: ${connectedThreads.size})")

      // Send initial handshake identification packet
      if (localNodeId.isNotBlank()) {
        val handshakeBeacon = DuplexPacket(
          type = "BEACON",
          packetId = "bt_hs_${System.currentTimeMillis()}",
          senderId = localNodeId,
          senderName = localSenderName,
          receiverId = "BROADCAST",
          osPlatform = "Android/BluetoothSocket",
          timestamp = System.currentTimeMillis()
        )
        thread.write(handshakeBeacon)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start ConnectedThread for $deviceAddress: ${e.message}")
      try { socket.close() } catch (_: Exception) {}
    }
  }

  /**
   * Broadcasts a packet to all actively connected Bluetooth RFCOMM sockets.
   */
  fun broadcastPacket(packet: DuplexPacket) {
    if (connectedThreads.isEmpty()) return
    connectedThreads.forEach { (address, thread) ->
      if (thread.isRunning) {
        thread.write(packet)
      } else {
        connectedThreads.remove(address)
      }
    }
  }

  /**
   * Sends a packet to a specific connected device.
   */
  fun sendPacketToDevice(deviceAddress: String, packet: DuplexPacket): Boolean {
    val thread = connectedThreads[deviceAddress] ?: return false
    return thread.write(packet)
  }

  fun getConnectedDevicesCount(): Int = connectedThreads.size

  /**
   * Shuts down all Bluetooth RFCOMM worker threads and sockets cleanly.
   * Closes all server and client sockets, sets thread running flags to false, and interrupts threads.
   */
  @Synchronized
  fun stop() {
    Log.i(TAG, "Stopping BluetoothRfcommManager: shutting down all worker threads and sockets")

    acceptThread?.cancel()
    acceptThread = null

    connectThread?.cancel()
    connectThread = null

    connectedThreads.forEach { (_, thread) ->
      thread.cancel()
    }
    connectedThreads.clear()
  }

  // =========================================================================
  // WORKER THREAD IMPLEMENTATIONS WITH EXPLICIT CLEANUP & INTERRUPT
  // =========================================================================

  /**
   * AcceptThread: Listens for incoming RFCOMM sockets.
   */
  private inner class AcceptThread : Thread("BluefiAcceptThread") {
    @Volatile
    var isRunning = true
      private set

    private var serverSocket: BluetoothServerSocket? = null

    init {
      try {
        @SuppressLint("MissingPermission")
        serverSocket = try {
          bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(SOCKET_SERVICE_NAME, RFCOMM_UUID)
        } catch (e1: Exception) {
          Log.w(TAG, "listenUsingInsecureRfcomm failed, fallback to secure: ${e1.message}")
          try {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SOCKET_SERVICE_NAME, RFCOMM_UUID)
          } catch (e2: Exception) {
            Log.w(TAG, "listenUsingRfcomm failed, fallback to SPP: ${e2.message}")
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("BluefiSPP", SPP_UUID)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to bind BluetoothServerSocket: ${e.message}")
        serverSocket = null
      }
    }

    override fun run() {
      val ss = serverSocket ?: return
      Log.i(TAG, "AcceptThread listening for incoming peer sockets...")

      while (isRunning) {
        val socket: BluetoothSocket = try {
          ss.accept()
        } catch (e: Exception) {
          if (!isRunning) {
            Log.d(TAG, "AcceptThread server socket closed cleanly")
          } else {
            Log.d(TAG, "AcceptThread accept exception: ${e.message}")
          }
          break
        }

        if (socket != null && isRunning) {
          val remoteAddr = socket.remoteDevice?.address ?: "node_${System.currentTimeMillis()}"
          Log.i(TAG, "Accepted incoming BluetoothSocket from $remoteAddr")
          connected(socket, remoteAddr)
        }
      }
    }

    fun cancel() {
      isRunning = false
      try {
        serverSocket?.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing serverSocket in AcceptThread.cancel: ${e.message}")
      }
      serverSocket = null
      interrupt()
    }
  }

  /**
   * ConnectThread: Connects to a remote peer device with multi-tier OEM fallbacks.
   */
  private inner class ConnectThread(
    private val device: BluetoothDevice,
    private val onComplete: (Boolean) -> Unit
  ) : Thread("BluefiConnectThread-${device.address}") {

    @Volatile
    var isRunning = true
      private set

    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    override fun run() {
      // Always cancel discovery before connecting to prevent OEM timeouts
      try {
        if (bluetoothAdapter?.isDiscovering == true) {
          bluetoothAdapter?.cancelDiscovery()
        }
      } catch (_: Exception) {}

      var connectedSocket: BluetoothSocket? = null

      // Attempt 1: Insecure RFCOMM socket
      try {
        socket = device.createInsecureRfcommSocketToServiceRecord(RFCOMM_UUID)
        socket?.connect()
        connectedSocket = socket
      } catch (e1: Exception) {
        Log.w(TAG, "Attempt 1 failed: ${e1.message}. Trying secure...")
        try { socket?.close() } catch (_: Exception) {}

        if (isRunning) {
          // Attempt 2: Secure RFCOMM socket
          try {
            socket = device.createRfcommSocketToServiceRecord(RFCOMM_UUID)
            socket?.connect()
            connectedSocket = socket
          } catch (e2: Exception) {
            Log.w(TAG, "Attempt 2 failed: ${e2.message}. Trying SPP...")
            try { socket?.close() } catch (_: Exception) {}

            if (isRunning) {
              // Attempt 3: Standard SPP UUID
              try {
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                connectedSocket = socket
              } catch (e3: Exception) {
                Log.w(TAG, "Attempt 3 failed: ${e3.message}. Trying reflection channel 1...")
                try { socket?.close() } catch (_: Exception) {}

                if (isRunning) {
                  // Attempt 4: Reflection channel 1 for stubborn Samsung/Xiaomi firmware
                  try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as? BluetoothSocket
                    socket?.connect()
                    connectedSocket = socket
                  } catch (e4: Exception) {
                    Log.e(TAG, "All RFCOMM connection attempts failed for ${device.address}: ${e4.message}")
                    try { socket?.close() } catch (_: Exception) {}
                    connectedSocket = null
                  }
                }
              }
            }
          }
        }
      }

      if (!isRunning) {
        try { connectedSocket?.close() } catch (_: Exception) {}
        onComplete(false)
        return
      }

      if (connectedSocket != null && connectedSocket.isConnected) {
        Log.i(TAG, "Successfully connected to ${device.address}")
        connected(connectedSocket, device.address)
        onComplete(true)
      } else {
        onComplete(false)
      }
    }

    fun cancel() {
      isRunning = false
      try {
        socket?.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing socket in ConnectThread.cancel: ${e.message}")
      }
      socket = null
      interrupt()
    }
  }

  /**
   * ConnectedThread: Streams duplex lines over an active BluetoothSocket.
   */
  private inner class ConnectedThread(
    val socket: BluetoothSocket,
    val remoteAddress: String
  ) : Thread("BluefiConnectedThread-$remoteAddress") {

    @Volatile
    var isRunning = true
      private set

    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

    override fun run() {
      Log.i(TAG, "ConnectedThread started for $remoteAddress")
      while (isRunning && socket.isConnected) {
        try {
          val line = reader.readLine() ?: break
          if (line.isNotBlank()) {
            val packet = DuplexPacket.fromJsonString(line)
            if (packet != null) {
              Log.d(TAG, "Received packet over BluetoothSocket from $remoteAddress: ${packet.type}")
              _incomingPackets.tryEmit(packet)
            }
          }
        } catch (e: Exception) {
          if (!isRunning) {
            Log.d(TAG, "ConnectedThread socket stream closed cleanly for $remoteAddress")
          } else {
            Log.d(TAG, "ConnectedThread stream terminated for $remoteAddress: ${e.message}")
          }
          break
        }
      }

      cancel()
    }

    fun write(packet: DuplexPacket): Boolean {
      if (!isRunning || !socket.isConnected) return false
      return try {
        synchronized(writer) {
          writer.write(packet.toJsonString() + "\n")
          writer.flush()
        }
        true
      } catch (e: Exception) {
        Log.w(TAG, "Write error to $remoteAddress: ${e.message}")
        cancel()
        false
      }
    }

    fun cancel() {
      if (!isRunning) return
      isRunning = false
      connectedThreads.remove(remoteAddress)
      try {
        socket.close()
      } catch (e: Exception) {
        Log.d(TAG, "Notice closing socket in ConnectedThread: ${e.message}")
      }
      interrupt()
    }
  }
}
