package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Peer
import com.example.ui.components.QuickQrScannerDialog

enum class InboxFilterType {
  ALL,
  UNREAD,
  FAVOURITES,
  GROUPS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsInboxScreen(
  peers: List<Peer>,
  onPeerSelected: (String) -> Unit,
  onOpenQrScanner: () -> Unit = {},
  onPairByCode: ((String, (Result<Peer>) -> Unit) -> Unit)? = null,
  onPairByQr: ((String, (Result<Peer>) -> Unit) -> Unit)? = null,
  onMarkAllAsRead: () -> Unit,
  onDeleteChat: (String) -> Unit,
  onCreateNewChat: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf(InboxFilterType.ALL) }
  var showMenu by remember { mutableStateOf(false) }
  var showNewChatDialog by remember { mutableStateOf(false) }
  var showQrScannerDialog by remember { mutableStateOf(false) }
  var chatToDelete by remember { mutableStateOf<Peer?>(null) }

  // Theme-aware Accent & Container Palette
  val primaryAccent = MaterialTheme.colorScheme.primary
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
  val cardContainerColor = MaterialTheme.colorScheme.surfaceVariant
  val surfaceColor = MaterialTheme.colorScheme.surface

  // Filter out raw unidentified BLE beacons (e.g. Peer_0D4, Peer_E:B8, MeshNode_Beta, raw MAC addresses)
  val visiblePeers = remember(peers) {
    peers.filterNot { peer ->
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
  }

  // Filter peers based on Search and Selected Filter Chip
  val unreadCount = visiblePeers.sumOf { it.unreadCount }
  val groupCount = visiblePeers.count {
    it.id.startsWith("group_") ||
    it.id.startsWith("channel_") ||
    it.displayName.contains("Group", ignoreCase = true) ||
    it.displayName.contains("Team", ignoreCase = true) ||
    it.displayName.contains("Mesh", ignoreCase = true) ||
    it.displayName.contains("Channel", ignoreCase = true) ||
    it.displayName.contains("Broadcast", ignoreCase = true) ||
    it.displayName.contains("Operations", ignoreCase = true) ||
    it.displayName.contains("Unit", ignoreCase = true)
  }

  val filteredPeers = visiblePeers.filter { peer ->
    val matchesSearch = searchQuery.isBlank() ||
      peer.displayName.contains(searchQuery, ignoreCase = true) ||
      peer.lastMessagePreview.contains(searchQuery, ignoreCase = true) ||
      peer.username.contains(searchQuery, ignoreCase = true)

    val isGroup = peer.id.startsWith("group_") ||
      peer.id.startsWith("channel_") ||
      peer.displayName.contains("Group", ignoreCase = true) ||
      peer.displayName.contains("Team", ignoreCase = true) ||
      peer.displayName.contains("Mesh", ignoreCase = true) ||
      peer.displayName.contains("Channel", ignoreCase = true) ||
      peer.displayName.contains("Broadcast", ignoreCase = true) ||
      peer.displayName.contains("Operations", ignoreCase = true) ||
      peer.displayName.contains("Unit", ignoreCase = true)

    val isFav = peer.id.contains("relay", ignoreCase = true) ||
      peer.id.contains("hardware", ignoreCase = true) ||
      peer.id.contains("transceiver", ignoreCase = true) ||
      peer.rssi > -65 ||
      peer.isOnline

    val matchesFilter = when (activeFilter) {
      InboxFilterType.ALL -> true
      InboxFilterType.UNREAD -> peer.unreadCount > 0
      InboxFilterType.FAVOURITES -> isFav
      InboxFilterType.GROUPS -> isGroup
    }

    matchesSearch && matchesFilter
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showNewChatDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
          .padding(bottom = 12.dp, end = 8.dp)
          .testTag("wa_fab_new_chat")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Chat,
          contentDescription = "New chat",
          modifier = Modifier.size(24.dp)
        )
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // 1. WhatsApp Top Bar: "Chats" + Camera + 3-dots Menu
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Chats",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          ),
          modifier = Modifier.testTag("chats_inbox_title")
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Top WhatsApp Camera / QR Action Button
          IconButton(
            onClick = {
              if (onPairByQr != null) {
                showQrScannerDialog = true
              } else {
                onOpenQrScanner()
              }
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
              .testTag("top_wa_camera_btn")
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Camera Scanner",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // 3-dots Overflow Menu
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.testTag("top_wa_menu_btn")
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }

            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false },
              modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
              DropdownMenuItem(
                text = { Text("New group") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                onClick = {
                  showMenu = false
                  showNewChatDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Starred messages") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                onClick = {
                  showMenu = false
                  activeFilter = InboxFilterType.FAVOURITES
                }
              )
              DropdownMenuItem(
                text = { Text("Scan QR Code") },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                onClick = {
                  showMenu = false
                  if (onPairByQr != null) {
                    showQrScannerDialog = true
                  } else {
                    onOpenQrScanner()
                  }
                }
              )
              DropdownMenuItem(
                text = { Text("Mark all as read") },
                leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                onClick = {
                  showMenu = false
                  onMarkAllAsRead()
                }
              )
            }
          }
        }
      }

      // 2. WhatsApp Search Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )

          Spacer(modifier = Modifier.width(10.dp))

          Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Search or start a new chat",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 14.sp
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
              ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("inbox_search_input")
            )
          }

          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // 3. Filter Chips Row: [All] [Unread 61] [Favourites] [Groups 6] [+]
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          InboxFilterChip(
            label = "All",
            isSelected = activeFilter == InboxFilterType.ALL,
            onClick = { activeFilter = InboxFilterType.ALL }
          )
        }
        item {
          InboxFilterChip(
            label = if (unreadCount > 0) "Unread $unreadCount" else "Unread",
            isSelected = activeFilter == InboxFilterType.UNREAD,
            onClick = { activeFilter = InboxFilterType.UNREAD }
          )
        }
        item {
          InboxFilterChip(
            label = "Favourites",
            isSelected = activeFilter == InboxFilterType.FAVOURITES,
            onClick = { activeFilter = InboxFilterType.FAVOURITES }
          )
        }
        item {
          InboxFilterChip(
            label = if (groupCount > 0) "Groups $groupCount" else "Groups",
            isSelected = activeFilter == InboxFilterType.GROUPS,
            onClick = { activeFilter = InboxFilterType.GROUPS }
          )
        }
        item {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .combinedClickable(
                onClick = { showNewChatDialog = true }
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add filter",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 4. Chat History List
      if (filteredPeers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotEmpty()) "No chats matching '$searchQuery'" else "No messages in this folder",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 15.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("inbox_chat_list"),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredPeers, key = { it.id }) { peer ->
            ChatInboxItemRow(
              peer = peer,
              onClick = { onPeerSelected(peer.id) },
              onLongClick = { chatToDelete = peer }
            )
          }
        }
      }
    }
  }

  // Dialog: Confirm Delete Chat
  chatToDelete?.let { peer ->
    AlertDialog(
      onDismissRequest = { chatToDelete = null },
      title = { Text("Delete Chat?") },
      text = { Text("Are you sure you want to delete chat with ${peer.displayName}? Messages and keys will be wiped from this device.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteChat(peer.id)
            chatToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { chatToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dialog: Start New Chat
  if (showNewChatDialog) {
    var newName by remember { mutableStateOf("") }
    var initialMsg by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showNewChatDialog = false },
      title = { Text("Start New Chat", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Enter contact name, phone number, or 2.4 GHz node ID:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = { Text("e.g. +91 98154 xxxxx or Node_Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = initialMsg,
            onValueChange = { initialMsg = it },
            placeholder = { Text("Initial message (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              onCreateNewChat(newName, initialMsg)
              showNewChatDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Start Chat", color = MaterialTheme.colorScheme.onPrimary)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewChatDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showQrScannerDialog && onPairByCode != null && onPairByQr != null) {
    QuickQrScannerDialog(
      onDismiss = { showQrScannerDialog = false },
      onPairByCode = { code, callback ->
        onPairByCode(code, callback)
      },
      onPairByQr = { payload, callback ->
        onPairByQr(payload, callback)
      },
      onPeerConnected = { peer ->
        showQrScannerDialog = false
        onPeerSelected(peer.id)
      }
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxFilterChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val backgroundColor = if (isSelected) {
    primaryColor.copy(alpha = 0.18f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
  }

  val textColor = if (isSelected) {
    primaryColor
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  val borderModifier = if (isSelected) {
    Modifier.border(1.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
  } else {
    Modifier
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = backgroundColor,
    modifier = borderModifier
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(onClick = onClick)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 13.sp,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInboxItemRow(
  peer: Peer,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val hasUnread = peer.unreadCount > 0

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (hasUnread) primaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("inbox_item_${peer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
    // Contact / Group Circular Avatar
    ChatContactAvatar(peer = peer)

    Spacer(modifier = Modifier.width(14.dp))

    // Middle column: Display Name + Message Preview
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = peer.displayName,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(3.dp))

      // Subtitle Line: Icons + Message Preview
      ChatMessagePreviewLine(peer = peer)
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Right column: Timestamp + Unread Badge
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = "Yesterday",
        fontSize = 12.sp,
        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
        color = if (hasUnread) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )

      if (hasUnread) {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(primaryColor),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${peer.unreadCount}",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
  }
}

@Composable
fun ChatContactAvatar(peer: Peer) {
  val isGroup = peer.id.startsWith("group_") ||
    peer.id.startsWith("channel_") ||
    peer.displayName.contains("Group", ignoreCase = true) ||
    peer.displayName.contains("Team", ignoreCase = true) ||
    peer.displayName.contains("Operations", ignoreCase = true) ||
    peer.displayName.contains("Unit", ignoreCase = true)

  val isChannel = peer.id.startsWith("channel_") ||
    peer.displayName.contains("Broadcast", ignoreCase = true) ||
    peer.displayName.contains("Channel", ignoreCase = true)

  val isHardware = peer.id.contains("transceiver", ignoreCase = true) ||
    peer.id.contains("hardware", ignoreCase = true) ||
    peer.displayName.contains("Transceiver", ignoreCase = true)

  val avatarPalette = listOf(
    Color(0xFF128C7E),
    Color(0xFF00796B),
    Color(0xFF0288D1),
    Color(0xFF7B1FA2),
    Color(0xFFC2185B),
    Color(0xFFE65100),
    Color(0xFF388E3C),
    Color(0xFF455A64)
  )
  val color = avatarPalette[kotlin.math.abs(peer.displayName.hashCode()) % avatarPalette.size]

  Box(
    modifier = Modifier.size(50.dp)
  ) {
    Box(
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape)
        .background(color),
      contentAlignment = Alignment.Center
    ) {
      when {
        isChannel -> {
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = "Channel",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
        isGroup -> {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = "Group",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
        isHardware -> {
          Icon(
            imageVector = Icons.Default.Devices,
            contentDescription = "Hardware",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
        else -> {
          val avatarEmojis = listOf("🛸", "⚡", "👾", "🛡️", "🔮", "🛰️")
          val emoji = avatarEmojis.getOrNull(peer.avatarIndex)
          if (emoji != null) {
            Text(
              text = emoji,
              fontSize = 24.sp
            )
          } else {
            val initials = peer.displayName.split(" ")
              .mapNotNull { it.firstOrNull()?.toString() }
              .take(2)
              .joinToString("")
              .uppercase()
            Text(
              text = if (initials.isNotBlank()) initials else peer.displayName.take(1).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          }
        }
      }
    }

    if (peer.isOnline) {
      Box(
        modifier = Modifier
          .size(13.dp)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
  }
}

@Composable
fun ChatMessagePreviewLine(peer: Peer) {
  val preview = peer.lastMessagePreview
  val waBlueCheck = Color(0xFF53BDEB)

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    when {
      preview.contains("Location", ignoreCase = true) -> {
        Icon(
          imageVector = Icons.Default.DoneAll,
          contentDescription = "Delivered",
          tint = waBlueCheck,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = "Location",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = "Location",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      preview.contains("Photo", ignoreCase = true) || preview.contains("Image", ignoreCase = true) -> {
        Icon(
          imageVector = Icons.Default.DoneAll,
          contentDescription = "Read",
          tint = waBlueCheck,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.PhotoCamera,
          contentDescription = "Photo",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Photo",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      preview.contains("deleted", ignoreCase = true) -> {
        Icon(
          imageVector = Icons.Default.Block,
          contentDescription = "Deleted",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = "This message was deleted",
          fontStyle = FontStyle.Italic,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      preview.startsWith("http://") || preview.startsWith("https://") -> {
        Icon(
          imageVector = Icons.Default.Link,
          contentDescription = "Link",
          tint = Color(0xFF0288D1),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = preview,
          fontSize = 13.sp,
          color = Color(0xFF0288D1),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      else -> {
        Icon(
          imageVector = Icons.Default.DoneAll,
          contentDescription = "Delivered",
          tint = if (peer.unreadCount == 0) waBlueCheck else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = preview.ifBlank { "Encrypted P2P connection active" },
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
