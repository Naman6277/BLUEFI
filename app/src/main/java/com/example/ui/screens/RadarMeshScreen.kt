package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mesh.MeshNetworkStats
import com.example.data.model.Peer
import com.example.ui.components.FrequencyWaveCard

@Composable
fun RadarMeshScreen(
  networkStats: MeshNetworkStats,
  peers: List<Peer>,
  isRelayNodeActive: Boolean,
  onToggleRelayNode: (Boolean) -> Unit,
  onTriggerScan: () -> Unit,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "RadarSweep")
  val radarSweepAngle by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RadarSweepAngle"
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("radar_mesh_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "2.4 GHz MESH RADAR",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "P2P Peer Discovery & Hop Routing Map",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            .padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // Animated Radar Screen
    item {
      val primaryColor = MaterialTheme.colorScheme.primary
      val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
      Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          primaryColor.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(220.dp)
              .background(MaterialTheme.colorScheme.background, CircleShape)
              .border(1.5.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.size(220.dp)) {
              val w = size.width
              val h = size.height
              val center = Offset(w / 2f, h / 2f)

              // Concentric radar distance rings (0.5km, 1.5km, 2.5km)
              drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = w * 0.2f,
                style = Stroke(width = 1f)
              )
              drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = w * 0.35f,
                style = Stroke(width = 1f)
              )
              drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = w * 0.48f,
                style = Stroke(width = 1.2f)
              )

              // Cross axes
              drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 1f
              )
              drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 1f
              )

              // Sweep radar line with glowing gradient
              val rad = Math.toRadians(radarSweepAngle.toDouble())
              val sweepX = (center.x + (w * 0.48f) * Math.cos(rad)).toFloat()
              val sweepY = (center.y + (h * 0.48f) * Math.sin(rad)).toFloat()
              val sweepEnd = Offset(sweepX, sweepY)
              drawLine(
                brush = Brush.linearGradient(
                  colors = listOf(primaryColor, primaryColor.copy(alpha = 0.15f)),
                  start = center,
                  end = sweepEnd
                ),
                start = center,
                end = sweepEnd,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
              )

              // Trailing sweep cone
              val trailSegments = 12
              for (i in 0 until trailSegments) {
                val backAngle = rad - (i * 0.04)
                val alpha = (1f - (i.toFloat() / trailSegments)) * 0.18f
                val p = Offset(
                  (center.x + (w * 0.48f) * Math.cos(backAngle)).toFloat(),
                  (center.y + (h * 0.48f) * Math.sin(backAngle)).toFloat()
                )
                drawLine(
                  color = primaryColor.copy(alpha = alpha),
                  start = center,
                  end = p,
                  strokeWidth = 2f
                )
              }

              // Plot active peer nodes
              val nodePositions = listOf(
                Offset(center.x + 35f, center.y - 45f),
                Offset(center.x - 55f, center.y - 30f),
                Offset(center.x + 70f, center.y + 50f),
                Offset(center.x - 40f, center.y + 65f)
              )

              nodePositions.forEachIndexed { i, pos ->
                drawCircle(
                  color = primaryColor,
                  radius = 5.5f,
                  center = pos
                )
                drawCircle(
                  color = primaryColor.copy(alpha = 0.35f),
                  radius = 11f,
                  center = pos,
                  style = Stroke(width = 1f)
                )
              }

              // Local device node in center (Electric Blue blip with halo)
              drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 10f, center = center)
              drawCircle(color = primaryColor, radius = 5.5f, center = center)
            }

            Text(
              text = "2.5 KM",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              color = primaryColor,
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            RadarStatItem("Active Nodes", "${peers.size} in Range")
            RadarStatItem("Relay Mesh", "${networkStats.effectiveRangeKm} km Max")
            RadarStatItem("Base Band", "${networkStats.baseFrequencyGhz} GHz")
          }
        }
      }
    }

    // 2.4 GHz RF Frequency Wave Card
    item {
      FrequencyWaveCard(
        carrierFreq = networkStats.carrierWave,
        rangeKm = networkStats.effectiveRangeKm,
        baseRequirement = "${networkStats.baseFrequencyGhz} GHz",
        packetsRelayed = networkStats.packetsRelayedTotal
      )
    }

    // Relay Node Toggle
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Mesh Relay Node Mode",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Forwards encrypted packets for distant nodes (up to 2.5 km)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isRelayNodeActive,
            onCheckedChange = onToggleRelayNode,
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("relay_node_switch")
          )
        }
      }
    }

    // Scan Button
    item {
      Button(
        onClick = onTriggerScan,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("radar_rescan_button")
      ) {
        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Trigger 2.4 GHz Beacon Sweep", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun RadarStatItem(title: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.primary,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = title,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
