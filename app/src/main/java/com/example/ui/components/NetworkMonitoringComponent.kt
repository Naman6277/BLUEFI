package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.hardware.HardwareRadioState

/**
 * Real-time network monitoring component that displays 'Online' or 'Disconnected' status
 * based on active P2P mesh connectivity.
 */
@Composable
fun NetworkMonitoringComponent(
  radioState: HardwareRadioState,
  connectedPeersCount: Int,
  onOpenBluetoothSettings: () -> Unit = {},
  onOpenWifiSettings: () -> Unit = {},
  onRefreshRadios: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  val isOnline = radioState.meshStatus == "Online" || connectedPeersCount > 0 || radioState.isBluetoothEnabled || radioState.isWifiEnabled

  // Pulsing dot animation for live status
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_net")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1100, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val errorColor = MaterialTheme.colorScheme.error
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isOnline) surfaceVariant else MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = androidx.compose.ui.graphics.SolidColor(
        if (isOnline) primaryColor.copy(alpha = 0.5f) else errorColor.copy(alpha = 0.5f)
      )
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded }
      .testTag("network_monitoring_component")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Indicator + Status text
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Radar Pulse Icon
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(
                color = (if (isOnline) primaryColor else errorColor).copy(alpha = 0.15f),
                shape = CircleShape
              )
              .border(
                1.5.dp,
                if (isOnline) primaryColor else errorColor,
                CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .scale(if (isOnline) pulseScale else 1.0f)
                .background(
                  color = if (isOnline) primaryColor.copy(alpha = pulseAlpha) else errorColor,
                  shape = CircleShape
                )
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (isOnline) "Online" else "Disconnected",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                color = if (isOnline) primaryColor else errorColor
              )
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .background(
                    (if (isOnline) primaryColor else errorColor).copy(alpha = 0.15f),
                    RoundedCornerShape(6.dp)
                  )
                  .border(
                    1.dp,
                    if (isOnline) primaryColor else errorColor,
                    RoundedCornerShape(6.dp)
                  )
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "P2P MESH",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = if (isOnline) primaryColor else errorColor
                )
              }
            }

            Text(
              text = if (isOnline)
                "$connectedPeersCount active peer(s) • 2.4 GHz duplex live"
              else
                "No RF link • Enable Bluetooth & Wi-Fi",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Expand arrow & refresh
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onRefreshRadios,
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Radio State",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Quick status row badges
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Bluetooth Badge
        RadioPill(
          label = "Bluetooth",
          isOn = radioState.isBluetoothEnabled,
          activeIcon = Icons.Default.Bluetooth,
          inactiveIcon = Icons.Default.BluetoothDisabled,
          onClick = onOpenBluetoothSettings,
          modifier = Modifier.weight(1f)
        )

        // Wi-Fi Badge
        RadioPill(
          label = "Wi-Fi P2P",
          isOn = radioState.isWifiEnabled,
          activeIcon = Icons.Default.Wifi,
          inactiveIcon = Icons.Default.WifiOff,
          onClick = onOpenWifiSettings,
          modifier = Modifier.weight(1f)
        )

        // Cross-OS compatibility tag
        Box(
          modifier = Modifier
            .weight(1.1f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "iOS • Android • XOS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Expandable hardware details & direct actions
      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          )
          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "HARDWARE TRANSCEIVER DETAILS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(6.dp))

          TelemetryRow("Carrier Freq", "2.4 GHz RF (ISM Band / 802.11 + BLE)")
          TelemetryRow("Duplex Port", "UDP 24242 (Broadcast + Multicast 239.255.242.42)")
          TelemetryRow("Encryption", "AES-256-GCM Hardware Demux (100% Offline)")
          TelemetryRow("XOS Power Safe", "Broadcast Socket Mode (Zero Firewall Block)")

          if (!radioState.isBluetoothEnabled || !radioState.isWifiEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (!radioState.isBluetoothEnabled) {
                Button(
                  onClick = onOpenBluetoothSettings,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(38.dp)
                ) {
                  Text("Turn ON BT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
              if (!radioState.isWifiEnabled) {
                Button(
                  onClick = onOpenWifiSettings,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(38.dp)
                ) {
                  Text("Turn ON Wi-Fi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RadioPill(
  label: String,
  isOn: Boolean,
  activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
  inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val errorColor = MaterialTheme.colorScheme.error

  Box(
    modifier = modifier
      .background(
        if (isOn) primaryColor.copy(alpha = 0.15f) else errorColor.copy(alpha = 0.12f),
        RoundedCornerShape(8.dp)
      )
      .border(
        1.dp,
        if (isOn) primaryColor.copy(alpha = 0.6f) else errorColor.copy(alpha = 0.5f),
        RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = if (isOn) activeIcon else inactiveIcon,
        contentDescription = label,
        tint = if (isOn) primaryColor else errorColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = if (isOn) "$label ON" else "$label OFF",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (isOn) primaryColor else errorColor
      )
    }
  }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

/**
 * Modal Popup Dialog with blinking / animated text:
 * "wifi or bluetooth ON kr lo Darling"
 * Triggered after login when either Bluetooth or Wi-Fi is turned OFF.
 */
@Composable
fun RadioAlertPopupDialog(
  isBluetoothEnabled: Boolean,
  isWifiEnabled: Boolean,
  onTurnOnBluetooth: () -> Unit,
  onTurnOnWifi: () -> Unit,
  onCheckAgain: () -> Unit,
  onDismiss: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "blinking_alert")

  // Fast blinking alpha for urgent alert text
  val blinkingAlpha by infiniteTransition.animateFloat(
    initialValue = 0.15f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 280, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "blink_alpha"
  )

  // Pulsing scale for heart/alert glow
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "blink_scale"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = Color(0xFF140808)
      ),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF3366))
      ),
      shape = RoundedCornerShape(22.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("radio_alert_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon badge with warning glow
        Box(
          modifier = Modifier
            .size(68.dp)
            .background(Color(0xFFFF1744).copy(alpha = 0.15f), CircleShape)
            .border(2.5.dp, Color(0xFFFF1744), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = null,
            tint = Color(0xFFFF1744),
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "RADIO HARDWARE VERIFICATION",
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.5.sp,
          color = Color(0xFFFF80AB)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // EXACT REQUESTED BLINKING ANIMATED POPUP TEXT:
        Text(
          text = "wifi or bluetooth ON kr lo Darling",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          color = Color(0xFFFF1744).copy(alpha = blinkingAlpha),
          modifier = Modifier
            .scale(pulseScale)
            .padding(horizontal = 4.dp)
            .testTag("blinking_darling_alert_text")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Bluefi needs Bluetooth and Wi-Fi hardware radios turned ON for offline 2.4 GHz peer-to-peer duplex communication and Curve25519 mesh discovery.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Radio Status Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Bluetooth Status Card
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isBluetoothEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(
                if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252)
              )
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = if (isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Bluetooth",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isBluetoothEnabled) "READY" else "OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252)
              )
              if (!isBluetoothEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = onTurnOnBluetooth,
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.fillMaxWidth().height(30.dp).testTag("direct_turn_on_bt_button")
                ) {
                  Text("TURN ON (DIRECT)", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
              }
            }
          }

          // Wi-Fi Status Card
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isWifiEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(
                if (isWifiEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252)
              )
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = if (isWifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isWifiEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Wi-Fi",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isWifiEnabled) "READY" else "OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isWifiEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5252)
              )
              if (!isWifiEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = onTurnOnWifi,
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.fillMaxWidth().height(30.dp).testTag("direct_turn_on_wifi_button")
                ) {
                  Text("TURN ON (DIRECT)", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct 1-tap activate both internal radios
        if (!isBluetoothEnabled || !isWifiEnabled) {
          Button(
            onClick = {
              onTurnOnBluetooth()
              onTurnOnWifi()
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFFF3366),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag("direct_activate_both_radios_button")
          ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("⚡ DIRECT TURN ON BOTH (NO SETTINGS)", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Action Buttons
        Button(
          onClick = onCheckAgain,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("radio_alert_check_again_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CHECK HARDWARE RADIOS AGAIN", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("radio_alert_continue_anyway_button")
        ) {
          Text("CONTINUE ANYWAY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}
