package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.crypto.FullPipelineTrace
import com.example.data.model.ChatMessage
import com.example.ui.theme.PacketDemuxColor
import com.example.ui.theme.PacketMuxColor

@Composable
fun PacketInspectorDialog(
  message: ChatMessage,
  trace: FullPipelineTrace?,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("packet_inspector_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  MaterialTheme.colorScheme.primaryContainer,
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "E2EE Packet Inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "MUX/DEMUX 2.4 GHz Pipeline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_inspector_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message Preview summary
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "MESSAGE CONTENT",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "\"${message.text}\"",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Packets: ${message.packetCount} frame(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = "Status: ${message.status.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Diagram Stages
        if (trace != null) {
          trace.stages.forEachIndexed { index, stage ->
            PipelineStageCard(
              stageNumber = stage.stepNumber,
              title = stage.stepTitle,
              summary = stage.outputSummary,
              details = stage.technicalDetails
            )
            if (index < trace.stages.size - 1) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowDownward,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        } else {
          // Fallback if trace not stored
          PipelineStageCard(
            stageNumber = 2,
            title = "Binary Form Conversion",
            summary = message.binaryForm.take(48) + "...",
            details = "UTF-8 text serialized into binary bits."
          )
          Spacer(modifier = Modifier.height(8.dp))
          val fingerprint = com.example.data.crypto.Aes256EncryptionHelper.getSessionKeyFingerprint(message.conversationPeerId)
          val keyInfo = if (fingerprint != null) " (Key Fingerprint: $fingerprint)" else ""
          PipelineStageCard(
            stageNumber = 3,
            title = "AES-256-GCM Ciphertext",
            summary = message.cipherTextHex.take(32) + "...",
            details = "Encrypted with 256-bit AES session key derived during initial handshake$keyInfo and 12-byte IV."
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dismiss_inspector_button")
        ) {
          Text("Done Inspecting", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun PipelineStageCard(
  stageNumber: Int,
  title: String,
  summary: String,
  details: String
) {
  val accentColor = when (stageNumber) {
    3 -> Color(0xFFFFD700) // Encryption
    5 -> PacketMuxColor // MUX
    6 -> PacketDemuxColor // DEMUX
    8 -> MaterialTheme.colorScheme.primary // Decrypted
    else -> MaterialTheme.colorScheme.primary
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp)
      )
      .border(
        width = 1.dp,
        color = accentColor.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp)
      )
      .padding(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(22.dp)
          .background(accentColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stageNumber.toString(),
          color = Color.Black,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.sp
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = summary,
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp,
      color = accentColor,
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(4.dp)
        )
        .padding(horizontal = 6.dp, vertical = 2.dp)
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = details,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp
    )
  }
}
