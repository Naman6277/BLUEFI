package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.hardware.HardwareRadioState
import com.example.data.mesh.MeshNetworkStats
import com.example.data.model.Peer
import com.example.data.model.UserSettings
import com.example.ui.components.BluefiDragonflyLogo
import com.example.ui.components.BluefiTacticalRadar
import com.example.ui.components.FrequencyWaveCard
import com.example.ui.components.NetworkMonitoringComponent
import com.example.ui.components.QrCodeView
import com.example.ui.components.QuickQrScannerDialog
import com.example.ui.components.DeveloperArchitectureDialog

/**
 * First Page (Home Page):
 * - Displays the Dragonfly Logo and "BLUEFI LINK" title
 * - QR Scanner icon button in top section near the title for instant pairing & auto-shift to chat
 * - Network monitoring component showing 'Online' or 'Disconnected' status based on active P2P mesh connectivity
 * - 2.4 GHz Tactical Radar animation above "MY PAIRING QR & PIN CODE"
 * - Shows device's own QR Code and 6-digit PIN code so another device can pair instantly
 * - Displays connected nodes & active chats so user can communicate directly with the connected device
 */
@Composable
fun HomeScreen(
  peers: List<Peer>,
  networkStats: MeshNetworkStats,
  radioState: HardwareRadioState,
  localPairingCode: String,
  localQrPayload: String,
  userSettings: UserSettings,
  onPeerSelected: (String) -> Unit,
  onOpenSplitChat: () -> Unit,
  onOpenPairing: () -> Unit,
  onScanClicked: () -> Unit,
  onPairByCode: (String, (Result<Peer>) -> Unit) -> Unit,
  onPairByQr: (String, (Result<Peer>) -> Unit) -> Unit,
  onOpenBluetoothSettings: () -> Unit = {},
  onOpenWifiSettings: () -> Unit = {},
  onTurnOnBluetoothInternally: () -> Unit = {},
  onTurnOnWifiInternally: () -> Unit = {},
  onRefreshRadios: () -> Unit = {},
  onLockApp: () -> Unit = {},
  onConnectRadarPeer: (Peer) -> Unit = {},
  pairingProgressPeer: Peer? = null,
  onCancelPairingRequest: () -> Unit = {},
  incomingRadarConnection: Peer? = null,
  onAcceptRadarConnection: (Peer) -> Unit = {},
  onDeclineRadarConnection: (Peer) -> Unit = {},
  connectionFailedMessage: String? = null,
  onClearConnectionFailedMessage: () -> Unit = {},
  onSimulatePhonePeer: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var showQrScannerDialog by remember { mutableStateOf(false) }
  var showDeveloperDialog by remember { mutableStateOf(false) }
  var codeCopied by remember { mutableStateOf(false) }
  var isQrCardExpanded by remember { mutableStateOf(true) }
  var selectedCategoryFilter by remember { mutableStateOf("ALL") }

  val filteredPeers = remember(peers, searchQuery, selectedCategoryFilter) {
    val cleanPeers = peers.filterNot { peer ->
      val name = peer.displayName.trim()
      val id = peer.id.trim()
      val preview = peer.lastMessagePreview.trim()
      name.matches(Regex("^(?i)peer_[0-9a-f:]{2,8}$")) ||
      (name.startsWith("Peer_", ignoreCase = true) && name.length <= 10) ||
      id.matches(Regex("^(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}$")) ||
      name.equals("MeshNode_Beta", ignoreCase = true) ||
      id.contains("mesh_beta", ignoreCase = true) ||
      preview.contains("BLE Beacon detected", ignoreCase = true) ||
      preview.contains("Discovered via BLE Scanner", ignoreCase = true) ||
      preview.contains("P2P Beacon", ignoreCase = true)
    }
    val searchFiltered = if (searchQuery.isBlank()) cleanPeers
    else cleanPeers.filter {
      it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true)
    }
    when (selectedCategoryFilter) {
      "DIRECT" -> searchFiltered.filter { it.hopCount == 1 }
      "GROUPS" -> searchFiltered.filter { it.displayName.contains("Group", ignoreCase = true) || it.displayName.contains("Channel", ignoreCase = true) || it.displayName.contains("Mesh", ignoreCase = true) }
      "RELAY" -> searchFiltered.filter { it.hopCount > 1 || it.distanceEstimatedMeters > 5.0 || it.protocol.contains("Relay", ignoreCase = true) }
      else -> searchFiltered
    }
  }

  // Quick Top Bar QR Scanner Dialog
  if (showQrScannerDialog) {
    QuickQrScannerDialog(
      onDismiss = { showQrScannerDialog = false },
      onPairByCode = onPairByCode,
      onPairByQr = onPairByQr,
      onPeerConnected = { peer ->
        showQrScannerDialog = false
        onPeerSelected(peer.id)
      }
    )
  }

  // Developer & Architecture Overview Dialog
  if (showDeveloperDialog) {
    DeveloperArchitectureDialog(
      onDismiss = { showDeveloperDialog = false }
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("home_screen")
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // 1. TOP HEADER: Dragonfly Logo + "BLUEFI LINK" + QR Scanner Button near Bluefi line
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: App Logo & Branding
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            BluefiDragonflyLogo(
              size = 32.dp,
              primaryColor = MaterialTheme.colorScheme.primary,
              secondaryColor = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "BLUEFI LINK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "2.4 GHz Mesh",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Right: Action buttons (QR, Developer Info, Lock App, Node Pill)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = { showQrScannerDialog = true },
              modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                .testTag("top_bar_qr_scanner_button")
            ) {
              Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan QR Code",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = { showDeveloperDialog = true },
              modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                .testTag("top_bar_developer_button")
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About Bluefi & System Architecture",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }

            IconButton(
              onClick = onLockApp,
              modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                .testTag("top_bar_lock_button")
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock App",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp)
              )
            }

            // Real Connected Devices Count Pill
            Box(
              modifier = Modifier
                .background(
                  MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(16.dp)
                )
                .border(
                  1.dp,
                  if (peers.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(if (peers.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (peers.isEmpty()) "0 Nodes" else "${peers.size} Node(s)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  color = if (peers.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      // DIRECT INTERNAL RADIO ACTIVATION ALERT BANNER (When either radio is OFF)
      if (!radioState.isBluetoothEnabled || !radioState.isWifiEnabled) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B090C)),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF3366))
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .testTag("home_internal_radio_alert_banner")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color(0xFFFF1744), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "DIRECT RADIO HARDWARE ALERT",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFFFF5252)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Turn ON Bluetooth and Wi-Fi directly inside Bluefi (no settings redirection required):",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (!radioState.isBluetoothEnabled) {
                  Button(
                    onClick = onTurnOnBluetoothInternally,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("home_turn_on_bt_internal_button")
                  ) {
                    Text("Turn ON BT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
                if (!radioState.isWifiEnabled) {
                  Button(
                    onClick = onTurnOnWifiInternally,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("home_turn_on_wifi_internal_button")
                  ) {
                    Text("Turn ON Wi-Fi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Button(
                  onClick = {
                    onTurnOnBluetoothInternally()
                    onTurnOnWifiInternally()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1.2f).height(34.dp).testTag("home_turn_on_both_internal_button")
                ) {
                  Text("⚡ Turn ON Both", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
              }
            }
          }
        }
      }

      // 2. NETWORK MONITORING COMPONENT (Displays 'Online' or 'Disconnected' status based on active P2P mesh connectivity)
      item {
        Spacer(modifier = Modifier.height(6.dp))
        NetworkMonitoringComponent(
          radioState = radioState,
          connectedPeersCount = peers.count { it.isOnline },
          onOpenBluetoothSettings = onOpenBluetoothSettings,
          onOpenWifiSettings = onOpenWifiSettings,
          onRefreshRadios = onRefreshRadios
        )
      }



      // 3. SHOW QR CODE AND PIN CODE IN HOME PAGE (Allows another device to pair & communicate directly)
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_pairing_identity_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.QrCode,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "MY PAIRING QR & PIN CODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = "Node ID: ${userSettings.username}",
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Toggle expand / collapse QR
              Box(
                modifier = Modifier
                  .clickable { isQrCardExpanded = !isQrCardExpanded }
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (isQrCardExpanded) "HIDE QR" else "SHOW QR",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            // Expandable QR code section
            AnimatedVisibility(visible = isQrCardExpanded) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                // QR Code Display
                Box(
                  modifier = Modifier
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                ) {
                  QrCodeView(
                    payload = localQrPayload,
                    size = 170.dp,
                    moduleColor = Color.Black,
                    backgroundColor = Color.White
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = "Point second phone's top scanner at this QR code",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6-Digit PIN Code Box with Copy Button
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "OFFLINE 6-DIGIT PIN",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = localPairingCode,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  letterSpacing = 4.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Bluefi Pairing Code", localPairingCode))
                    codeCopied = true
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(34.dp).testTag("copy_home_pin_button")
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (codeCopied) "COPIED" else "COPY PIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                  onClick = { showQrScannerDialog = true },
                  modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                  Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Peer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }

      // 4. DUAL SPLIT CHAT ACTION BANNER
      item {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSplitChat)
            .testTag("split_chat_banner_button")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
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
                  .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.CallSplit,
                  contentDescription = "Split Chat",
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Dual Split Chat View",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Chat with 2 mesh peers simultaneously",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text(
                text = "OPEN SPLIT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        }
      }

      // 5. 2.4 GHz CARRIER FREQUENCY WAVEFORM (Positioned right near Dual Split Chat View)
      item {
        Spacer(modifier = Modifier.height(10.dp))
        FrequencyWaveCard(
          carrierFreq = networkStats.carrierWave,
          rangeKm = networkStats.effectiveRangeKm,
          baseRequirement = "${networkStats.baseFrequencyGhz} GHz",
          packetsRelayed = networkStats.packetsRelayedTotal
        )
      }

      // 6. SEARCH BAR
      item {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search connected nodes or chats...") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("peer_search_input")
        )
      }

      // 6. CONNECTED DEVICES / ACTIVE CHATS SECTION
      if (filteredPeers.isEmpty()) {
        item {
          Spacer(modifier = Modifier.height(14.dp))
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("zero_connected_devices_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                  .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.DevicesOther,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(26.dp)
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "NO CONNECTED PEERS YET",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Scan peer's QR code using the top scanner button or share your 6-digit PIN code above to establish direct 2.4 GHz communication.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
              )

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { showQrScannerDialog = true },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("pair_devices_empty_button")
                ) {
                  Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("SCAN QR CODE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                  onClick = onScanClicked,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("DISCOVER 2.4G", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      } else {
        item {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CONNECTED PEERS & CHATS (${filteredPeers.size})",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp,
              color = MaterialTheme.colorScheme.primary
            )

            Text(
              text = "Tap to chat directly",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          // Quick Category Filter Tabs: ALL, DIRECT, GROUPS, RELAY
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("ALL" to "All Nodes", "DIRECT" to "Direct P2P", "GROUPS" to "Groups", "RELAY" to "Relay Hops").forEach { (key, label) ->
              val isSelected = selectedCategoryFilter == key
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                  )
                  .clickable { selectedCategoryFilter = key }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // List of all connected peers
        items(
          items = filteredPeers,
          key = { it.id }
        ) { peer ->
          ConnectedPeerChatCard(
            peer = peer,
            onClick = { onPeerSelected(peer.id) },
            modifier = Modifier.padding(vertical = 5.dp)
          )
        }
        item {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }

    if (showDeveloperDialog) {
      DeveloperArchitectureDialog(onDismiss = { showDeveloperDialog = false })
    }

    // Outgoing Radar Pairing Progress Dialog (Device A)
    if (pairingProgressPeer != null) {
      val peer = pairingProgressPeer
      AlertDialog(
        onDismissRequest = onCancelPairingRequest,
        icon = {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
          )
        },
        title = {
          Text(
            text = "Requesting pairing with " + peer.displayName + "...",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
          )
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Sending PAIRING_REQUEST socket/GATT handshake payload containing your Username and Avatar...",
              fontSize = 12.sp,
              lineHeight = 16.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Waiting for " + peer.displayName + " to accept...",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        confirmButton = {},
        dismissButton = {
          TextButton(
            onClick = onCancelPairingRequest,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Cancel")
          }
        }
      )
    }

    // Device B Confirmation Popup:
    // Device B receives the incoming packet and triggers a native MaterialAlertDialog:
    // "Device A wants to pair with you. [ Accept ] | [ Reject ]"
    if (incomingRadarConnection != null) {
      val peer = incomingRadarConnection
      AlertDialog(
        onDismissRequest = { onDeclineRadarConnection(peer) },
        icon = {
          Box(
            modifier = Modifier
              .size(52.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
              .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Wifi,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }
        },
        title = {
          Text(
            text = "Device " + peer.displayName + " (@" + peer.username + ") wants to pair with you.",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
          )
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "@" + peer.username,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Proximity: " + String.format("%.1f", peer.distanceEstimatedMeters) + "m • " + peer.protocol,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Accepting will exchange profile credentials, save peer into SQLite/Room database, and open ChatActivity.",
              fontSize = 11.sp,
              lineHeight = 15.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = {
          Button(
            onClick = { onAcceptRadarConnection(peer) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("radar_accept_connection_btn")
          ) {
            Text("Accept", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { onDeclineRadarConnection(peer) },
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("radar_decline_connection_btn")
          ) {
            Text("Reject")
          }
        }
      )
    }

    // Response Handling Rejection Dialog:
    // Device A receives rejection: "Device is not paired. Sorry, request was declined!"
    if (connectionFailedMessage != null) {
      AlertDialog(
        onDismissRequest = onClearConnectionFailedMessage,
        icon = {
          Box(
            modifier = Modifier
              .size(46.dp)
              .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape)
              .border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
          }
        },
        title = {
          Text(
            text = "Device is not paired. Request was declined!",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        },
        text = {
          Text(
            text = connectionFailedMessage,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          Button(
            onClick = onClearConnectionFailedMessage,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("OK", fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  }
}

/**
 * Peer Card allowing direct communication with connected device.
 */
@Composable
fun ConnectedPeerChatCard(
  peer: Peer,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (peer.isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
      else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("peer_item_${peer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar with Online badge (ONLY show when device is connected, otherwise show nothing!)
      BadgedBox(
        badge = {
          if (peer.isOnline) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(
                  color = MaterialTheme.colorScheme.primary,
                  shape = CircleShape
                )
                .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            )
          }
        }
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              shape = CircleShape
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = peer.displayName.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Peer details
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = peer.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Connected Status pill: Only show when device is connected, otherwise show nothing!
          if (peer.isOnline) {
            Box(
              modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                  shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Connected",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Protocol, RSSI and Hop distance
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = if (peer.protocol.contains("BT")) Icons.Default.Bluetooth else Icons.Default.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = "${peer.protocol} • ${peer.rssi} dBm • ${peer.hopCount} hop",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
          )
        }

        if (peer.lastMessagePreview.isNotEmpty()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = peer.lastMessagePreview,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Instant "CHAT DIRECTLY" Button
      Box(
        modifier = Modifier
          .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "CHAT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = Color.Black
          )
        }
      }
    }
  }
}
