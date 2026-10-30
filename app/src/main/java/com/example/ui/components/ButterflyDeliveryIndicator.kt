package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual message delivery status indicator.
 * Displays a delicate butterfly:
 * - When delivered to peer device: Butterfly with open, fluttering wings.
 * - While sending/transmitting: Slender butterfly body without wings (larva/chrysalis axis).
 */
@Composable
fun ButterflyDeliveryIndicator(
  isDelivered: Boolean,
  modifier: Modifier = Modifier,
  width: Dp = 17.dp,
  height: Dp = 13.dp,
  tint: Color = if (isDelivered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
) {
  val wingExpansion by animateFloatAsState(
    targetValue = if (isDelivered) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "butterfly_wings_expansion"
  )

  Box(
    modifier = modifier
      .size(width = width, height = height)
      .semantics {
        contentDescription = if (isDelivered) "Message delivered (Butterfly with wings)" else "Sending message (Butterfly without wings)"
      }
      .testTag(if (isDelivered) "butterfly_status_winged" else "butterfly_status_wingless"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(width = width, height = height)) {
      val w = size.width
      val h = size.height
      val centerX = w / 2f
      val centerY = h / 2f

      // 1. WINGS (Expanded only when delivered)
      if (wingExpansion > 0.02f) {
        val wingAlpha = wingExpansion.coerceIn(0f, 1f)
        val wingFillColor = tint.copy(alpha = 0.78f * wingAlpha)
        val wingStrokeColor = tint.copy(alpha = 0.95f * wingAlpha)
        val veinColor = tint.copy(alpha = 0.45f * wingAlpha)

        // Left upper forewing
        val leftForewing = Path().apply {
          moveTo(centerX - 1.2f, centerY - (h * 0.10f))
          cubicTo(
            centerX - (w * 0.48f * wingExpansion), centerY - (h * 0.55f * wingExpansion),
            centerX - (w * 0.52f * wingExpansion), centerY - (h * 0.05f * wingExpansion),
            centerX - 1.2f, centerY + (h * 0.10f)
          )
          close()
        }
        drawPath(leftForewing, color = wingFillColor, style = Fill)
        drawPath(leftForewing, color = wingStrokeColor, style = Stroke(width = 1.1f))

        // Right upper forewing
        val rightForewing = Path().apply {
          moveTo(centerX + 1.2f, centerY - (h * 0.10f))
          cubicTo(
            centerX + (w * 0.48f * wingExpansion), centerY - (h * 0.55f * wingExpansion),
            centerX + (w * 0.52f * wingExpansion), centerY - (h * 0.05f * wingExpansion),
            centerX + 1.2f, centerY + (h * 0.10f)
          )
          close()
        }
        drawPath(rightForewing, color = wingFillColor, style = Fill)
        drawPath(rightForewing, color = wingStrokeColor, style = Stroke(width = 1.1f))

        // Left lower hindwing
        val leftHindwing = Path().apply {
          moveTo(centerX - 1.0f, centerY + (h * 0.10f))
          cubicTo(
            centerX - (w * 0.42f * wingExpansion), centerY + (h * 0.18f * wingExpansion),
            centerX - (w * 0.35f * wingExpansion), centerY + (h * 0.48f * wingExpansion),
            centerX - 1.0f, centerY + (h * 0.35f)
          )
          close()
        }
        drawPath(leftHindwing, color = wingFillColor.copy(alpha = 0.65f * wingAlpha), style = Fill)
        drawPath(leftHindwing, color = wingStrokeColor, style = Stroke(width = 1.0f))

        // Right lower hindwing
        val rightHindwing = Path().apply {
          moveTo(centerX + 1.0f, centerY + (h * 0.10f))
          cubicTo(
            centerX + (w * 0.42f * wingExpansion), centerY + (h * 0.18f * wingExpansion),
            centerX + (w * 0.35f * wingExpansion), centerY + (h * 0.48f * wingExpansion),
            centerX + 1.0f, centerY + (h * 0.35f)
          )
          close()
        }
        drawPath(rightHindwing, color = wingFillColor.copy(alpha = 0.65f * wingAlpha), style = Fill)
        drawPath(rightHindwing, color = wingStrokeColor, style = Stroke(width = 1.0f))

        // Delicate inner wing decorative veins
        val leftVein = Path().apply {
          moveTo(centerX - 1.2f, centerY)
          quadraticBezierTo(
            centerX - (w * 0.26f * wingExpansion), centerY - (h * 0.14f * wingExpansion),
            centerX - (w * 0.38f * wingExpansion), centerY - (h * 0.22f * wingExpansion)
          )
        }
        drawPath(leftVein, color = veinColor, style = Stroke(width = 0.75f))

        val rightVein = Path().apply {
          moveTo(centerX + 1.2f, centerY)
          quadraticBezierTo(
            centerX + (w * 0.26f * wingExpansion), centerY - (h * 0.14f * wingExpansion),
            centerX + (w * 0.38f * wingExpansion), centerY - (h * 0.22f * wingExpansion)
          )
        }
        drawPath(rightVein, color = veinColor, style = Stroke(width = 0.75f))
      }

      // 2. BUTTERFLY BODY (Head + Antennae + Thorax/Abdomen)
      // Antennae with delicate curved feelers and round tips
      val leftAntenna = Path().apply {
        moveTo(centerX - 0.7f, centerY - (h * 0.28f))
        quadraticBezierTo(
          centerX - (w * 0.14f), centerY - (h * 0.42f),
          centerX - (w * 0.22f), centerY - (h * 0.46f)
        )
      }
      drawPath(leftAntenna, color = tint, style = Stroke(width = 0.9f, cap = StrokeCap.Round))
      drawCircle(color = tint, radius = 0.8f, center = Offset(centerX - (w * 0.22f), centerY - (h * 0.46f)))

      val rightAntenna = Path().apply {
        moveTo(centerX + 0.7f, centerY - (h * 0.28f))
        quadraticBezierTo(
          centerX + (w * 0.14f), centerY - (h * 0.42f),
          centerX + (w * 0.22f), centerY - (h * 0.46f)
        )
      }
      drawPath(rightAntenna, color = tint, style = Stroke(width = 0.9f, cap = StrokeCap.Round))
      drawCircle(color = tint, radius = 0.8f, center = Offset(centerX + (w * 0.22f), centerY - (h * 0.46f)))

      // Head
      drawCircle(color = tint, radius = 1.4f, center = Offset(centerX, centerY - (h * 0.26f)))

      // Thorax & Slender Abdomen
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
      drawPath(bodyPath, color = tint, style = Fill)
    }
  }
}
