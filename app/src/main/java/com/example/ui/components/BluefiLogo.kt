package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BluefiDragonflyLogo(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  primaryColor: Color = MaterialTheme.colorScheme.primary,
  secondaryColor: Color = MaterialTheme.colorScheme.secondary,
  animated: Boolean = true
) {
  val infiniteTransition = rememberInfiniteTransition(label = "BluefiWaveTransition")
  val wavePulse by if (animated) {
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(1400, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "WavePulse"
    )
  } else {
    androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
  }

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height

      // Scale factors based on 100x100 reference canvas
      val sx = w / 100f
      val sy = h / 100f
      val strokeW = 3f * sx

      val centerX = 45f * sx
      val centerY = 50f * sy

      // Central Spine
      drawLine(
        color = primaryColor,
        start = Offset(centerX, 15f * sy),
        end = Offset(centerX, 85f * sy),
        strokeWidth = strokeW * 1.2f,
        cap = StrokeCap.Round
      )

      // Bluetooth Rune & Dragonfly wing mesh path
      val runePath = Path().apply {
        moveTo(32f * sx, 30f * sy)
        lineTo(60f * sx, 50f * sy)
        lineTo(centerX, 60f * sy)
        lineTo(centerX, 15f * sy)
        lineTo(60f * sx, 30f * sy)
        lineTo(32f * sx, 50f * sy)
        lineTo(centerX, 60f * sy)
        lineTo(centerX, 85f * sy)
        lineTo(60f * sx, 70f * sy)
        lineTo(32f * sx, 50f * sy)
      }
      drawPath(
        path = runePath,
        color = primaryColor,
        style = Stroke(
          width = strokeW,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // Dragonfly Wing Facet Lines (Top & Bottom Wing Tips)
      // Top Wings
      drawLine(
        color = secondaryColor,
        start = Offset(centerX, 28f * sy),
        end = Offset(18f * sx, 20f * sy),
        strokeWidth = strokeW * 0.9f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(18f * sx, 20f * sy),
        end = Offset(36f * sx, 42f * sy),
        strokeWidth = strokeW * 0.8f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(centerX, 28f * sy),
        end = Offset(72f * sx, 20f * sy),
        strokeWidth = strokeW * 0.9f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(72f * sx, 20f * sy),
        end = Offset(54f * sx, 42f * sy),
        strokeWidth = strokeW * 0.8f,
        cap = StrokeCap.Round
      )

      // Bottom Wings
      drawLine(
        color = secondaryColor,
        start = Offset(centerX, 72f * sy),
        end = Offset(18f * sx, 80f * sy),
        strokeWidth = strokeW * 0.9f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(18f * sx, 80f * sy),
        end = Offset(36f * sx, 58f * sy),
        strokeWidth = strokeW * 0.8f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(centerX, 72f * sy),
        end = Offset(72f * sx, 80f * sy),
        strokeWidth = strokeW * 0.9f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = secondaryColor,
        start = Offset(72f * sx, 80f * sy),
        end = Offset(54f * sx, 58f * sy),
        strokeWidth = strokeW * 0.8f,
        cap = StrokeCap.Round
      )

      // Cryptographic Lock / E2EE Core at Center Node
      drawCircle(
        color = primaryColor,
        radius = 4f * sx,
        center = Offset(centerX, centerY)
      )
      drawRect(
        color = primaryColor,
        topLeft = Offset(centerX - 3f * sx, centerY - 1f * sy),
        size = Size(6f * sx, 5f * sy)
      )

      // 3 Concentric Radio Broadcast Signal Waves (2.4 GHz Carrier Waves)
      // Wave 1
      drawArc(
        color = primaryColor.copy(alpha = 0.6f + 0.4f * wavePulse),
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(55f * sx, 35f * sy),
        size = Size(20f * sx, 30f * sy),
        style = Stroke(width = strokeW * 1.1f, cap = StrokeCap.Round)
      )

      // Wave 2
      drawArc(
        color = primaryColor.copy(alpha = 0.4f + 0.5f * wavePulse),
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(62f * sx, 28f * sy),
        size = Size(26f * sx, 44f * sy),
        style = Stroke(width = strokeW * 1.1f, cap = StrokeCap.Round)
      )

      // Wave 3
      drawArc(
        color = primaryColor.copy(alpha = 0.2f + 0.7f * wavePulse),
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(70f * sx, 20f * sy),
        size = Size(32f * sx, 60f * sy),
        style = Stroke(width = strokeW * 1.1f, cap = StrokeCap.Round)
      )
    }
  }
}
