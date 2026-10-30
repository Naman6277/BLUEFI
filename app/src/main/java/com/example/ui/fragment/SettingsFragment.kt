package com.example.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.example.R
import com.example.ui.activity.AboutActivity
import com.example.ui.theme.BluefiTheme

/**
 * SettingsFragment:
 * Fragment providing the Settings menu with section relabeling:
 * strictly displays "About" with summary_about string resource,
 * and launches AboutActivity detailing developer profile and architecture.
 */
class SettingsFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        BluefiTheme {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            SettingsFragmentContent(
              onOpenAbout = {
                val intent = Intent(requireContext(), AboutActivity::class.java)
                startActivity(intent)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun SettingsFragmentContent(
  onOpenAbout: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text(
        text = "SETTINGS",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
      )
    }

    // 1. Radio & Mesh Settings
    item {
      SettingsMenuRow(
        title = "Mesh Radio & Carrier",
        subtitle = "2.4 GHz dual-radio BLE & Wi-Fi Direct auto-failover",
        icon = Icons.Default.Sensors,
        onClick = {}
      )
    }

    // 2. Encryption & Key Exchange
    item {
      SettingsMenuRow(
        title = "Encryption & Keys",
        subtitle = "64KB AES-256-GCM chunked zero-cloud security",
        icon = Icons.Default.Lock,
        onClick = {}
      )
    }

    // 3. Storage & Downloads
    item {
      SettingsMenuRow(
        title = "Storage and Data",
        subtitle = "Public Downloads/Bluefy, Movies, Pictures saving",
        icon = Icons.Default.Storage,
        onClick = {}
      )
    }

    // 4. Notifications
    item {
      SettingsMenuRow(
        title = "Notifications",
        subtitle = "Message, peer connection, and chime tones",
        icon = Icons.Default.Notifications,
        onClick = {}
      )
    }

    // 5. Relabeled Section: "About" (formerly "Help and Feedback")
    item {
      SettingsMenuRow(
        title = stringResource(R.string.title_about),
        subtitle = stringResource(R.string.summary_about),
        icon = Icons.Default.Info,
        highlighted = true,
        testTag = "settings_menu_about",
        onClick = onOpenAbout
      )
    }
  }
}

@Composable
fun SettingsMenuRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  highlighted: Boolean = false,
  testTag: String = "",
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
      else MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
      else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(
            if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface,
            RoundedCornerShape(8.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Open",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
