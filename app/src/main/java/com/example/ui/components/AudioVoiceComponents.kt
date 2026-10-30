package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Chat bubble displaying an encrypted voice note with interactive play/pause and dynamic waveform.
 */
@Composable
fun AudioVoiceNoteBubble(
  messageId: String,
  mediaContent: String,
  isFromMe: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val activeId by AudioVoicePlayer.activeMessageId.collectAsState()
  val isPlaying by AudioVoicePlayer.isPlaying.collectAsState()
  val playbackProgress by AudioVoicePlayer.playbackProgress.collectAsState()
  val currentPosSec by AudioVoicePlayer.currentPositionSec.collectAsState()

  val isThisPlaying = (activeId == messageId && isPlaying)
  val isThisActive = (activeId == messageId)
  val progress = if (isThisActive) playbackProgress else 0f

  // Parse duration and base64 from mediaContent ("audio_meta|durationSec|base64|filePath")
  val parts = remember(mediaContent) {
    if (mediaContent.startsWith("audio_meta|")) mediaContent.split("|") else emptyList()
  }
  val totalDurationSec = remember(parts) {
    if (parts.size >= 2) parts[1].toIntOrNull() ?: 5 else 5
  }

  // Pre-generate deterministic pseudo-waveform heights based on message ID hash
  val waveHeights = remember(messageId) {
    val rng = Random(messageId.hashCode())
    List(24) { (0.25f + rng.nextFloat() * 0.75f).coerceIn(0.2f, 1f) }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .testTag("audio_voice_bubble_$messageId")
  ) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Play / Pause Circle Button
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(
            if (isThisPlaying) primaryColor else primaryColor.copy(alpha = 0.15f),
            CircleShape
          )
          .border(
            1.5.dp,
            if (isThisPlaying) primaryColor else primaryColor.copy(alpha = 0.6f),
            CircleShape
          )
          .clip(CircleShape)
          .clickable {
            AudioVoicePlayer.playVoiceNote(context, scope, messageId, mediaContent)
          }
          .testTag("voice_play_pause_button_$messageId"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when {
            isThisActive && progress >= 1f -> Icons.Default.Replay
            isThisPlaying -> Icons.Default.Pause
            else -> Icons.Default.PlayArrow
          },
          contentDescription = if (isThisPlaying) "Pause" else "Play",
          tint = if (isThisPlaying) onPrimaryColor else primaryColor,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Waveform and Duration Column
      Column(modifier = Modifier.weight(1f)) {
        // Waveform Canvas with interactive scrub
        val barColorInactive = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        val barColorActive = primaryColor

        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              if (!isThisActive) {
                AudioVoicePlayer.playVoiceNote(context, scope, messageId, mediaContent)
              }
            }
        ) {
          val totalBars = waveHeights.size
          val barWidth = 3.dp.toPx()
          val spacing = ((size.width - (totalBars * barWidth)) / (totalBars - 1)).coerceAtLeast(1.5f)

          for (i in 0 until totalBars) {
            val barHeight = size.height * waveHeights[i]
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2f

            val isBarActive = (i.toFloat() / totalBars.toFloat()) <= progress
            val color = if (isBarActive) barColorActive else barColorInactive

            drawRoundRect(
              color = color,
              topLeft = Offset(x, y),
              size = Size(barWidth, barHeight),
              cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Time and E2EE verification row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val currentDisplay = if (isThisActive) {
            String.format("%02d:%02d", currentPosSec / 60, currentPosSec % 60)
          } else "00:00"
          val totalDisplay = String.format("%02d:%02d", totalDurationSec / 60, totalDurationSec % 60)

          Text(
            text = "$currentDisplay / $totalDisplay",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = if (isThisPlaying) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = primaryColor,
              modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "AES-256 AUDIO",
              fontSize = 8.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = primaryColor
            )
          }
        }
      }
    }
  }
}

/**
 * Bar shown above/within the chat input bar while user is recording an audio voice note.
 */
@Composable
fun VoiceRecordingBar(
  durationSeconds: Int,
  amplitude: Float,
  onCancel: () -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
      .border(1.5.dp, primaryColor.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag("voice_recording_bar"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left: Pulsing Red/Recording dot + timer
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(12.dp)
          .background(MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha), CircleShape)
      )
      Spacer(modifier = Modifier.width(8.dp))
      val timeStr = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
      Text(
        text = timeStr,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    // Center: Live Audio Waveform Bars that react to amplitude
    Row(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val barCount = 14
      for (i in 0 until barCount) {
        val waveFactor = kotlin.math.sin((i.toDouble() / barCount) * Math.PI).toFloat()
        val barHeight = (8.dp + ((24.dp * amplitude) * waveFactor)).coerceIn(6.dp, 28.dp)
        Box(
          modifier = Modifier
            .width(3.dp)
            .height(barHeight)
            .padding(horizontal = 0.5.dp)
            .background(primaryColor.copy(alpha = 0.85f), RoundedCornerShape(1.5.dp))
        )
        Spacer(modifier = Modifier.width(3.dp))
      }
    }

    // Right: Cancel and Send Actions
    Row(verticalAlignment = Alignment.CenterVertically) {
      // Discard Button
      IconButton(
        onClick = onCancel,
        modifier = Modifier
          .size(36.dp)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
          .testTag("cancel_voice_record_button")
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Cancel Recording",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Send Voice Note Button
      IconButton(
        onClick = onSend,
        modifier = Modifier
          .size(38.dp)
          .background(primaryColor, CircleShape)
          .testTag("send_voice_record_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send Voice Note",
          tint = onPrimaryColor,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
