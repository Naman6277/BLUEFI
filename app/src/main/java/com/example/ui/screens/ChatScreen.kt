package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.UniversalFileTransferManager
import com.example.data.crypto.PublicStorageManager
import androidx.compose.material.icons.filled.Download
import com.example.data.crypto.FileHeaderPacket
import com.example.data.crypto.FileTransferProgressState
import com.example.data.crypto.ChunkedVideoEncryptor
import com.example.data.crypto.VideoProgressState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.model.ChatMessage
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.Peer
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.ui.components.AudioVoiceNoteBubble
import com.example.ui.components.AudioVoicePlayer
import com.example.ui.components.AudioVoiceRecorder
import com.example.ui.components.ButterflyDeliveryIndicator
import com.example.ui.components.ButterflyFlutteringIcon
import com.example.ui.components.FullscreenImageViewerDialog
import com.example.ui.components.InAppVideoPlayerDialog
import com.example.ui.components.MediaEncodingHelper
import com.example.ui.components.PeerTypingIndicator
import com.example.ui.components.VoiceRecordingBar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  peer: Peer?,
  messages: List<ChatMessage>,
  onBack: () -> Unit,
  onSendMessage: (String, String, String) -> Unit,
  onMessageClick: (ChatMessage) -> Unit,
  onClearChat: () -> Unit,
  keyFingerprint: String? = null,
  isPeerTyping: Boolean = false,
  onTyping: (Boolean) -> Unit = {},
  onSimulatePeerTyping: () -> Unit = {},
  onVerifyAndConnectPeer: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var inputText by remember { mutableStateOf("") }
  var showAttachMenu by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()

  // Voice recording state
  val voiceRecorder = remember { AudioVoiceRecorder(scope) }
  val isRecordingAudio by voiceRecorder.isRecording.collectAsState()
  val recordDurationSec by voiceRecorder.durationSeconds.collectAsState()
  val recordAmplitude by voiceRecorder.currentAmplitude.collectAsState()

  val audioPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    voiceRecorder.startRecording(context)
  }

  val density = LocalDensity.current
  val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

  // Scroll to bottom whenever keyboard opens, messages change, or peer is typing
  LaunchedEffect(isKeyboardOpen, messages.size, isPeerTyping) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  // Fullscreen viewer states for encrypted photos & videos
  var viewingImageMessage by remember { mutableStateOf<ChatMessage?>(null) }
  var viewingVideoMessage by remember { mutableStateOf<ChatMessage?>(null) }

  // Photo Picker Launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val base64 = MediaEncodingHelper.uriToCompressedBase64Image(context, uri, maxDimension = 720)
      if (base64 != null) {
        onSendMessage("📷 Encrypted Photo", "image", base64)
      }
      showAttachMenu = false
    }
  }

  // Video Picker Launcher - preserves local file path and encodes for P2P transfer with real-time 64KB AES-GCM encryption
  val videoPickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val saveResult = MediaEncodingHelper.saveVideoFromUri(context, uri)
      if (saveResult != null) {
        val mediaContent = "video_meta|${saveResult.durationStr}|${saveResult.thumbnailBase64}|${uri.lastPathSegment ?: "video.mp4"}|${saveResult.localPath}|${saveResult.smallBase64Video}"
        val tempMsgId = "msg_vid_${System.currentTimeMillis()}"
        onSendMessage("🎥 Encrypted Video (${saveResult.durationStr})", "video", mediaContent)

        // Launch 64KB chunked AES-256-GCM encryption & progress streaming engine
        val localFile = java.io.File(saveResult.localPath)
        if (localFile.exists()) {
          scope.launch {
            ChunkedVideoEncryptor.encryptAndChunkVideo(
              context = context,
              messageId = tempMsgId,
              videoFile = localFile
            )
          }
        }
      } else {
        val (duration, thumbnailBase64) = MediaEncodingHelper.extractVideoMeta(context, uri)
        val mediaContent = "video_meta|$duration|$thumbnailBase64|${uri.lastPathSegment ?: "video.mp4"}"
        onSendMessage("🎥 Encrypted Video ($duration)", "video", mediaContent)
      }
      showAttachMenu = false
    }
  }

  // Camera Snapshot Launcher
  val cameraSnapshotLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      val base64 = MediaEncodingHelper.bitmapToBase64Jpeg(bitmap, maxDimension = 720)
      onSendMessage("📷 Live Camera Snap", "image", base64)
      showAttachMenu = false
    }
  }

  // Universal Any-Format File Picker Launcher (Unlimited MB Size)
  val anyFilePickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      val (fileName, fileSizeBytes) = UniversalFileTransferManager.queryFileMetadata(context, uri)
      val ext = fileName.substringAfterLast(".", "bin").uppercase()
      val formattedSize = UniversalFileTransferManager.formatFileSize(fileSizeBytes)
      val tempMsgId = "msg_file_${System.currentTimeMillis()}"

      // Format: "file_meta|$fileName|$ext|$fileSizeBytes|$formattedSize|$localFilePath|$mimeType"
      val initialMediaContent = "file_meta|$fileName|$ext|$fileSizeBytes|$formattedSize||*/*"
      onSendMessage("📁 Encrypted File: $fileName ($formattedSize)", "file", initialMediaContent)
      showAttachMenu = false

      // Stream file in 64KB chunks with AES-256-GCM encryption
      scope.launch {
        UniversalFileTransferManager.streamAndSendAnyFile(
          context = context,
          messageId = tempMsgId,
          uri = uri
        )
      }
    }
  }

  // Auto-scroll on new message
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val quickStickers = listOf(
    "Dragonfly Node" to "🛸",
    "RF 2.4GHz" to "📡",
    "Encrypted Key" to "🔐",
    "Shield Firewall" to "🛡️",
    "Packet ACK" to "⚡",
    "Mesh Hop OK" to "📶"
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          if (peer != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(38.dp)) {
                val avatarEmojis = listOf("🛸", "⚡", "👾", "🛡️", "🔮", "🛰️")
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                      CircleShape
                    )
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = avatarEmojis.getOrElse(peer.avatarIndex) { peer.displayName.take(2).uppercase() },
                    fontSize = 18.sp
                  )
                }
                // Connected dot logo: ONLY show when device is connected, otherwise show nothing!
                if (peer.isOnline) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .align(Alignment.BottomEnd)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                      .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  // Connected dot: ONLY show when device is connected, otherwise show nothing!
                  if (peer.isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "E2EE Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                  )
                }
                Text(
                  text = if (peer.isOnline) {
                    "${peer.protocol} • Connected • ${peer.rssi} dBm"
                  } else {
                    "${peer.protocol} • Disconnected"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  color = if (peer.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
              }
            }
          } else {
            Text("P2P Secure Channel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("chat_back_button")
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
            onClick = { showMenu = !showMenu },
            modifier = Modifier.testTag("chat_more_options")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Test Butterfly Typing 🦋") },
              leadingIcon = {
                ButterflyFlutteringIcon(width = 16.dp, height = 12.dp)
              },
              onClick = {
                showMenu = false
                onSimulatePeerTyping()
              }
            )
            DropdownMenuItem(
              text = { Text("Clear Chat Messages") },
              leadingIcon = {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
              },
              onClick = {
                showMenu = false
                onClearChat()
              }
            )
          }
        }
      )
    },
    contentWindowInsets = WindowInsets.statusBars,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .imePadding()
    ) {
      // E2EE Channel Security Banner
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "AES-256 P2P DIRECT CHANNEL",
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = "🔒 Direct Offline Link",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Messages List
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(messages, key = { it.id }) { message ->
          MessageBubble(
            message = message,
            onClick = { onMessageClick(message) },
            onImageClick = { viewingImageMessage = it },
            onVideoClick = { viewingVideoMessage = it }
          )
        }
      }

      // Attachment Action Menu (Photos, Videos, Camera, Stickers)
      if (showAttachMenu) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "SECURE MEDIA TRANSMISSION OVER BLUEFI",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              // Send Photo (Gallery)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  }
                  .padding(8.dp)
                  .testTag("send_photo_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Image, contentDescription = "Send Photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Send Video (Gallery)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    videoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                  }
                  .padding(8.dp)
                  .testTag("send_video_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Videocam, contentDescription = "Send Video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Send Any-Format Document / File (Unlimited Size)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    anyFilePickerLauncher.launch("*/*")
                  }
                  .padding(8.dp)
                  .testTag("send_any_file_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.AttachFile, contentDescription = "Send Any File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Demo Stream Video (Instant offline playable video test)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    val sampleFile = MediaEncodingHelper.createSampleDemoVideo(context)
                    val (durationStr, thumbBase64) = MediaEncodingHelper.extractVideoMeta(context, android.net.Uri.fromFile(sampleFile))
                    val mediaContent = "video_meta|$durationStr|$thumbBase64|${sampleFile.name}|${sampleFile.absolutePath}|"
                    onSendMessage("🎥 Encrypted Video ($durationStr)", "video", mediaContent)
                    showAttachMenu = false
                  }
                  .padding(8.dp)
                  .testTag("send_demo_video_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.VideoLibrary, contentDescription = "Test Video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Demo Vid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Voice Note
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    showAttachMenu = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                      voiceRecorder.startRecording(context)
                    } else {
                      audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                  .padding(8.dp)
                  .testTag("send_voice_note_menu_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Mic, contentDescription = "Voice Note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Camera Snapshot
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    cameraSnapshotLauncher.launch(null)
                  }
                  .padding(8.dp)
                  .testTag("take_camera_photo_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }

              // Stickers
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clickable {
                    onSendMessage("Dragonfly Node", "sticker", "🛸")
                    showAttachMenu = false
                  }
                  .padding(8.dp)
                  .testTag("quick_sticker_shortcut_button")
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🛸", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sticker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Sticker Horizontal Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(quickStickers) { (name, emoji) ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                      RoundedCornerShape(8.dp)
                    )
                    .clickable {
                      onSendMessage(name, "sticker", emoji)
                      showAttachMenu = false
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = name, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }
      }

      // Subtle 'typing...' animation with butterfly fluttering icon above the text input field
      AnimatedVisibility(
        visible = isPeerTyping,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        PeerTypingIndicator(
          peerName = peer?.displayName ?: "Peer",
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Offline Waiting Area Banner: Notifies user that data stays in waiting area until peer connects
      val waitingCount = remember(messages) {
        messages.count { it.isFromMe && it.status == MessageDeliveryStatus.WAITING_IN_QUEUE }
      }
      if (peer != null && !peer.isOnline) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF211403))
            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Text(text = "⏳", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (waitingCount > 0) "$waitingCount message(s) in waiting area" else "Peer not connected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFCA28)
              )
              Text(
                text = "Data stays in waiting area until peer connects, then automatically sends.",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFFFFE082)
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onVerifyAndConnectPeer,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFFFB300),
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp).testTag("verify_deliver_waiting_button")
          ) {
            Text("Verify & Send", fontSize = 10.sp, fontWeight = FontWeight.Black)
          }
        }
      }

      // Input Bar (always stays above the keypad with navigationBarsPadding)
      if (isRecordingAudio) {
        VoiceRecordingBar(
          durationSeconds = recordDurationSec,
          amplitude = recordAmplitude,
          onCancel = { voiceRecorder.cancelRecording() },
          onSend = {
            val result = voiceRecorder.stopRecording()
            if (result != null) {
              val mediaContent = "audio_meta|${result.durationSeconds}|${result.base64Data}|${result.filePath}"
              onSendMessage("🎙️ Voice note (${result.durationSeconds}s)", "audio", mediaContent)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
        )
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Attachment toggle button (photos, videos, camera, stickers)
          IconButton(
            onClick = { showAttachMenu = !showAttachMenu },
            modifier = Modifier.testTag("attach_media_button")
          ) {
            Icon(
              imageVector = if (showAttachMenu) Icons.Default.Add else Icons.Default.AttachFile,
              contentDescription = "Attach Photos or Videos",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          OutlinedTextField(
            value = inputText,
            onValueChange = {
              inputText = it
              onTyping(it.isNotBlank())
            },
            placeholder = { Text("2.4 GHz Encrypted message...") },
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
            keyboardActions = KeyboardActions(
              onSend = {
                if (inputText.isNotBlank()) {
                  onTyping(false)
                  onSendMessage(inputText, "text", "")
                  inputText = ""
                }
              }
            ),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field")
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Send or Voice Record Action Button
          if (inputText.isNotBlank()) {
            IconButton(
              onClick = {
                if (inputText.isNotBlank()) {
                  onTyping(false)
                  onSendMessage(inputText, "text", "")
                  inputText = ""
                }
              },
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .testTag("send_message_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          } else {
            // Microphone button to record encrypted audio voice notes
            IconButton(
              onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                  voiceRecorder.startRecording(context)
                } else {
                  audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              },
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .testTag("record_audio_note_button")
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Record Voice Note",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }

  // Fullscreen Photo Viewer Dialog
  if (viewingImageMessage != null) {
    FullscreenImageViewerDialog(
      mediaContent = viewingImageMessage!!.mediaContent,
      senderName = viewingImageMessage!!.senderName,
      timestamp = viewingImageMessage!!.timestamp,
      keyFingerprint = keyFingerprint,
      onDismiss = { viewingImageMessage = null }
    )
  }

  // In-App Video Player Dialog
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
fun MessageBubble(
  message: ChatMessage,
  onClick: () -> Unit,
  onImageClick: (ChatMessage) -> Unit = {},
  onVideoClick: (ChatMessage) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isMe = message.isFromMe
  // Strict Dark Palette: Fast Electric Blue (#2563EB) for outgoing, Dark Cobalt (#1E293B) for incoming
  val bubbleBg = if (isMe) Color(0xFF2563EB) else Color(0xFF1E293B)
  val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

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

  // Decode bitmap if message is an image
  val imageBitmap = remember(message.mediaContent, message.mediaType) {
    if (message.mediaType == "image" && message.mediaContent.isNotBlank()) {
      MediaEncodingHelper.decodeBase64ToBitmap(message.mediaContent)
    } else null
  }

  // Extract video duration and thumbnail if message is a video
  val (videoDuration, videoThumbBitmap) = remember(message.mediaContent, message.mediaType) {
    if (message.mediaType == "video" && message.mediaContent.isNotBlank()) {
      val parts = message.mediaContent.split("|")
      val duration = if (parts.size >= 2) parts[1] else "0:12"
      val thumb = if (parts.size >= 3) MediaEncodingHelper.decodeBase64ToBitmap(parts[2]) else null
      Pair(duration, thumb)
    } else {
      Pair("0:12", null)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = bubbleBg),
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      ),
      modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("message_bubble_${message.id}")
    ) {
      Column(
        modifier = Modifier
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .widthIn(max = 280.dp)
      ) {
        // 1. Photo Media Rendering
        if (message.mediaType == "image") {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clickable { onImageClick(message) }
              .testTag("chat_photo_preview_${message.id}")
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              if (imageBitmap != null) {
                Image(
                  bitmap = imageBitmap.asImageBitmap(),
                  contentDescription = "Encrypted photo",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Encrypted P2P Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
              }

              // Encryption badge overlay
              Box(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .padding(6.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("AES-256", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                }
              }

              // Dedicated Save to Phone Storage Button (Pictures/Bluefy)
              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(6.dp)
                  .size(34.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                  .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                  .clickable {
                    val raw = if (message.mediaContent.contains(",")) message.mediaContent.substringAfter(",") else message.mediaContent
                    val imgFile = com.example.ui.components.MediaEncodingHelper.saveBase64ToFile(
                      context, "photos", raw, "photo_${message.id}", "jpg"
                    )
                    com.example.data.crypto.HighSpeedTransferManager.saveToPublicStorageAndScanAsync(
                      context = context,
                      sourceFile = imgFile,
                      originalFileName = "Bluefy_Photo_${message.id.takeLast(6)}.jpg",
                      fileExtension = "jpg"
                    )
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Save to Phone Storage",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 2. Video Media Rendering with Real-time Chunked Encryption & Decryption Progress Bar
        if (message.mediaType == "video") {
          val videoProgressFlow = remember(message.id, isMe) {
            ChunkedVideoEncryptor.getProgressFlow(message.id, isMe)
          }
          val videoProgressState by videoProgressFlow.collectAsState()
          val isProgressActive = videoProgressState.progressPercent in 1..99

          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(185.dp)
              .clickable {
                if (isProgressActive) {
                  android.widget.Toast.makeText(
                    context,
                    "Loading file... (${videoProgressState.progressPercent}%) ⏳",
                    android.widget.Toast.LENGTH_SHORT
                  ).show()
                } else {
                  onVideoClick(message)
                }
              }
              .testTag("chat_video_preview_${message.id}")
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              if (videoThumbBitmap != null) {
                Image(
                  bitmap = videoThumbBitmap.asImageBitmap(),
                  contentDescription = "Encrypted video preview",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Box(
                  modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                }
              }

              // Real-Time Progress Bar & Status Overlay
              if (isProgressActive) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                  contentAlignment = Alignment.Center
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                  ) {
                    CircularProgressIndicator(
                      progress = { videoProgressState.progressPercent / 100f },
                      modifier = Modifier.size(42.dp),
                      color = MaterialTheme.colorScheme.primary,
                      trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                      strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = if (isMe) "Encrypting & Sending (${videoProgressState.progressPercent}%)"
                             else "Receiving & Decrypting (${videoProgressState.progressPercent}%)",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                      progress = { videoProgressState.progressPercent / 100f },
                      modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                      color = MaterialTheme.colorScheme.primary,
                      trackColor = Color.White.copy(alpha = 0.2f)
                    )
                  }
                }
              } else {
                // Play Icon Overlay when ready / complete
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.PlayCircle, contentDescription = "Play Video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
              }

              // Duration Badge
              Box(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(6.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = if (isMe && videoProgressState.isComplete) "Sent ✓ $videoDuration" else videoDuration,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              // Dedicated Save to Phone Storage Button (Movies/Bluefy)
              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(6.dp)
                  .size(34.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                  .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                  .clickable {
                    val vidFile = File(PublicStorageManager.getTargetPublicDirectory(context, "mp4"), "VID_REC_${message.id}.mp4")
                    com.example.data.crypto.HighSpeedTransferManager.saveToPublicStorageAndScanAsync(
                      context = context,
                      sourceFile = if (vidFile.exists()) vidFile else null,
                      originalFileName = "Bluefy_Vid_${message.id.takeLast(6)}.mp4",
                      fileExtension = "mp4"
                    )
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Save to Phone Storage",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }

              // Encryption badge overlay
              Box(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .padding(6.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("E2EE 64KB CHUNKED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 2b. Universal Document & Any-File Rendering (Unlimited Size)
        if (message.mediaType == "file") {
          val context = androidx.compose.ui.platform.LocalContext.current
          val parts = remember(message.mediaContent) {
            if (message.mediaContent.startsWith("file_meta|")) message.mediaContent.split("|") else emptyList()
          }
          val fileName = if (parts.size >= 2) parts[1] else "document.bin"
          val ext = if (parts.size >= 3) parts[2] else fileName.substringAfterLast(".", "FILE").uppercase()
          val formattedSize = if (parts.size >= 5) parts[4] else "File"
          val localPath = if (parts.size >= 6) parts[5] else ""
          val mimeType = if (parts.size >= 7) parts[6] else "*/*"

          val fileProgressFlow = remember(message.id, isMe) {
            UniversalFileTransferManager.getProgressFlow(message.id, isMe, fileName)
          }
          val fileProgressState by fileProgressFlow.collectAsState()
          val isFileProgressActive = fileProgressState.progressPercent in 1..99

          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val fileToOpen = fileProgressState.localFile ?: if (localPath.isNotBlank()) java.io.File(localPath) else null
                val publicDir = com.example.data.crypto.PublicStorageManager.getTargetPublicDirectory(context, ext.lowercase())
                val candidatePublic = java.io.File(publicDir, fileName)
                val target = when {
                  candidatePublic.exists() && candidatePublic.length() > 0 -> candidatePublic
                  fileToOpen != null && fileToOpen.exists() && fileToOpen.length() > 0 -> fileToOpen
                  else -> candidatePublic
                }
                val progress = when {
                  isFileProgressActive -> fileProgressState.progressPercent
                  target.exists() && target.length() > 0 -> 100
                  else -> fileProgressState.progressPercent
                }
                com.example.data.crypto.FastFileOpener.handleFileTap(
                  context = context,
                  localFile = target,
                  progress = progress,
                  explicitMimeType = mimeType
                )
              }
              .testTag("chat_file_item_${message.id}")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                // File Type Badge Icon Box
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                      imageVector = Icons.Default.AttachFile,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                    Text(
                      text = ext.take(4),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "$ext • $formattedSize",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Dedicated Save to Phone Storage Button (Downloads/Bluefy)
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                    .clickable {
                      val fileToSave = fileProgressState.localFile ?: if (localPath.isNotBlank()) java.io.File(localPath) else null
                      com.example.data.crypto.HighSpeedTransferManager.saveToPublicStorageAndScanAsync(
                        context = context,
                        sourceFile = fileToSave,
                        originalFileName = fileName,
                        fileExtension = ext.lowercase()
                      )
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save to Downloads",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              // Real-Time Progress Bar for Document Streaming (Sender & Receiver)
              if (isFileProgressActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = fileProgressState.statusText,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                  progress = { fileProgressState.progressPercent / 100f },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                  color = MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 3. Audio Voice Note Rendering
        if (message.mediaType == "audio") {
          AudioVoiceNoteBubble(
            messageId = message.id,
            mediaContent = message.mediaContent,
            isFromMe = isMe
          )
        }

        // 4. Sticker Rendering
        if (message.mediaType == "sticker") {
          Text(text = message.mediaContent, fontSize = 36.sp)
        }

        // Message text
        if (message.text.isNotEmpty() && message.mediaType != "audio") {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom info row: Timestamp + Butterfly delivery indicator + Lock
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "AES-256",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${message.packetCount} PKT",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )

            if (isBurnTimerActive) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .background(Color(0xFF3E1205), RoundedCornerShape(4.dp))
                  .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  text = if (remainingSeconds > 0) "🔥 ${remainingSeconds}s" else "🔥 Burn",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFFFF7043)
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = timeFormat.format(Date(message.timestamp)),
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(5.dp))
            if (isMe && message.status == MessageDeliveryStatus.WAITING_IN_QUEUE) {
              Box(
                modifier = Modifier
                  .background(Color(0xFF352003), RoundedCornerShape(4.dp))
                  .border(0.8.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "⏳ Waiting Area",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFFFFCA28)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
            }
            // Butterfly delivery status indicator:
            // If the message is successfully sent/delivered to the other phone -> butterfly with wings
            // Otherwise (transmitting / sending) -> just butterfly without wings
            val isDelivered = !isMe || (message.status == MessageDeliveryStatus.DELIVERED || message.status == MessageDeliveryStatus.READ)
            ButterflyDeliveryIndicator(
              isDelivered = isDelivered,
              tint = if (isDelivered) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              }
            )
          }
        }
      }
    }

    // Fullscreen viewer hint for media only (technical MUX text removed)
    if (message.mediaType == "image" || message.mediaType == "video") {
      Text(
        text = "Tap to open in fullscreen viewer",
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 2.dp, end = 4.dp, start = 4.dp)
      )
    }
  }
}
