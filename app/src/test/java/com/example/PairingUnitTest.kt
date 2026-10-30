package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.BluefiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PairingUnitTest {

  private lateinit var repository: BluefiRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    repository = BluefiRepository(context)
  }

  @Test
  fun testLocalPairingCodeFormat() {
    val code = repository.getLocalPairingCode()
    assertNotNull(code)
    assertTrue("Code should start with BF- prefix: $code", code.startsWith("BF-"))
    assertTrue("Code should have format BF-XXXX: $code", code.length >= 6)
  }

  @Test
  fun testLocalQrPayloadFormat() {
    val payload = repository.getLocalQrPayload()
    assertNotNull(payload)
    assertTrue("QR payload must start with bluefi://connect: $payload", payload.startsWith("bluefi://connect"))
    assertTrue("QR payload must contain id query: $payload", payload.contains("id="))
    assertTrue("QR payload must contain code query: $payload", payload.contains("code=BF-"))
    assertTrue("QR payload must contain key query: $payload", payload.contains("key="))
  }

  @Test
  fun testPairDeviceByCode() = runBlocking {
    val testCode = "BF-7744"
    val result = repository.pairDeviceByCode(testCode)
    assertTrue("Pairing by code should succeed", result.isSuccess)

    val peer = result.getOrNull()
    assertNotNull(peer)
    assertTrue("Peer should be marked online", peer!!.isOnline)
    assertEquals(1, peer.hopCount)

    // Verify messages list contains handshake message
    val messages = repository.getMessagesForPeer(peer.id).first()
    assertTrue("Handshake message should be generated", messages.isNotEmpty())
    assertTrue("Message should mention E2EE Handshake", messages.first().text.contains("E2EE Handshake"))
  }

  @Test
  fun testPairDeviceByQr() = runBlocking {
    val qrPayload = "bluefi://connect?id=node_sat_09&name=Satellite%20Mesh&code=BF-5512&key=0401020304050607"
    val result = repository.pairDeviceByQr(qrPayload)
    assertTrue("Pairing by valid QR should succeed", result.isSuccess)

    val peer = result.getOrNull()
    assertNotNull(peer)
    assertEquals("node_sat_09", peer!!.id)
    assertEquals("Satellite Mesh", peer.displayName)
    assertEquals("0401020304050607", peer.publicKeyHex)
  }

  @Test
  fun testPairDeviceInvalidCode() = runBlocking {
    val result = repository.pairDeviceByCode("A")
    assertTrue("Too short pairing code should fail", result.isFailure)
  }

  @Test
  fun testPairDeviceInvalidQr() = runBlocking {
    val result = repository.pairDeviceByQr("https://example.com/not_bluefi")
    assertTrue("Non-bluefi QR URI should fail", result.isFailure)
  }
}
