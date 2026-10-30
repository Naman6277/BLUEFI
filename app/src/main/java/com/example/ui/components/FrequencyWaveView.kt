package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun FrequencyWaveCard(
  modifier: Modifier = Modifier,
  carrierFreq: String = "2.4 GHz Frequency Wave",
  rangeKm: Double = 2.5,
  baseRequirement: String = "0.4 GHz",
  packetsRelayed: Long = 18420L
) {
  val transition = rememberInfiniteTransition(label = "RfWaveAnimation")
  val wavePhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "WavePhase"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
      )
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp)
      )
      .padding(14.dp)
      .testTag("frequency_wave_card")
  ) {
    // Header Row with 2.4 GHz title and range
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Wifi,
          contentDescription = "Carrier Frequency",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = carrierFreq,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = "Range: Up to $rangeKm km",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Waveform Canvas (representing the 2.4 GHz sine carrier wave from user brief)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(
          MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
          shape = RoundedCornerShape(8.dp)
        )
        .padding(horizontal = 8.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val amplitude = height * 0.38f
        val cycles = 7.5f

        val path = Path()
        val step = 4f
        var x = 0f
        var isFirst = true

        while (x <= width) {
          val normalized = (x / width) * (cycles * 2 * Math.PI.toFloat()) + wavePhase
          val y = midY + sin(normalized) * amplitude
          if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
          } else {
            path.lineTo(x, y)
          }
          x += step
        }

        // Draw background grid lines
        drawLine(
          color = primaryColor.copy(alpha = 0.15f),
          start = Offset(0f, midY),
          end = Offset(width, midY),
          strokeWidth = 1f
        )

        // Draw sine wave
        drawPath(
          path = path,
          color = primaryColor,
          style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Bottom Spec Badges (System Requirements & Security tags from brief)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Base Requirement: $baseRequirement",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "E2EE",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "FIREWALL • ENCRYPTED",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
