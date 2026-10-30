package com.example.data.mesh

import android.content.Context
import com.example.data.bluetooth.BluetoothDiscoveryState
import com.example.data.bluetooth.BluetoothManager
import com.example.data.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeshNetworkStats(
  val totalNodesInMesh: Int = 0,
  val localNodesInRange: Int = 0,
  val carrierWave: String = "2.4 GHz Frequency Wave",
  val effectiveRangeKm: Double = 2.5,
  val baseFrequencyGhz: Double = 2.4,
  val packetsRelayedTotal: Long = 0L,
  val isFirewallActive: Boolean = true,
  val isScanning: Boolean = false,
  val isAdvertising: Boolean = true
)

class BluetoothMeshManager(private val context: Context) {

  val bluetoothManager = BluetoothManager(context)

  private val _networkStats = MutableStateFlow(MeshNetworkStats())
  val networkStats: StateFlow<MeshNetworkStats> = _networkStats.asStateFlow()

  private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
  val discoveredPeers: StateFlow<List<Peer>> = _discoveredPeers.asStateFlow()

  val bluetoothDiscoveryState: StateFlow<BluetoothDiscoveryState> = bluetoothManager.state

  private val scope = CoroutineScope(Dispatchers.IO)

  init {
    // Observe discovered peers from BluetoothManager
    scope.launch {
      bluetoothManager.discoveredPeers.collect { blePeers ->
        val domainPeers = blePeers.map { blePeer ->
          with(bluetoothManager) { blePeer.toDomainPeer() }
        }
        _discoveredPeers.value = domainPeers
        _networkStats.value = _networkStats.value.copy(
          localNodesInRange = if (domainPeers.isNotEmpty()) domainPeers.size else _networkStats.value.localNodesInRange
        )
      }
    }

    // Synchronize scanning and advertising status
    scope.launch {
      bluetoothManager.state.collect { st ->
        _networkStats.value = _networkStats.value.copy(
          isScanning = st.isScanning,
          isAdvertising = st.isAdvertising
        )
      }
    }
  }

  fun startScanning(filterByBluefiOnly: Boolean = false) {
    bluetoothManager.startScanning(filterByBluefiOnly)
  }

  fun stopScanning() {
    bluetoothManager.stopScanning()
  }

  fun setBatterySaverMode(enabled: Boolean) {
    bluetoothManager.isBatterySaverMode = enabled
  }

  fun cancelAllDiscovery() {
    bluetoothManager.cancelAllDiscovery()
  }

  fun startAdvertising(nodeId: String, displayName: String = "", isEncrypted: Boolean = true) {
    bluetoothManager.startAdvertising(nodeId, displayName, isEncrypted)
  }

  fun stopAdvertising() {
    bluetoothManager.stopAdvertising()
  }

  fun incrementRelayedPacketCount() {
    _networkStats.value = _networkStats.value.copy(
      packetsRelayedTotal = _networkStats.value.packetsRelayedTotal + 1
    )
  }

  fun calculateDistance(rssi: Int, txPower: Int = -59): Double {
    return bluetoothManager.calculateDistance(rssi, txPower)
  }
}
