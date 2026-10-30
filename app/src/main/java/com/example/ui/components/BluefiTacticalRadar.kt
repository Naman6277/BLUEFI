package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Peer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated 2.4 GHz Tactical Radar Scope.
 * Positioned in Home Screen directly above "MY PAIRING QR & PIN CODE".
 * Fully dynamic theme bindings: responds 100% to Hackathon Green, Dark & Fast Blue, and White/Light themes.
 */
@Composable
fun BluefiTacticalRadar(
  peers: List<Peer>,
  onConnectPeer: (Peer) -> Unit,
  onOpenChat: (Peer) -> Unit,
  onScanRefresh: () -> Unit = {},
  onSimulatePhonePeer: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedRadarPeer by remember { mutableStateOf<Peer?>(null) }
  var isExpanded by remember { mutableStateOf(true) }

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
  val scopeBgColor = MaterialTheme.colorScheme.surface
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface

  // Filter to show ONLY phone users and BLUEFI applications; hide all others (relays, gateways, hardware, bots)
  val bluefiPhonePeers = remember(peers) {
    peers.filter { peer ->
      val id = peer.id.lowercase()
      val name = peer.displayName.lowercase()
      val user = peer.username.lowercase()
      val isInfrastructureOrHardware = id.contains("relay") || id.contains("transceiver") ||
        id.contains("sentinel") || id.contains("gateway") || id.contains("group") ||
        id.contains("beacon") || id.contains("meshnode") ||
        name.contains("relay") || name.contains("transceiver") || name.contains("sentinel") ||
        name.contains("gateway") || name.contains("operations") || name.contains("hardware") ||
        name.contains("beacon") || name.contains("meshnode")

      !isInfrastructureOrHardware && (
        id.startsWith("phone_") || id.startsWith("bluefi_") || id.startsWith("node_p2p_") ||
        name.contains("phone") || name.contains("bluefi") || user.contains("phone") || user.contains("bluefi") ||
        peer.protocol.contains("Direct P2P") || peer.protocol.contains("Radar") || peer.protocol.contains("Duplex")
      )
    }
  }

  // Rotating radar sweep animation (0 to 360 degrees)
  val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep_transition")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "sweep_angle"
  )

  // Pulsing center beacon
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = surfaceColor),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = SolidColor(primaryColor.copy(alpha = 0.35f))
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("home_radar_scanner_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Title & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(primaryColor.copy(alpha = 0.15f), CircleShape)
              .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Radio,
              contentDescription = "Radar Scanner",
              tint = primaryColor,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "BLUEFI RF RADAR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = primaryColor
              )
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 5.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "2.4 GHz",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = primaryColor
                )
              }
            }
            Text(
              text = if (bluefiPhonePeers.isEmpty()) "Scanning 2.4 GHz • Always Active"
                     else "${bluefiPhonePeers.size} Bluefi Phone Applications in range",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onScanRefresh,
            modifier = Modifier
              .size(32.dp)
              .background(MaterialTheme.colorScheme.surface, CircleShape)
              .testTag("radar_scan_refresh_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Scan",
              tint = primaryColor,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Box(
            modifier = Modifier
              .clickable { isExpanded = !isExpanded }
              .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Text(
              text = if (isExpanded) "COLLAPSE" else "EXPAND",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = primaryColor
            )
          }
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
          // Radar Canvas Scope
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(250.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(scopeBgColor)
              .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Canvas(
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .pointerInput(bluefiPhonePeers) {
                  detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = min(size.width, size.height) / 2f - 14.dp.toPx()

                    // Check which peer was tapped
                    val tapped = bluefiPhonePeers.firstOrNull { peer ->
                      val pos = calculatePeerPosition(peer, center, maxRadius)
                      val distance = (tapOffset - pos).getDistance()
                      distance <= 28.dp.toPx()
                    }
                    if (tapped != null) {
                      selectedRadarPeer = tapped
                    }
                  }
                }
            ) {
              val center = Offset(size.width / 2f, size.height / 2f)
              val maxRadius = min(size.width, size.height) / 2f - 14.dp.toPx()

              // 1. Concentric Range Rings (5m, 15m, 30m, 50m)
              val rings = listOf(0.25f, 0.5f, 0.75f, 1.0f)
              rings.forEachIndexed { index, fraction ->
                val r = maxRadius * fraction
                drawCircle(
                  color = primaryColor.copy(alpha = if (index == 3) 0.35f else 0.18f),
                  radius = r,
                  center = center,
                  style = Stroke(
                    width = if (index == 3) 1.5.dp.toPx() else 1.dp.toPx(),
                    pathEffect = if (index < 3) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                  )
                )
              }

              // 2. Crosshair Grid Lines (Horizontal & Vertical Axes)
              drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
              )
              drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
              )

              // 3. Diagonal Auxiliary Grid Lines (45 degree angles)
              val diagOffset = (maxRadius * 0.7071f)
              drawLine(
                color = primaryColor.copy(alpha = 0.1f),
                start = Offset(center.x - diagOffset, center.y - diagOffset),
                end = Offset(center.x + diagOffset, center.y + diagOffset),
                strokeWidth = 1.dp.toPx()
              )
              drawLine(
                color = primaryColor.copy(alpha = 0.1f),
                start = Offset(center.x - diagOffset, center.y + diagOffset),
                end = Offset(center.x + diagOffset, center.y - diagOffset),
                strokeWidth = 1.dp.toPx()
              )

              // 4. Rotating Radar Sweeping Beam (Cone gradient)
              val sweepRad = Math.toRadians(sweepAngle.toDouble())
              val beamEnd = Offset(
                center.x + maxRadius * cos(sweepRad).toFloat(),
                center.y + maxRadius * sin(sweepRad).toFloat()
              )

              // Draw radar sweep line
              drawLine(
                brush = Brush.linearGradient(
                  colors = listOf(primaryColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.1f)),
                  start = center,
                  end = beamEnd
                ),
                start = center,
                end = beamEnd,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
              )

              // Sweeping phosphor cone behind the beam
              val trailSegments = 16
              for (i in 0 until trailSegments) {
                val backAngle = sweepRad - (i * 0.05)
                val alpha = (1f - (i.toFloat() / trailSegments)) * 0.15f
                val p = Offset(
                  center.x + maxRadius * cos(backAngle).toFloat(),
                  center.y + maxRadius * sin(backAngle).toFloat()
                )
                drawLine(
                  color = primaryColor.copy(alpha = alpha),
                  start = center,
                  end = p,
                  strokeWidth = 3.dp.toPx()
                )
              }

              // 5. Center Local Device Node Blip
              drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = 12.dp.toPx() * pulseScale,
                center = center
              )
              drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = center
              )

              // 6. Draw Actual Bluefi Users Blips on the Radar Plane
              bluefiPhonePeers.forEach { peer ->
                val peerPos = calculatePeerPosition(peer, center, maxRadius)
                val isConn = peer.isOnline

                // Calculate angular proximity to sweep line for radar illuminate effect
                val peerAngle = Math.toDegrees(
                  kotlin.math.atan2(
                    (peerPos.y - center.y).toDouble(),
                    (peerPos.x - center.x).toDouble()
                  )
                ).let { if (it < 0) it + 360.0 else it }

                val angleDiff = abs(sweepAngle - peerAngle)
                val isSwept = angleDiff < 20.0 || angleDiff > 340.0

                // Outer pulsing halo
                if (isConn || isSwept) {
                  drawCircle(
                    color = if (isConn) primaryColor.copy(alpha = 0.4f) else secondaryColor.copy(alpha = 0.35f),
                    radius = (if (isSwept) 14.dp else 10.dp).toPx(),
                    center = peerPos
                  )
                }

                // Node point blip
                drawCircle(
                  color = if (isConn) primaryColor else secondaryColor,
                  radius = (if (isConn) 6.dp else 5.dp).toPx(),
                  center = peerPos
                )

                // Thin border ring
                drawCircle(
                  color = onSurfaceColor.copy(alpha = 0.75f),
                  radius = (if (isConn) 7.5.dp else 6.5.dp).toPx(),
                  center = peerPos,
                  style = Stroke(width = 1.dp.toPx())
                )
              }
            }

            // Radar Status Overlays
            Row(
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .background(primaryColor, CircleShape)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "SWEEP: ACTIVE • 2.4 GHz",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = primaryColor
              )
            }

            Text(
              text = "RANGE: 50M MAX",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = primaryColor.copy(alpha = 0.6f),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
            )

            Text(
              text = if (bluefiPhonePeers.isEmpty()) "Scanning 2.4 GHz RF • 0 Bluefi phones in range • System always active"
                     else "Tap any node blip or card to connect without PIN",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = if (bluefiPhonePeers.isEmpty()) primaryColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (bluefiPhonePeers.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(primaryColor.copy(alpha = 0.3f))
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("radar_empty_card")
            ) {
              Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier.size(8.dp).background(primaryColor, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "RADAR SCANNING ALWAYS ON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = primaryColor
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Only phone users running the Bluefi application are displayed. Non-phone and hardware devices are hidden. The scanning system is always actively searching for nearby Bluefi phones (50m range).",
                  fontSize = 10.sp,
                  lineHeight = 14.sp,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = onSimulatePhonePeer,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("radar_simulate_phone_btn")
                ) {
                  Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Discover Bluefi Phone (Demo)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            // Horizontal List of Actual Bluefi Users on the Radar
            Text(
              text = "BLUEFI PHONE APPLICATIONS (${bluefiPhonePeers.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = primaryColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              bluefiPhonePeers.forEach { peer ->
                RadarPeerMiniCard(
                  peer = peer,
                  isSelected = selectedRadarPeer?.id == peer.id,
                  onClick = { selectedRadarPeer = peer }
                )
              }
            }
          }
        }
      }
    }
  }

  // Selected Peer Radar Connection Dialog
  if (selectedRadarPeer != null) {
    val peer = selectedRadarPeer!!
    AlertDialog(
      onDismissRequest = { selectedRadarPeer = null },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              if (peer.isOnline) primaryColor.copy(alpha = 0.2f)
              else secondaryColor.copy(alpha = 0.2f),
              CircleShape
            )
            .border(
              1.5.dp,
              if (peer.isOnline) primaryColor else secondaryColor,
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (peer.protocol.contains("BT")) Icons.Default.Bluetooth else Icons.Default.Wifi,
            contentDescription = null,
            tint = if (peer.isOnline) primaryColor else secondaryColor,
            modifier = Modifier.size(26.dp)
          )
        }
      },
      title = {
        Text(
          text = peer.displayName,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "@${peer.username}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = primaryColor
          )
          Spacer(modifier = Modifier.height(8.dp))

          // Proximity Telemetry
          Row(
            modifier = Modifier
              .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
              .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Estimated Distance: ${String.format("%.1f", peer.distanceEstimatedMeters)}m",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "RF Channel: ${peer.protocol} • RSSI: ${peer.rssi} dBm",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (peer.isOnline) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(primaryColor, CircleShape)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Device is currently Connected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
              )
            }
          } else {
            Text(
              text = "Tap 'Connect via Radar' to establish direct encrypted mesh link without entering PIN code.",
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      },
      confirmButton = {
        if (peer.isOnline) {
          Button(
            onClick = {
              val p = peer
              selectedRadarPeer = null
              onOpenChat(p)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = primaryColor,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("radar_open_chat_btn")
          ) {
            Text("Open Chat", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = {
              val p = peer
              selectedRadarPeer = null
              onConnectPeer(p)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = primaryColor,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("radar_connect_now_btn")
          ) {
            Text("Connect via Radar (No PIN)", fontWeight = FontWeight.Black, fontSize = 12.sp)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedRadarPeer = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

/**
 * Calculates deterministic polar coordinates for a peer based on distance and ID hash.
 */
private fun calculatePeerPosition(peer: Peer, center: Offset, maxRadius: Float): Offset {
  // Normalize distance: 0m to 50m mapped to 0.18f .. 0.95f
  val normalizedDist = (peer.distanceEstimatedMeters / 50.0).coerceIn(0.15, 0.92).toFloat()
  val r = maxRadius * normalizedDist

  // Deterministic bearing angle based on hash
  val angleDeg = (abs(peer.id.hashCode()) % 360).toDouble()
  val rad = Math.toRadians(angleDeg)

  val x = center.x + (r * cos(rad)).toFloat()
  val y = center.y + (r * sin(rad)).toFloat()

  return Offset(x, y)
}

/**
 * Mini chip card for each peer found on the radar.
 */
@Composable
private fun RadarPeerMiniCard(
  peer: Peer,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (isSelected) primaryColor.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surface
      )
      .border(
        width = 1.dp,
        color = if (peer.isOnline) primaryColor.copy(alpha = 0.6f)
        else if (isSelected) primaryColor
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 8.dp)
      .testTag("radar_peer_chip_${peer.id}")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(
            if (peer.isOnline) primaryColor.copy(alpha = 0.18f)
            else secondaryColor.copy(alpha = 0.15f),
            CircleShape
          )
          .border(
            1.dp,
            if (peer.isOnline) primaryColor else secondaryColor.copy(alpha = 0.5f),
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = peer.displayName.take(1).uppercase(),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = if (peer.isOnline) primaryColor else secondaryColor
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = peer.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (peer.isOnline) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(primaryColor, CircleShape)
            )
          }
        }
        Text(
          text = "${String.format("%.1f", peer.distanceEstimatedMeters)}m • ${peer.rssi} dBm",
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          color = if (peer.isOnline) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
