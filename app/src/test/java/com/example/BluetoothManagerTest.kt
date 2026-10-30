package com.example
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.bluetooth.BluetoothDiscoveredPeer
import com.example.data.bluetooth.BluetoothManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BluetoothManagerTest {

  @Test
  fun testBluetoothManagerInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = BluetoothManager(context)

    assertNotNull(manager)
    val state = manager.state.value
    assertNotNull(state)
    assertEquals("0000bfe1-0000-1000-8000-00805f9b34fb", BluetoothManager.SERVICE_UUID.toString())
  }

  @Test
  fun testDistanceCalculation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = BluetoothManager(context)

    // Exact reference power RSSI should equal ~1.0 meter
    val distRef = manager.calculateDistance(-59, -59)
    assertTrue("Distance for reference RSSI should be close to 1.0m", distRef in 0.9..1.1)

    // Stronger RSSI indicates closer distance (< 1m)
    val distNear = manager.calculateDistance(-45, -59)
    assertTrue("Stronger RSSI should indicate closer distance", distNear in 0.0..1.0)

    // Weaker RSSI indicates farther distance (> 1m)
    val distFar = manager.calculateDistance(-85, -59)
    assertTrue("Weaker RSSI should indicate farther distance", distFar > 1.0)
  }

  @Test
  fun testToDomainPeerConversion() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = BluetoothManager(context)

    val discovered = BluetoothDiscoveredPeer(
      id = "AA:BB:CC:DD:EE:FF",
      name = "CyberNode_Alpha",
      rssi = -65,
      distanceMeters = 1.45,
      lastSeenTimestamp = 1700000000000L,
      serviceData = "BF:Alpha:E",
      isBluefiPeer = true,
      hopCount = 1
    )

    with(manager) {
      val domainPeer = discovered.toDomainPeer()
      assertEquals("AA:BB:CC:DD:EE:FF", domainPeer.id)
      assertEquals("CyberNode_Alpha", domainPeer.displayName)
      assertEquals("cybernode_alpha", domainPeer.username)
      assertEquals(-65, domainPeer.rssi)
      assertEquals(1, domainPeer.hopCount)
      assertTrue(domainPeer.isOnline)
      assertTrue(domainPeer.publicKeyHex.startsWith("04"))
    }
  }
}
