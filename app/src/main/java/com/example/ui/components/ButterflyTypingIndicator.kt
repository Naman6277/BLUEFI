package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated fluttering butterfly icon designed specifically for typing and activity indicators.
 * Wings flap gracefully via an infinite sine/cubic transition while the butterfly gently bobs in place.
 */
@Composable
fun ButterflyFlutteringIcon(
  modifier: Modifier = Modifier,
  width: Dp = 20.dp,
  height: Dp = 15.dp,
  tint: Color = Color.Unspecified
) {
  val effectiveTint = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.primary
  val infiniteTransition = rememberInfiniteTransition(label = "butterfly_flutter_transition")

  // Wing flap animation: scales wing horizontal projection from 0.25f (wings folded up) to 1.0f (wings open)
  val wingScale by infiniteTransition.animateFloat(
    initialValue = 0.28f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "butterfly_wing_scale"
  )

  // Gentle vertical hovering motion
  val hoverOffset by infiniteTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 640, easing = LinearOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "butterfly_hover_bob"
  )

  Box(
    modifier = modifier
      .size(width = width, height = height)
      .offset(y = hoverOffset.dp)
      .semantics { contentDescription = "Butterfly fluttering typing indicator" }
      .testTag("butterfly_fluttering_icon"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(width = width, height = height)) {
      val w = size.width
      val h = size.height
      val centerX = w / 2f
      val centerY = h / 2f

      val wingAlpha = 0.55f + (0.45f * wingScale)
      val wingFillColor = effectiveTint.copy(alpha = 0.72f * wingAlpha)
      val wingStrokeColor = effectiveTint.copy(alpha = 0.95f * wingAlpha)
      val veinColor = effectiveTint.copy(alpha = 0.40f * wingAlpha)

      // 1. LEFT UPPER FOREWING
      val leftForewing = Path().apply {
        moveTo(centerX - 1.2f, centerY - (h * 0.10f))
        cubicTo(
          centerX - (w * 0.48f * wingScale), centerY - (h * 0.55f * (0.85f + 0.15f * wingScale)),
          centerX - (w * 0.52f * wingScale), centerY - (h * 0.05f),
          centerX - 1.2f, centerY + (h * 0.10f)
        )
        close()
      }
      drawPath(leftForewing, color = wingFillColor, style = Fill)
      drawPath(leftForewing, color = wingStrokeColor, style = Stroke(width = 1.2f))

      // 2. RIGHT UPPER FOREWING
      val rightForewing = Path().apply {
        moveTo(centerX + 1.2f, centerY - (h * 0.10f))
        cubicTo(
          centerX + (w * 0.48f * wingScale), centerY - (h * 0.55f * (0.85f + 0.15f * wingScale)),
          centerX + (w * 0.52f * wingScale), centerY - (h * 0.05f),
          centerX + 1.2f, centerY + (h * 0.10f)
        )
        close()
      }
      drawPath(rightForewing, color = wingFillColor, style = Fill)
      drawPath(rightForewing, color = wingStrokeColor, style = Stroke(width = 1.2f))

      // 3. LEFT LOWER HINDWING
      val leftHindwing = Path().apply {
        moveTo(centerX - 1.0f, centerY + (h * 0.10f))
        cubicTo(
          centerX - (w * 0.40f * wingScale), centerY + (h * 0.18f),
          centerX - (w * 0.35f * wingScale), centerY + (h * 0.48f * (0.85f + 0.15f * wingScale)),
          centerX - 1.0f, centerY + (h * 0.35f)
        )
        close()
      }
      drawPath(leftHindwing, color = wingFillColor.copy(alpha = 0.58f * wingAlpha), style = Fill)
      drawPath(leftHindwing, color = wingStrokeColor, style = Stroke(width = 1.0f))

      // 4. RIGHT LOWER HINDWING
      val rightHindwing = Path().apply {
        moveTo(centerX + 1.0f, centerY + (h * 0.10f))
        cubicTo(
          centerX + (w * 0.40f * wingScale), centerY + (h * 0.18f),
          centerX + (w * 0.35f * wingScale), centerY + (h * 0.48f * (0.85f + 0.15f * wingScale)),
          centerX + 1.0f, centerY + (h * 0.35f)
        )
        close()
      }
      drawPath(rightHindwing, color = wingFillColor.copy(alpha = 0.58f * wingAlpha), style = Fill)
      drawPath(rightHindwing, color = wingStrokeColor, style = Stroke(width = 1.0f))

      // 5. INNER WING VEINS
      if (wingScale > 0.45f) {
        val leftVein = Path().apply {
          moveTo(centerX - 1.2f, centerY)
          quadraticBezierTo(
            centerX - (w * 0.25f * wingScale), centerY - (h * 0.15f),
            centerX - (w * 0.36f * wingScale), centerY - (h * 0.24f)
          )
        }
        drawPath(leftVein, color = veinColor, style = Stroke(width = 0.8f))

        val rightVein = Path().apply {
          moveTo(centerX + 1.2f, centerY)
          quadraticBezierTo(
            centerX + (w * 0.25f * wingScale), centerY - (h * 0.15f),
            centerX + (w * 0.36f * wingScale), centerY - (h * 0.24f)
          )
        }
        drawPath(rightVein, color = veinColor, style = Stroke(width = 0.8f))
      }

      // 6. ANTENNAE
      val leftAntenna = Path().apply {
        moveTo(centerX - 0.7f, centerY - (h * 0.28f))
        quadraticBezierTo(
          centerX - (w * 0.14f), centerY - (h * 0.42f),
          centerX - (w * 0.22f), centerY - (h * 0.48f)
        )
      }
      drawPath(leftAntenna, color = effectiveTint, style = Stroke(width = 0.9f, cap = StrokeCap.Round))
      drawCircle(color = effectiveTint, radius = 0.8f, center = Offset(centerX - (w * 0.22f), centerY - (h * 0.48f)))

      val rightAntenna = Path().apply {
        moveTo(centerX + 0.7f, centerY - (h * 0.28f))
        quadraticBezierTo(
          centerX + (w * 0.14f), centerY - (h * 0.42f),
          centerX + (w * 0.22f), centerY - (h * 0.48f)
        )
      }
      drawPath(rightAntenna, color = effectiveTint, style = Stroke(width = 0.9f, cap = StrokeCap.Round))
      drawCircle(color = effectiveTint, radius = 0.8f, center = Offset(centerX + (w * 0.22f), centerY - (h * 0.48f)))

      // 7. HEAD
      drawCircle(color = effectiveTint, radius = 1.4f, center = Offset(centerX, centerY - (h * 0.26f)))

      // 8. SLENDER THORAX & ABDOMEN
      val bodyPath = Path().apply {
        moveTo(centerX - 1.0f, centerY - (h * 0.18f))
        cubicTo(
          centerX - 1.3f, centerY,
          centerX - 0.9f, centerY + (h * 0.32f),
          centerX, centerY + (h * 0.44f)
        )
        cubicTo(
          centerX + 0.9f, centerY + (h * 0.32f),
          centerX + 1.3f, centerY,
          centerX + 1.0f, centerY - (h * 0.18f)
        )
        close()
      }
      drawPath(bodyPath, color = effectiveTint, style = Fill)
    }
  }
}

/**
 * 3 Staggered pulsing dots for typing indication.
 */
@Composable
fun PulsingTypingDots(
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  dotSize: Dp = 4.dp
) {
  val effectiveColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
  val transition = rememberInfiniteTransition(label = "dots_pulsing_transition")

  val dot1Alpha by transition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, delayMillis = 0, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot1"
  )

  val dot2Alpha by transition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, delayMillis = 200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot2"
  )

  val dot3Alpha by transition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, delayMillis = 400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot3"
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(dotSize)
        .background(effectiveColor.copy(alpha = dot1Alpha), CircleShape)
    )
    Box(
      modifier = Modifier
        .size(dotSize)
        .background(effectiveColor.copy(alpha = dot2Alpha), CircleShape)
    )
    Box(
      modifier = Modifier
        .size(dotSize)
        .background(effectiveColor.copy(alpha = dot3Alpha), CircleShape)
    )
  }
}

/**
 * Subtle 'typing...' banner with fluttering butterfly icon, positioned directly above the text input field.
 */
@Composable
fun PeerTypingIndicator(
  peerName: String,
  modifier: Modifier = Modifier,
  tint: Color = Color.Unspecified
) {
  val effectiveTint = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.primary

  Box(
    modifier = modifier
      .padding(horizontal = 14.dp, vertical = 4.dp)
      .testTag("peer_typing_indicator")
  ) {
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
        .border(
          width = 0.8.dp,
          color = effectiveTint.copy(alpha = 0.35f),
          shape = RoundedCornerShape(16.dp)
        )
        .padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Subtle Fluttering Butterfly Icon
      ButterflyFlutteringIcon(
        width = 18.dp,
        height = 14.dp,
        tint = effectiveTint
      )

      Spacer(modifier = Modifier.width(8.dp))

      // Typing text
      val displayText = if (peerName.isNotBlank()) "$peerName is typing" else "typing..."
      Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.width(6.dp))

      // 3 Pulsing Dots
      PulsingTypingDots(
        color = effectiveTint,
        dotSize = 3.5.dp
      )
    }
  }
}
