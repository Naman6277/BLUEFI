package com.example

import com.example.data.crypto.Aes256EncryptionHelper
import com.example.data.crypto.EncryptedPayload
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Aes256EncryptionHelperTest {

  @Before
  fun setUp() {
    Aes256EncryptionHelper.clearAllKeys()
  }

  @After
  fun tearDown() {
    Aes256EncryptionHelper.clearAllKeys()
  }

  @Test
  fun testHandshakeKeyDerivationSymmetry() {
    val localNode = "node_alpha_101"
    val peerNode = "node_beta_202"
    val handshakeSecret = "BF-8842"
    val peerPublicKey = "043059301306072a8648ce3d020106082a8648ce3d03010703420004"

    // Both parties independently derive the key during initial handshake
    val keyDerivedByAlpha = Aes256EncryptionHelper.deriveSharedKey(
      localId = localNode,
      peerId = peerNode,
      peerPublicKeyHex = peerPublicKey,
      handshakeSecret = handshakeSecret
    )

    val keyDerivedByBeta = Aes256EncryptionHelper.deriveSharedKey(
      localId = peerNode,
      peerId = localNode,
      peerPublicKeyHex = peerPublicKey,
      handshakeSecret = handshakeSecret
    )

    assertEquals(Aes256EncryptionHelper.KEY_SIZE_BYTES, keyDerivedByAlpha.size)
    assertEquals(Aes256EncryptionHelper.KEY_SIZE_BYTES, keyDerivedByBeta.size)
    assertArrayEquals("Derived session keys must be perfectly symmetric", keyDerivedByAlpha, keyDerivedByBeta)
  }

  @Test
  fun testHandshakeKeyDerivationUniqueness() {
    val nodeA = "node_alpha"
    val nodeB = "node_beta"
    val nodeC = "node_gamma"

    val keyAB = Aes256EncryptionHelper.deriveSharedKey(nodeA, nodeB, null, "PIN-1111")
    val keyAC = Aes256EncryptionHelper.deriveSharedKey(nodeA, nodeC, null, "PIN-1111")
    val keyABDifferentPin = Aes256EncryptionHelper.deriveSharedKey(nodeA, nodeB, null, "PIN-2222")

    assertFalse("Keys for different peers must differ", keyAB.contentEquals(keyAC))
    assertFalse("Keys with different handshake secrets must differ", keyAB.contentEquals(keyABDifferentPin))
  }

  @Test
  fun testEncryptAndDecryptString() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("nodeA", "nodeB", null, "SECRET-KEY")
    val plaintext = "Hello from offline 2.4 GHz mesh node! AES-256 E2EE works."

    val encryptedPayload = Aes256EncryptionHelper.encrypt(plaintext, sessionKey)
    assertNotNull(encryptedPayload)
    assertEquals(Aes256EncryptionHelper.IV_SIZE_BYTES, encryptedPayload.iv.size)
    assertTrue("Ciphertext should be non-empty", encryptedPayload.ciphertext.isNotEmpty())
    assertNotEquals("Ciphertext must not match plaintext", plaintext, encryptedPayload.ciphertextHex)

    val decryptedText = Aes256EncryptionHelper.decrypt(encryptedPayload, sessionKey)
    assertEquals(plaintext, decryptedText)
  }

  @Test
  fun testEncryptAndDecryptBytes() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("nodeA", "nodeB", null, "BYTES-TEST")
    val rawBytes = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte(), 0x00, 0x42)

    val encrypted = Aes256EncryptionHelper.encryptBytes(rawBytes, sessionKey)
    val decrypted = Aes256EncryptionHelper.decryptBytes(encrypted, sessionKey)

    assertArrayEquals(rawBytes, decrypted)
  }

  @Test(expected = SecurityException::class)
  fun testTamperDetectionOnCiphertext() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("nodeA", "nodeB", null, "INTEGRITY-KEY")
    val originalText = "Top secret mesh telemetry packet"

    val encrypted = Aes256EncryptionHelper.encrypt(originalText, sessionKey)

    // Tamper with the ciphertext (flip one bit)
    val tamperedCiphertext = encrypted.ciphertext.copyOf()
    tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0x01).toByte()

    val tamperedPayload = EncryptedPayload(encrypted.iv, tamperedCiphertext)

    // Decryption must fail with SecurityException due to GCM authentication tag mismatch
    Aes256EncryptionHelper.decrypt(tamperedPayload, sessionKey)
  }

  @Test(expected = SecurityException::class)
  fun testTamperDetectionOnIV() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("nodeA", "nodeB", null, "IV-TEST-KEY")
    val originalText = "Sensor coordinates: 37.7749,-122.4194"

    val encrypted = Aes256EncryptionHelper.encrypt(originalText, sessionKey)

    // Tamper with the IV
    val tamperedIv = encrypted.iv.copyOf()
    tamperedIv[0] = (tamperedIv[0].toInt() xor 0xFF).toByte()

    val tamperedPayload = EncryptedPayload(tamperedIv, encrypted.ciphertext)

    Aes256EncryptionHelper.decrypt(tamperedPayload, sessionKey)
  }

  @Test
  fun testWireFormatSerializationAndDeserialization() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("sender", "receiver", null, "WIRE-KEY")
    val message = "Rapid P2P burst transfer"

    val encrypted = Aes256EncryptionHelper.encrypt(message, sessionKey)
    val wire = encrypted.toWireFormat()

    assertTrue("Wire format must start with BF256:", wire.startsWith("BF256:"))

    val parsed = EncryptedPayload.fromWireFormat(wire)
    assertNotNull(parsed)
    assertArrayEquals(encrypted.iv, parsed!!.iv)
    assertArrayEquals(encrypted.ciphertext, parsed.ciphertext)

    val decrypted = Aes256EncryptionHelper.decrypt(parsed, sessionKey)
    assertEquals(message, decrypted)
  }

  @Test
  fun testCombinedHexFormat() {
    val sessionKey = Aes256EncryptionHelper.deriveSharedKey("node1", "node2", null, "HEX-KEY")
    val message = "Testing hex representation"

    val encrypted = Aes256EncryptionHelper.encrypt(message, sessionKey)
    val combinedHex = encrypted.combinedHex

    val parsedFromHex = EncryptedPayload.fromCombinedHex(combinedHex)
    assertNotNull(parsedFromHex)
    assertArrayEquals(encrypted.iv, parsedFromHex!!.iv)
    assertArrayEquals(encrypted.ciphertext, parsedFromHex.ciphertext)

    val decrypted = Aes256EncryptionHelper.decrypt(parsedFromHex, sessionKey)
    assertEquals(message, decrypted)
  }

  @Test
  fun testSessionKeyRegistrationAndFingerprint() {
    val peerId = "node_delta_99"
    val localId = "my_local_node"

    assertFalse(Aes256EncryptionHelper.hasSessionKey(peerId))

    val key = Aes256EncryptionHelper.deriveAndRegisterSharedKey(
      localId = localId,
      peerId = peerId,
      handshakeSecret = "BF-9900"
    )

    assertTrue(Aes256EncryptionHelper.hasSessionKey(peerId))
    assertArrayEquals(key, Aes256EncryptionHelper.getSessionKey(peerId))

    val fingerprint = Aes256EncryptionHelper.getSessionKeyFingerprint(peerId)
    assertNotNull(fingerprint)
    assertTrue("Fingerprint should be formatted with colons: $fingerprint", fingerprint!!.contains(":"))

    Aes256EncryptionHelper.clearSessionKey(peerId)
    assertFalse(Aes256EncryptionHelper.hasSessionKey(peerId))
  }

  @Test
  fun testEndToEndPeerEncryptionAndDecryption() {
    val aliceId = "alice_node"
    val bobId = "bob_node"
    val handshakePin = "BF-7788"

    // Alice performs handshake
    Aes256EncryptionHelper.deriveAndRegisterSharedKey(aliceId, bobId, null, handshakePin)
    // Bob performs handshake
    Aes256EncryptionHelper.deriveAndRegisterSharedKey(bobId, aliceId, null, handshakePin)

    val secretMessage = "Mission coordinates: Alpha Sector confirmed."

    // Alice encrypts for Bob
    val payload = Aes256EncryptionHelper.encryptForPeer(secretMessage, bobId, aliceId)

    // Bob decrypts from Alice
    val decrypted = Aes256EncryptionHelper.decryptFromPeer(payload, aliceId, bobId)
    assertEquals(secretMessage, decrypted)

    // Decrypt hex wire string
    val decryptedFromHex = Aes256EncryptionHelper.decryptHexFromPeer(payload.combinedHex, aliceId, bobId)
    assertEquals(secretMessage, decryptedFromHex)
  }
}
