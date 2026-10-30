package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.Peer
import com.example.ui.components.AudioVoiceNoteBubble
import com.example.ui.components.ButterflyDeliveryIndicator
import com.example.ui.components.InAppVideoPlayerDialog
import com.example.ui.components.MediaEncodingHelper
import com.example.ui.components.PeerTypingIndicator
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitChatScreen(
  allPeers: List<Peer>,
  leftPeer: Peer?,
  rightPeer: Peer?,
  leftMessages: List<ChatMessage>,
  rightMessages: List<ChatMessage>,
  onSelectLeftPeer: (String) -> Unit,
  onSelectRightPeer: (String) -> Unit,
  onSendMessage: (String, String) -> Unit,
  onBack: () -> Unit,
  onOpenPairing: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Dual Split View, 1 = Left Node, 2 = Right Node
  var leftInput by remember { mutableStateOf("") }
  var rightInput by remember { mutableStateOf("") }
  var viewingVideoMessage by remember { mutableStateOf<ChatMessage?>(null) }
  val context = LocalContext.current

  val leftListState = rememberLazyListState()
  val rightListState = rememberLazyListState()

  LaunchedEffect(leftMessages.size) {
    if (leftMessages.isNotEmpty()) leftListState.animateScrollToItem(leftMessages.size - 1)
  }
  LaunchedEffect(rightMessages.size) {
    if (rightMessages.isNotEmpty()) rightListState.animateScrollToItem(rightMessages.size - 1)
  }

  val density = LocalDensity.current
  val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

  LaunchedEffect(isKeyboardOpen, leftMessages.size, rightMessages.size) {
    if (leftMessages.isNotEmpty()) {
      leftListState.animateScrollToItem(leftMessages.size - 1)
    }
    if (rightMessages.isNotEmpty()) {
      rightListState.animateScrollToItem(rightMessages.size - 1)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.CallSplit,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "SPLIT CHAT CONSOLE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Simultaneous Dual-Node P2P Link",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("split_chat_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          IconButton(
            onClick = onOpenPairing,
            modifier = Modifier.testTag("split_chat_pairing_btn")
          ) {
            Icon(
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = "PIN and QR Pairing",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    contentWindowInsets = WindowInsets.statusBars,
    modifier = modifier.testTag("split_chat_screen")
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .imePadding()
        .navigationBarsPadding()
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Peer Selection Header Bar (Switch active split channels)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Peer Pill
        PeerSelectorChip(
          label = "NODE A",
          peer = leftPeer,
          allPeers = allPeers,
          onSelect = onSelectLeftPeer,
          modifier = Modifier.weight(1f)
        )

        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = "Split",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Right Peer Pill
        PeerSelectorChip(
          label = "NODE B",
          peer = rightPeer,
          allPeers = allPeers,
          onSelect = onSelectRightPeer,
          modifier = Modifier.weight(1f)
        )
      }

      // Mode Selector Tabs (Dual Split Columns vs Focused Node tabs)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Dual Split Columns", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text(leftPeer?.displayName ?: "Node A", fontSize = 12.sp) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text(rightPeer?.displayName ?: "Node B", fontSize = 12.sp) }
        )
      }

      // Content based on tab
      when (selectedTab) {
        0 -> {
          // Dual Column Split View side-by-side or stacked
          Row(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f)
              .padding(6.dp)
          ) {
            // Left Chat Column
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(end = 3.dp)
                .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(12.dp)
                )
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                  RoundedCornerShape(12.dp)
                )
            ) {
              ChatColumnHeader(peer = leftPeer, channelTag = "CH 1 (BT 5.0)")
              LazyColumn(
                state = leftListState,
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(leftMessages) { msg ->
                  CompactMessageItem(message = msg, onVideoClick = { viewingVideoMessage = it })
                }
              }
              AnimatedVisibility(
                visible = rightInput.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
              ) {
                PeerTypingIndicator(peerName = rightPeer?.displayName ?: "Node B")
              }
              ChatColumnInput(
                value = leftInput,
                onValueChange = { leftInput = it },
                onSend = {
                  leftPeer?.let { p ->
                    onSendMessage(p.id, leftInput)
                    leftInput = ""
                  }
                },
                placeholder = "Msg Node A..."
              )
            }

            // Right Chat Column
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(start = 3.dp)
                .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(12.dp)
                )
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                  RoundedCornerShape(12.dp)
                )
            ) {
              ChatColumnHeader(peer = rightPeer, channelTag = "CH 2 (Wi-Fi P2P)")
              LazyColumn(
                state = rightListState,
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(rightMessages) { msg ->
                  CompactMessageItem(message = msg, onVideoClick = { viewingVideoMessage = it })
                }
              }
              AnimatedVisibility(
                visible = leftInput.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
              ) {
                PeerTypingIndicator(peerName = leftPeer?.displayName ?: "Node A")
              }
              ChatColumnInput(
                value = rightInput,
                onValueChange = { rightInput = it },
                onSend = {
                  rightPeer?.let { p ->
                    onSendMessage(p.id, rightInput)
                    rightInput = ""
                  }
                },
                placeholder = "Msg Node B..."
              )
            }
          }
        }
        1 -> {
          // Full pane for Left Peer
          SingleChatPane(
            peer = leftPeer,
            messages = leftMessages,
            input = leftInput,
            isPeerTyping = rightInput.isNotBlank(),
            onInputChange = { leftInput = it },
            onSend = {
              leftPeer?.let { p ->
                onSendMessage(p.id, leftInput)
                leftInput = ""
              }
            },
            onVideoClick = { viewingVideoMessage = it }
          )
        }
        2 -> {
          // Full pane for Right Peer
          SingleChatPane(
            peer = rightPeer,
            messages = rightMessages,
            input = rightInput,
            isPeerTyping = leftInput.isNotBlank(),
            onInputChange = { rightInput = it },
            onSend = {
              rightPeer?.let { p ->
                onSendMessage(p.id, rightInput)
                rightInput = ""
              }
            },
            onVideoClick = { viewingVideoMessage = it }
          )
        }
      }
    }
  }

  // Fullscreen video playback dialog for Split Chat
  if (viewingVideoMessage != null) {
    InAppVideoPlayerDialog(
      mediaContent = viewingVideoMessage!!.mediaContent,
      senderName = viewingVideoMessage!!.senderName,
      timestamp = viewingVideoMessage!!.timestamp,
      onDismiss = { viewingVideoMessage = null }
    )
  }
}

@Composable
private fun PeerSelectorChip(
  label: String,
  peer: Peer?,
  allPeers: List<Peer>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        .clickable { expanded = true }
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = peer?.displayName ?: "Choose Node",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    androidx.compose.material3.DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      allPeers.forEach { p ->
        androidx.compose.material3.DropdownMenuItem(
          text = { Text("${p.displayName} (${p.protocol})") },
          onClick = {
            onSelect(p.id)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun ChatColumnHeader(peer: Peer?, channelTag: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = peer?.displayName ?: "No peer",
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (peer?.isOnline == true) {
          Spacer(modifier = Modifier.width(4.dp))
          Box(
            modifier = Modifier
              .size(6.dp)
              .background(MaterialTheme.colorScheme.primary, CircleShape)
          )
        }
      }
      Text(
        text = channelTag,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = FontFamily.Monospace
      )
    }
    Icon(
      imageVector = Icons.Default.Lock,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(12.dp)
    )
  }
}

@Composable
private fun CompactMessageItem(
  message: ChatMessage,
  onVideoClick: (ChatMessage) -> Unit = {}
) {
  val isMe = message.isFromMe
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(message.burnsInTimestamp) {
    if (message.burnsInTimestamp > 0) {
      while (isActive) {
        now = System.currentTimeMillis()
        delay(500L)
      }
    }
  }

  val isBurnTimerActive = message.burnsInTimestamp > 0
  val remainingSeconds = if (isBurnTimerActive) {
    ((message.burnsInTimestamp - now) / 1000).coerceAtLeast(0)
  } else 0L

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Box(
      modifier = Modifier
        .background(
          if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
          RoundedCornerShape(8.dp)
        )
        .border(
          0.5.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
          RoundedCornerShape(8.dp)
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Column {
        if (message.mediaType == "audio") {
          AudioVoiceNoteBubble(
            messageId = message.id,
            mediaContent = message.mediaContent,
            isFromMe = isMe
          )
        } else if (message.mediaType == "video") {
          val parts = remember(message.mediaContent) {
            if (message.mediaContent.startsWith("video_meta|")) message.mediaContent.split("|") else emptyList()
          }
          val thumb = remember(parts) {
            if (parts.size >= 3) MediaEncodingHelper.decodeBase64ToBitmap(parts[2]) else null
          }
          Box(
            modifier = Modifier
              .size(110.dp, 75.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { onVideoClick(message) },
            contentAlignment = Alignment.Center
          ) {
            if (thumb != null) {
              Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = "Encrypted video preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.PlayCircle, contentDescription = "Play Video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
        }

        if (message.text.isNotEmpty() && message.mediaType != "audio") {
          Text(
            text = message.text,
            fontSize = 11.sp,
            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Text(
            text = "${message.packetCount} PKT",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.primary
          )
          if (isBurnTimerActive) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (remainingSeconds > 0) "🔥 ${remainingSeconds}s" else "🔥",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFFFF7043)
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          val isDelivered = !isMe || (message.status == MessageDeliveryStatus.DELIVERED || message.status == MessageDeliveryStatus.READ)
          ButterflyDeliveryIndicator(
            isDelivered = isDelivered,
            width = 13.dp,
            height = 10.dp,
            tint = if (isDelivered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
          )
        }
      }
    }
  }
}

@Composable
private fun ChatColumnInput(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  placeholder: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text(placeholder, fontSize = 11.sp) },
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
      ),
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.width(4.dp))
    IconButton(
      onClick = onSend,
      modifier = Modifier
        .size(36.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Send,
        contentDescription = "Send",
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
private fun SingleChatPane(
  peer: Peer?,
  messages: List<ChatMessage>,
  input: String,
  isPeerTyping: Boolean = false,
  onInputChange: (String) -> Unit,
  onSend: () -> Unit,
  onVideoClick: (ChatMessage) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(messages) { msg ->
        MessageBubble(
          message = msg,
          onClick = {},
          onVideoClick = onVideoClick
        )
      }
    }
    AnimatedVisibility(
      visible = isPeerTyping,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      PeerTypingIndicator(peerName = peer?.displayName ?: "Peer")
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        placeholder = { Text("Message ${peer?.displayName ?: ""}...") },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.weight(1f)
      )
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(
        onClick = onSend,
        modifier = Modifier
          .background(MaterialTheme.colorScheme.primary, CircleShape)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send",
          tint = MaterialTheme.colorScheme.onPrimary
        )
      }
    }
  }
}
