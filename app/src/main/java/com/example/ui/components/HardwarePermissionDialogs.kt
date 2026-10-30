package com.example.ui.components

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Friendly dialog explaining why hardware & runtime permissions are required for offline P2P chat,
 * with options to grant permissions or open system App Settings.
 */
@Composable
fun HardwarePermissionsDialog(
  onGrantPermissions: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        .testTag("hardware_permissions_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon header
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "HARDWARE PERMISSIONS REQUIRED",
          fontSize = 15.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Bluefi operates 100% offline without cellular data or the Internet. To discover nearby devices and establish socket connections, Android requires the following hardware access:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permission items
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PermissionExplanationRow(
            icon = Icons.Default.Bluetooth,
            title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Nearby Bluetooth (Scan & Connect)" else "Bluetooth & Location Access",
            description = "Discovers nearby nodes and pairs RFCOMM duplex sockets."
          )

          PermissionExplanationRow(
            icon = Icons.Default.Wifi,
            title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Nearby Wi-Fi Devices" else "Wi-Fi Direct Peer Discovery",
            description = "Enables high-bandwidth local P2P channels for zero-data media."
          )

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionExplanationRow(
              icon = Icons.Default.Notifications,
              title = "Mesh Notifications",
              description = "Alerts you when an offline mesh packet or chat is received."
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: Grant Permissions
        Button(
          onClick = onGrantPermissions,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("grant_permissions_button")
        ) {
          Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "GRANT PERMISSIONS",
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary Action: Open App Settings
        OutlinedButton(
          onClick = onOpenSettings,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("open_app_settings_button")
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "OPEN APP SETTINGS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("dismiss_permissions_dialog_button")
        ) {
          Text(
            text = "CONTINUE ANYWAY",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun PermissionExplanationRow(
  icon: ImageVector,
  title: String,
  description: String
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ),
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = description,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 13.sp
        )
      }
    }
  }
}

/**
 * Dialog prompting the user to request exemption from battery limits (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).
 * Critical for keeping Bluetooth and Wi-Fi Direct socket connections alive when screen turns off.
 */
@Composable
fun BatteryOptimizationDialog(
  onRequestExemption: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        .testTag("battery_optimization_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.BatteryAlert,
            contentDescription = null,
            tint = Color(0xFFFFB300),
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "BACKGROUND SOCKET EXEMPTION",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFFFFB300),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "To keep Bluetooth & Wi-Fi Direct sockets connected when your screen turns off, Bluefi needs exemption from aggressive OEM battery restrictions (Samsung, Xiaomi, Oppo, Transsion).",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF261D07)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Without exemption, the Android OS may sleep sockets after 30 seconds of screen lock.",
              fontSize = 10.sp,
              color = Color(0xFFFFE082),
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            onRequestExemption()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFB300),
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("request_battery_exemption_button")
        ) {
          Text(
            text = "ALLOW UNRESTRICTED BACKGROUND",
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("dismiss_battery_dialog_button")
        ) {
          Text(
            text = "LATER",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
