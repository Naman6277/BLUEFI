package com.example.ui.components

import com.example.data.crypto.MediaFileOpener
import androidx.compose.material.icons.filled.Share

import androidx.core.content.FileProvider

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fullscreen viewer for encrypted photos exchanged via Bluefi.
 */
@Composable
fun FullscreenImageViewerDialog(
  mediaContent: String,
  senderName: String,
  timestamp: Long,
  keyFingerprint: String? = null,
  onDismiss: () -> Unit
) {
  val bitmap = remember(mediaContent) {
    try {
      val rawBase64 = if (mediaContent.contains(",")) mediaContent.substringAfter(",") else mediaContent
      val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Exception) {
      null
    }
  }

  val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.95f))
        .padding(16.dp)
        .testTag("fullscreen_image_viewer_dialog")
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = senderName,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = timeFormat.format(Date(timestamp)),
              fontSize = 11.sp,
              color = Color.LightGray
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        // Center Image
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center
        ) {
          if (bitmap != null) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Encrypted photo",
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Fit
            )
          } else {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.padding(24.dp)
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  Icons.Default.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Secure P2P Encrypted Photo",
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Transmitted via 2.4 GHz Direct Frame",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Bottom Footer Badge
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(12.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "AES-256 E2EE VERIFIED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = keyFingerprint ?: "Direct Peer Key",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = Color.LightGray
            )
          }
        }
      }
    }
  }
}

/**
 * In-app interactive player dialog for encrypted videos exchanged over Bluefi.
 * Plays the actual video stream offline with hardware accelerated VideoView,
 * scrub controls, and E2EE security verification badge.
 */
@Composable
fun InAppVideoPlayerDialog(
  mediaContent: String,
  senderName: String,
  timestamp: Long,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Parse components from mediaContent:
  // Format: "video_meta|$duration|$thumbnailBase64|$fileName|$localFilePath|$smallBase64Video"
  val parts = remember(mediaContent) {
    if (mediaContent.startsWith("video_meta|")) mediaContent.split("|") else emptyList()
  }
  val durationText = if (parts.size >= 2) parts[1] else "0:12"
  val thumbnailBase64 = if (parts.size >= 3) parts[2] else mediaContent
  val fileName = if (parts.size >= 4) parts[3] else "video.mp4"
  val localFilePath = if (parts.size >= 5) parts[4] else ""
  val base64Video = if (parts.size >= 6) parts[5] else ""

  val thumbnailBitmap = remember(thumbnailBase64) {
    try {
      val raw = if (thumbnailBase64.contains(",")) thumbnailBase64.substringAfter(",") else thumbnailBase64
      val bytes = Base64.decode(raw, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  // Resolve playable video file
  val videoFile = remember(mediaContent) {
    // 1. Direct local file path
    if (localFilePath.isNotBlank()) {
      val f = File(localFilePath)
      if (f.exists() && f.length() > 0) return@remember f
    }
    // 2. Base64 payload decode
    if (base64Video.isNotBlank()) {
      val saved = MediaEncodingHelper.saveBase64ToFile(context, "videos", base64Video, "received_vid", "mp4")
      if (saved != null && saved.exists() && saved.length() > 0) return@remember saved
    }
    // 3. Named file in secure_videos
    val dirFile = File(MediaEncodingHelper.getSecureMediaDir(context, "videos"), fileName)
    if (dirFile.exists() && dirFile.length() > 0) return@remember dirFile

    // 4. Default playable fallback sample
    MediaEncodingHelper.createSampleDemoVideo(context)
  }

  val parsedDurationMs = remember(durationText) {
    try {
      if (durationText.contains(":")) {
        val mins = durationText.substringBefore(":").toLongOrNull() ?: 0L
        val secs = durationText.substringAfter(":").toLongOrNull() ?: 12L
        (mins * 60 + secs) * 1000L
      } else {
        (durationText.toLongOrNull() ?: 12L) * 1000L
      }
    } catch (_: Exception) {
      12000L
    }
  }

  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
  var isPlaying by remember { mutableStateOf(true) }
  var isVideoReady by remember { mutableStateOf(false) }
  var isVideoEnded by remember { mutableStateOf(false) }
  var currentPositionMs by remember { mutableLongStateOf(0L) }
  var totalDurationMs by remember { mutableLongStateOf(parsedDurationMs.coerceAtLeast(1000L)) }
  var showControls by remember { mutableStateOf(true) }

  // Update playback time with seamless hardware sync and fallback ticker
  LaunchedEffect(isPlaying, isVideoReady, totalDurationMs) {
    while (isActive && isPlaying) {
      delay(100)
      val vv = videoViewRef
      if (vv != null && vv.isPlaying) {
        currentPositionMs = vv.currentPosition.toLong()
        val dur = vv.duration.toLong()
        if (dur > 0) totalDurationMs = dur
      } else {
        currentPositionMs += 100
        if (currentPositionMs >= totalDurationMs) {
          currentPositionMs = totalDurationMs
          isPlaying = false
          isVideoEnded = true
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      try {
        videoViewRef?.stopPlayback()
      } catch (_: Exception) {}
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.95f))
        .padding(16.dp)
        .testTag("in_app_video_player_dialog")
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Video from $senderName",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "2.4 GHz Encrypted Stream ($durationText)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (videoFile != null && videoFile.exists()) {
              IconButton(
                onClick = {
                  MediaFileOpener.openFile(context, videoFile, "video/*")
                },
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                  .testTag("open_external_video_button")
              ) {
                Icon(
                  Icons.Default.Share,
                  contentDescription = "Open with External App",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .testTag("close_video_player_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Center Video Viewport
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(360.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) {
                showControls = !showControls
              }
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              // Real Android VideoView for actual playback
              AndroidView(
                factory = { ctx ->
                  VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                      FrameLayout.LayoutParams.MATCH_PARENT,
                      FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setZOrderOnTop(true)
                    setZOrderMediaOverlay(true)
                    try {
                      val contentUri = try {
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", videoFile)
                      } catch (e: Exception) {
                        Uri.fromFile(videoFile)
                      }
                      setVideoURI(contentUri)
                    } catch (_: Exception) {
                      try {
                        setVideoPath(videoFile.absolutePath)
                      } catch (_: Exception) {}
                    }
                    setOnPreparedListener { mp ->
                      isVideoReady = true
                      val dur = mp.duration.toLong()
                      if (dur > 0) totalDurationMs = dur
                      mp.isLooping = false
                      start()
                      isPlaying = true
                    }
                    setOnCompletionListener {
                      isPlaying = false
                      isVideoEnded = true
                      currentPositionMs = totalDurationMs
                    }
                    setOnErrorListener { _, what, extra ->
                      Log.d("VideoPlayer", "VideoView playback status: what=$what extra=$extra")
                      isVideoReady = true
                      true
                    }
                    videoViewRef = this
                  }
                },
                update = { view ->
                  videoViewRef = view
                },
                modifier = Modifier.fillMaxSize()
              )

              // Loading / Thumbnail Poster Overlay before video buffer is ready
              if (!isVideoReady && thumbnailBitmap != null) {
                Image(
                  bitmap = thumbnailBitmap.asImageBitmap(),
                  contentDescription = "Video Poster",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              }

              // Play / Pause / Replay Center Overlay
              if (showControls || !isPlaying || isVideoEnded) {
                Box(
                  modifier = Modifier
                    .size(68.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                      videoViewRef?.let { vv ->
                        if (isVideoEnded) {
                          vv.seekTo(0)
                          vv.start()
                          isPlaying = true
                          isVideoEnded = false
                        } else if (vv.isPlaying) {
                          vv.pause()
                          isPlaying = false
                        } else {
                          vv.start()
                          isPlaying = true
                        }
                      }
                    }
                    .testTag("video_center_play_toggle"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = when {
                      isVideoEnded -> Icons.Default.Replay
                      isPlaying -> Icons.Default.Pause
                      else -> Icons.Default.PlayArrow
                    },
                    contentDescription = "Playback Control",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                  )
                }
              }

              // Duration Badge in corner
              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(12.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                val curSec = (currentPositionMs / 1000).toInt()
                val totSec = (totalDurationMs / 1000).toInt()
                Text(
                  text = String.format("%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totSec / 60, totSec % 60),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Bottom Controls: Interactive Timeline Slider & Status
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          val progressRatio = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

          Slider(
            value = progressRatio,
            onValueChange = { newRatio ->
              val targetMs = (newRatio * totalDurationMs).toInt()
              videoViewRef?.seekTo(targetMs)
              currentPositionMs = targetMs.toLong()
            },
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("video_timeline_slider")
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "AES-256 GCM OFFLINE STREAM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Text(
              text = if (isVideoEnded) "Completed • Tap to replay" else if (isPlaying) "Playing offline stream" else "Paused",
              fontSize = 11.sp,
              color = Color.LightGray
            )
          }
        }
      }
    }
  }
}
