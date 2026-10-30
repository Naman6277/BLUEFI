package com.example.ui.screens

import android.content.Context
import com.example.util.ApkShareManager
import com.example.util.DynamicFontManager
import com.example.util.DynamicThemeManager
import com.example.ui.theme.BluefiFontStyle
import java.security.MessageDigest
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLockType
import com.example.data.model.RetentionRule
import com.example.data.model.UserSettings
import com.example.ui.components.DeveloperArchitectureDialog
import com.example.ui.theme.BluefiThemeOption

/**
 * Settings Screen matching the Prototype Screenshot Design:
 * Header: [← Username / e.g. BaBa_YaGA] with Search, QR Code Share, and Edit Profile (pencil)
 * 13 Functional Categories:
 * 1. Account — Security notifications, change number / node info
 * 2. Privacy — Blocked accounts, disappearing messages, stealth mode
 * 3. Lists — Manage people and groups
 * 4. Chats — Chat history, backup, auto-delete rules
 * 5. Appearance — Chat theme, app icon, app theme (Dark Navy Slate)
 * 6. Broadcasts — Manage lists and send broadcasts
 * 7. Notifications — Message, group & call tones
 * 8. Storage and data — Network usage, auto-download
 * 9. Parental controls — Settings for your family
 * 10. Accessibility — Increase contrast, animation
 * 11. App language — English (device's language)
 * 12. Help and feedback — Help Center, contact us, privacy policy
 * 13. Invite a friend — Share offline APK / P2P invite link
 */
@Composable
fun SettingsScreen(
  settings: UserSettings,
  onUpdateProfile: (String, Int) -> Unit,
  onSelectTheme: (BluefiThemeOption) -> Unit,
  onSelectRetentionRule: (RetentionRule) -> Unit,
  onToggleBluetooth: (Boolean) -> Unit,
  isAdvertising: Boolean = true,
  onToggleAdvertising: (Boolean) -> Unit = {},
  onToggleWifiDirect: (Boolean) -> Unit,
  onToggleRadarVisibility: (Boolean) -> Unit = {},
  onLogout: () -> Unit,
  onOpenPairing: () -> Unit = {},
  onOpenPipeline: () -> Unit = {},
  onBack: () -> Unit = {},
  onGetSecurityQuestions: suspend (String) -> Triple<String, String, String>? = { null },
  onVerifySecurityAnswers: suspend (String, String, String, String) -> Boolean = { _, _, _, _ -> false },
  onUpdateLockFormatAndSecret: (userId: String, lockType: String, passcode: String) -> Unit = { _, _, _ -> },
  onToggleBatterySaver: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showUsernameDialog by remember { mutableStateOf(false) }
  var showDeveloperDialog by remember { mutableStateOf(false) }
  var showQrDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchActive by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  // Dynamic App Lock Configuration state
  val lockPrefs = remember { context.getSharedPreferences("bluefi_lock_prefs", Context.MODE_PRIVATE) }

  // Security Questions Verification & Change Lock Dialog States
  var showSecurityVerificationDialog by remember { mutableStateOf(false) }
  var securityQ1 by remember { mutableStateOf("") }
  var securityQ2 by remember { mutableStateOf("") }
  var securityQ3 by remember { mutableStateOf("") }
  var securityA1 by remember { mutableStateOf("") }
  var securityA2 by remember { mutableStateOf("") }
  var securityA3 by remember { mutableStateOf("") }
  var securityVerificationError by remember { mutableStateOf<String?>(null) }
  var isVerifyingQuestions by remember { mutableStateOf(false) }
  var currentLockType by remember {
    mutableStateOf(
      AppLockType.valueOf(lockPrefs.getString("SELECTED_LOCK_TYPE", lockPrefs.getString("app_lock_type", AppLockType.PIN_4.name)) ?: AppLockType.PIN_4.name)
    )
  }
  var showLockConfigDialog by remember { mutableStateOf(false) }
  var newLockSecretInput by remember { mutableStateOf("") }
  var lockConfigMessage by remember { mutableStateOf<String?>(null) }

  // Privacy: Read receipts & Blocked contacts state
  var readReceiptsEnabled by remember { mutableStateOf(lockPrefs.getBoolean("read_receipts_enabled", true)) }
  var instantWipeOnLeaveEnabled by remember { mutableStateOf(lockPrefs.getBoolean("instant_wipe_on_leave", false)) }
  var selectedFontStyle by remember { mutableStateOf(lockPrefs.getString("app_font_style", "INTER") ?: "INTER") }
  var blockedContactsCount by remember { mutableIntStateOf(0) }
  var editedUsername by remember { mutableStateOf(settings.username) }
  var selectedAvatarIndex by remember { mutableIntStateOf(settings.avatarIndex) }

  // Expanded category tracker (1..13 or null)
  var expandedCategory by remember { mutableStateOf<Int?>(2) } // Default Privacy expanded

  val avatarEmojis = listOf("🛸", "⚡", "👾", "🛡️", "🔮", "🛰️")

  // State toggles
  var autoDownloadMedia by remember { mutableStateOf(false) }
  var notifyMessages by remember { mutableStateOf(true) }
  var notifySounds by remember { mutableStateOf(true) }
  var highContrast by remember { mutableStateOf(false) }
  var reduceAnimations by remember { mutableStateOf(false) }
  var parentalPinLock by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Prototype Header Bar: ← [Username / BaBa_YaGA] with Search, QR Code Share, and Edit Profile
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
          modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = avatarEmojis.getOrElse(settings.avatarIndex) { "🛸" },
            fontSize = 18.sp
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = settings.username,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "@${settings.username.lowercase().replace(" ", "_")}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace
          )
        }

        // Action icons: Search, QR Share, Edit Profile
        IconButton(
          onClick = { isSearchActive = !isSearchActive },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = { showQrDialog = true },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = "QR Code Share",
            tint = MaterialTheme.colorScheme.primary
          )
        }

        IconButton(
          onClick = { showUsernameDialog = true },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Profile",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Optional Inline Search Bar
      if (isSearchActive) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search settings...", fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
    }

    // 1. 🔑 Account — Security notifications, change number / node info, App Lock
    item {
      PrototypeSettingItemCard(
        index = 1,
        title = "Account",
        subtitle = "Security notifications, app lock type, node info",
        icon = Icons.Default.AccountCircle,
        isExpanded = expandedCategory == 1,
        onToggle = { expandedCategory = if (expandedCategory == 1) null else 1 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingDetailRow("Node Identifier", settings.userId)
          SettingDetailRow("Security Verification", "ECDH secp256r1 Ephemeral Key Pairs")
          SettingDetailRow("E2EE Strict Enforcement", if (settings.e2eeStrictEnabled) "Enabled (AES-256-GCM)" else "Disabled")

          // Dynamic Security & Lock Password Type Configuration
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "App Lock Authentication Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Active: ${currentLockType.label} • ${currentLockType.description}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Button(
                  onClick = {
                    securityA1 = ""
                    securityA2 = ""
                    securityA3 = ""
                    securityVerificationError = null
                    isVerifyingQuestions = true
                    scope.launch {
                      val questions = onGetSecurityQuestions(settings.userId)
                      isVerifyingQuestions = false
                      if (questions != null) {
                        securityQ1 = questions.first
                        securityQ2 = questions.second
                        securityQ3 = questions.third
                        showSecurityVerificationDialog = true
                      } else {
                        // If no custom questions are saved, default to direct setup
                        newLockSecretInput = ""
                        lockConfigMessage = null
                        showLockConfigDialog = true
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("Change Password / Lock Format", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    // 2. 🔒 Privacy — Blocked contacts, disappearing messages, read receipts
    item {
      PrototypeSettingItemCard(
        index = 2,
        title = "Privacy",
        subtitle = "Blocked contacts, disappearing messages, read receipts",
        icon = Icons.Default.Lock,
        isExpanded = expandedCategory == 2,
        onToggle = { expandedCategory = if (expandedCategory == 2) null else 2 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Blocked Contacts
          SettingDetailRow("Blocked Contacts", "$blockedContactsCount contacts blocked")

          // Disappearing Messages (24-hr auto-delete)
          Text(
            text = "DISAPPEARING MESSAGES & EPHEMERAL BURN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            RetentionRule.values().forEach { rule ->
              RetentionRuleItem(
                title = rule.label,
                subtitle = when (rule) {
                  RetentionRule.BURN_ON_READ -> "15-second ephemeral burn upon opening message"
                  RetentionRule.RETENTION_24_HOURS -> "Automatically purge decrypted copy after 24 hours"
                  RetentionRule.PERMANENT -> "Retain encrypted offline copy in local SQLite Room table"
                },
                isSelected = settings.retentionRule == rule,
                onClick = { onSelectRetentionRule(rule) }
              )
            }
          }

          // Read Receipts
          PrototypeToggleRow(
            title = "Read Receipts",
            subtitle = "If turned off, you won't send or receive Read Receipts for encrypted P2P messages",
            checked = readReceiptsEnabled,
            onCheckedChange = {
              readReceiptsEnabled = it
              lockPrefs.edit().putBoolean("read_receipts_enabled", it).apply()
            }
          )

          // Instant Message Wipe on Leave
          PrototypeToggleRow(
            title = "Instant Message Wipe on Leave",
            subtitle = "Immediately purges conversation history and flushes media cache upon pausing or navigating away from chat",
            checked = instantWipeOnLeaveEnabled,
            onCheckedChange = {
              instantWipeOnLeaveEnabled = it
              lockPrefs.edit().putBoolean("instant_wipe_on_leave", it).apply()
              Toast.makeText(context, if (it) "Instant wipe active on chat exit" else "Instant wipe disabled", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }



    // 4. 💬 Chats — Chat history, backup, auto-delete rules
    item {
      PrototypeSettingItemCard(
        index = 4,
        title = "Chats",
        subtitle = "Chat history, backup, auto-delete rules",
        icon = Icons.AutoMirrored.Filled.Chat,
        isExpanded = expandedCategory == 4,
        onToggle = { expandedCategory = if (expandedCategory == 4) null else 4 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onOpenPipeline,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Crypto Pipeline Lab (Packet Fragmentation)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          SettingDetailRow("Local SQLite Room Storage", "Encrypted with AES-256 master key")
          SettingDetailRow("Media Folder Base", settings.mediaStorageBaseFolder)
        }
      }
    }

    // 5. 🎨 Appearance — 4 Dynamic Themes & 10 Instant Font Styles
    item {
      PrototypeSettingItemCard(
        index = 5,
        title = "Appearance",
        subtitle = "4 Working themes, 10 instant font styles, unified dark cyber aesthetic",
        icon = Icons.Default.ColorLens,
        isExpanded = expandedCategory == 5,
        onToggle = { expandedCategory = if (expandedCategory == 5) null else 5 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // A. 4 DISTINCT WORKING THEMES
          Text(
            text = "4 WORKING APP THEMES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BluefiThemeOption.values().forEach { themeOption ->
              val isSelected = settings.theme == themeOption
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                  .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable {
                    onSelectTheme(themeOption)
                    DynamicThemeManager.setSelectedTheme(context, themeOption)
                    Toast.makeText(context, "Theme set to ${themeOption.label}", Toast.LENGTH_SHORT).show()
                  }
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(themeOption.accentColor, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = themeOption.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = when (themeOption) {
                      BluefiThemeOption.DARK_FAST_BLUE -> "Navy Slate (#0F172A) • Cobalt Cards • Fast Blue (#2563EB)"
                      BluefiThemeOption.JET_BLACK_CYBER -> "Jet Black (#0A0A0A) • Dark Cards • Neon Rose (#FF2D95)"
                      BluefiThemeOption.CERULEAN_BLUE -> "Cerulean (#002B36) • Deep Cards • Cerulean Accent (#007BA7)"
                      BluefiThemeOption.EMERALD_MESH -> "Emerald (#051A10) • OLED Mesh • Emerald Accent (#10B981)"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          // B. 10 WORKING FONT STYLES (INSTANT RUNTIME SWITCH)
          Text(
            text = "10 WORKING FONT STYLES (GOOGLE FONTS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            BluefiFontStyle.values().forEach { fontStyleOption ->
              val isSelected = selectedFontStyle == fontStyleOption.key
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                  .clickable {
                    selectedFontStyle = fontStyleOption.key
                    lockPrefs.edit().putString("app_font_style", fontStyleOption.key).apply()
                    DynamicFontManager.setFontInstant(context, fontStyleOption)
                    Toast.makeText(context, "Typography set to ${fontStyleOption.label}", Toast.LENGTH_SHORT).show()
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = fontStyleOption.label,
                  fontFamily = fontStyleOption.fontFamily,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }

    // 6. 📻 Broadcasts — Manage lists and send broadcasts
    item {
      PrototypeSettingItemCard(
        index = 6,
        title = "Broadcasts",
        subtitle = "Manage lists and send broadcasts",
        icon = Icons.Default.Radio,
        isExpanded = expandedCategory == 6,
        onToggle = { expandedCategory = if (expandedCategory == 6) null else 6 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SettingDetailRow("2.4 GHz RF Carrier", settings.rfFrequencyWave)
          SettingDetailRow("Broadcast Scope", "Local Wi-Fi P2P Group Owner & BLE Advertising")
          SettingDetailRow("Multi-Hop Range", "Up to 2.5 km via relay node hops")
        }
      }
    }

    // 7. 🔔 Notifications — Message, group & call tones
    item {
      PrototypeSettingItemCard(
        index = 7,
        title = "Notifications",
        subtitle = "Message, group & call tones",
        icon = Icons.Default.Notifications,
        isExpanded = expandedCategory == 7,
        onToggle = { expandedCategory = if (expandedCategory == 7) null else 7 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PrototypeToggleRow(
            title = "Message Notifications",
            subtitle = "Show notification alert banners for incoming encrypted messages",
            checked = notifyMessages,
            onCheckedChange = { notifyMessages = it }
          )
          PrototypeToggleRow(
            title = "Peer Discovery Tones",
            subtitle = "Play short chime when new 2.4 GHz nodes enter RF range",
            checked = notifySounds,
            onCheckedChange = { notifySounds = it }
          )
        }
      }
    }

    // 8. 💾 Storage and data — Network usage, auto-download
    item {
      PrototypeSettingItemCard(
        index = 8,
        title = "Storage and data",
        subtitle = "Network usage, auto-download",
        icon = Icons.Default.SdStorage,
        isExpanded = expandedCategory == 8,
        onToggle = { expandedCategory = if (expandedCategory == 8) null else 8 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PrototypeToggleRow(
            title = "Auto-download P2P media",
            subtitle = "Automatically accept and decrypt voice notes and photos over Wi-Fi Direct",
            checked = autoDownloadMedia,
            onCheckedChange = { autoDownloadMedia = it }
          )
          SettingDetailRow("Internal App Storage", "Database & encrypted keys strictly on device")
          SettingDetailRow("Zero Cloud Dependency", "100% Offline — No telemetry uploaded anywhere")
        }
      }
    }

    // 8B. 🔋 Battery Saver — Conserve device power during long-term mesh connectivity
    item {
      PrototypeSettingItemCard(
        index = 88,
        title = "Battery Saver",
        subtitle = if (settings.isBatterySaverMode) "Active • Reduced mesh search & low-power radio duty cycle" else "Reduces foreground service frequency & mesh search intervals",
        icon = Icons.Default.PowerSettingsNew,
        isExpanded = expandedCategory == 88,
        onToggle = { expandedCategory = if (expandedCategory == 88) null else 88 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PrototypeToggleRow(
            title = "Battery Saver Mode",
            subtitle = "Reduces foreground service polling and increases mesh search intervals to 12s to preserve battery life during continuous mesh connectivity",
            checked = settings.isBatterySaverMode,
            onCheckedChange = { isEnabled ->
              onToggleBatterySaver(isEnabled)
              Toast.makeText(
                context,
                if (isEnabled) "Battery Saver Mode Enabled 🔋" else "Battery Saver Mode Disabled",
                Toast.LENGTH_SHORT
              ).show()
            }
          )
          SettingDetailRow("Mesh Beacon Frequency", if (settings.isBatterySaverMode) "12 seconds (Low-Power Duty Cycle)" else "4 seconds (Real-Time Ultra-Fast)")
          SettingDetailRow("BLE Scanner Profile", if (settings.isBatterySaverMode) "SCAN_MODE_LOW_POWER (Preserves battery)" else "SCAN_MODE_LOW_LATENCY (High responsiveness)")
          SettingDetailRow("Background Inactivity Cutoff", if (settings.isBatterySaverMode) "30s auto-pause during inactivity" else "60s auto-pause during inactivity")
        }
      }
    }

    // 9. 🛡️ Parental controls — Settings for your family
    item {
      PrototypeSettingItemCard(
        index = 9,
        title = "Parental controls",
        subtitle = "Settings for your family",
        icon = Icons.Default.FamilyRestroom,
        isExpanded = expandedCategory == 9,
        onToggle = { expandedCategory = if (expandedCategory == 9) null else 9 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PrototypeToggleRow(
            title = "Require PIN to Pair New Devices",
            subtitle = "Prevent connecting to unknown nodes without 6-digit confirmation",
            checked = parentalPinLock,
            onCheckedChange = { parentalPinLock = it }
          )
          SettingDetailRow("Protected Mode", "Gated behind local account password")
        }
      }
    }

    // 10. ♿ Accessibility — Increase contrast, animation
    item {
      PrototypeSettingItemCard(
        index = 10,
        title = "Accessibility",
        subtitle = "Increase contrast, animation",
        icon = Icons.Default.Accessibility,
        isExpanded = expandedCategory == 10,
        onToggle = { expandedCategory = if (expandedCategory == 10) null else 10 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PrototypeToggleRow(
            title = "High Contrast Mode",
            subtitle = "Enhance outline borders and status badges for improved visibility",
            checked = highContrast,
            onCheckedChange = { highContrast = it }
          )
          PrototypeToggleRow(
            title = "Reduce Animations",
            subtitle = "Simplify screen transitions for low-power or e-ink operation",
            checked = reduceAnimations,
            onCheckedChange = { reduceAnimations = it }
          )
        }
      }
    }

    // 11. 🌐 App language — English (device's language)
    item {
      PrototypeSettingItemCard(
        index = 11,
        title = "App language",
        subtitle = "English (device's language)",
        icon = Icons.Default.Language,
        isExpanded = expandedCategory == 11,
        onToggle = { expandedCategory = if (expandedCategory == 11) null else 11 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SettingDetailRow("Selected Language", "English (United States)")
          SettingDetailRow("System Locale", "Automatic device locale matching")
        }
      }
    }

    // 12. ℹ️ About — App details, tech architecture, and developer profile
    item {
      PrototypeSettingItemCard(
        index = 12,
        title = "About",
        subtitle = "App details, tech architecture, and developer profile",
        icon = Icons.Default.Info,
        isExpanded = expandedCategory == 12,
        onToggle = { expandedCategory = if (expandedCategory == 12) null else 12 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingDetailRow("Developer Info", "Naman Kumar . CU (Chandigarh University)")
          SettingDetailRow("Course / Batch", "Bachelor of Computer Application | 2024")
          SettingDetailRow("Core Stack", "Rust Core Engine + Tokio Async Reactor + Tauri 2.0 / Native Android SDK")
          SettingDetailRow("Features", "Dual-Radio BLE 5.0 & Wi-Fi Direct Auto-Failover, 64KB AES-256-GCM Chunked Encryption, Universal File Sharing, Outbox Waiting Area")

          Button(
            onClick = {
              context.startActivity(Intent(context, com.example.ui.activity.AboutActivity::class.java))
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("open_about_activity_button")
          ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open About Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = { showDeveloperDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("View Full System Architecture", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 13. 👥 Invite a friend — Share offline APK / P2P invite link
    item {
      PrototypeSettingItemCard(
        index = 13,
        title = "Invite a friend",
        subtitle = "Share offline APK / P2P invite link",
        icon = Icons.Default.PersonAdd,
        isExpanded = expandedCategory == 13,
        onToggle = { expandedCategory = if (expandedCategory == 13) null else 13 }
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Share Bluefi APK offline over Bluetooth or Wi-Fi Direct so nearby peers can install and join your mesh immediately:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Button(
            onClick = {
              ApkShareManager.shareNativeApk(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Native Bluefy2.0_Offline.apk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 🚪 Log out (Red Highlight Action)
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onLogout)
          .testTag("logout_card_action")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PowerSettingsNew,
              contentDescription = "Log out",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Log out",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.error
            )
            Text(
              text = "Disconnect node identity and return to login lock",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // QR Code Share Dialog
  if (showQrDialog) {
    AlertDialog(
      onDismissRequest = { showQrDialog = false },
      title = { Text("Offline P2P Node Identity", fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
              .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(text = avatarEmojis.getOrElse(settings.avatarIndex) { "🛸" }, fontSize = 32.sp)
          }
          Text(
            text = settings.username,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Node ID: ${settings.userId}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Scan this offline identity with any nearby Bluefi node to pair instantly without internet.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showQrDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Done")
        }
      }
    )
  }

  // Edit Username & Avatar Dialog
  if (showUsernameDialog) {
    AlertDialog(
      onDismissRequest = { showUsernameDialog = false },
      title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = editedUsername,
            onValueChange = { editedUsername = it },
            label = { Text("Display Username") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Text("Select Avatar Icon:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            avatarEmojis.forEachIndexed { index, emoji ->
              val isSelected = selectedAvatarIndex == index
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surface
                  )
                  .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                  )
                  .clickable { selectedAvatarIndex = index },
                contentAlignment = Alignment.Center
              ) {
                Text(text = emoji, fontSize = 18.sp)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editedUsername.isNotBlank()) {
              onUpdateProfile(editedUsername.trim(), selectedAvatarIndex)
            }
            showUsernameDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUsernameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Internal Security Verification Dialog Guarding Password / Lock Format Change
  if (showSecurityVerificationDialog) {
    AlertDialog(
      onDismissRequest = { showSecurityVerificationDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Security Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Answer your 3 security questions to authorize password / lock format change:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Text(text = "Q1: $securityQ1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = securityA1,
              onValueChange = { securityA1 = it },
              placeholder = { Text("Answer 1", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Text(text = "Q2: $securityQ2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = securityA2,
              onValueChange = { securityA2 = it },
              placeholder = { Text("Answer 2", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Text(text = "Q3: $securityQ3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = securityA3,
              onValueChange = { securityA3 = it },
              placeholder = { Text("Answer 3", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          if (securityVerificationError != null) {
            Text(
              text = securityVerificationError ?: "",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (securityA1.isBlank() || securityA2.isBlank() || securityA3.isBlank()) {
              securityVerificationError = "Please answer all 3 security questions."
              return@Button
            }
            scope.launch {
              val verified = onVerifySecurityAnswers(settings.userId, securityA1, securityA2, securityA3)
              if (verified) {
                showSecurityVerificationDialog = false
                newLockSecretInput = ""
                lockConfigMessage = null
                showLockConfigDialog = true
              } else {
                securityVerificationError = "Security answers incorrect. Password change denied."
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Verify & Continue")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSecurityVerificationDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Dynamic App Lock Configuration Dialog
  if (showLockConfigDialog) {
    var selectedType by remember { mutableStateOf(currentLockType) }
    var enteredSecret by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showLockConfigDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Configure App Lock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Select your preferred security authentication method:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          AppLockType.values().forEach { lockType ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (selectedType == lockType) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                  else MaterialTheme.colorScheme.surface
                )
                .clickable {
                  selectedType = lockType
                  enteredSecret = ""
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedType == lockType,
                onClick = {
                  selectedType = lockType
                  enteredSecret = ""
                },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(text = lockType.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = lockType.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          when (selectedType) {
            AppLockType.PIN_4 -> {
              OutlinedTextField(
                value = enteredSecret,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredSecret = it },
                label = { Text("Set 4-Digit PIN") },
                placeholder = { Text("e.g. 1234") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
            AppLockType.PIN_6 -> {
              OutlinedTextField(
                value = enteredSecret,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) enteredSecret = it },
                label = { Text("Set 6-Digit PIN") },
                placeholder = { Text("e.g. 123456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
            AppLockType.PASSWORD -> {
              OutlinedTextField(
                value = enteredSecret,
                onValueChange = { enteredSecret = it },
                label = { Text("Set Alphanumeric Password") },
                placeholder = { Text("e.g. Pass@123") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }

          }

          if (lockConfigMessage != null) {
            Text(
              text = lockConfigMessage ?: "",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val valid = when (selectedType) {
              AppLockType.PIN_4 -> enteredSecret.length == 4
              AppLockType.PIN_6 -> enteredSecret.length == 6
              AppLockType.PASSWORD -> enteredSecret.length >= 6
            }
            if (!valid) {
              lockConfigMessage = when (selectedType) {
                AppLockType.PIN_4 -> "Please enter exactly 4 digits."
                AppLockType.PIN_6 -> "Please enter exactly 6 digits."
                AppLockType.PASSWORD -> "Password must be at least 6 characters."
              }
              return@Button
            }

            val secretString = enteredSecret.trim()

            // Generate SHA-256 hash of the passcode/PIN
            val md = MessageDigest.getInstance("SHA-256")
            val digestBytes = md.digest(secretString.toByteArray(Charsets.UTF_8))
            val sha256Hex = digestBytes.joinToString("") { "%02x".format(it) }

            // Atomically overwrite both lock type and password hash in preferences
            lockPrefs.edit()
              .putString("SELECTED_LOCK_TYPE", selectedType.name)
              .putString("SELECTED_LOCK_SECRET", secretString)
              .putString("STORED_PASSWORD_HASH", sha256Hex)
              .putString("PASSWORD_HASH_" + settings.userId, sha256Hex)
              .putString("LOCK_TYPE_" + settings.userId, selectedType.name)
              .putString("app_lock_type", selectedType.name)
              .putString("app_lock_secret", secretString)
              .putString("lock_type_" + settings.userId, selectedType.name)
              .putString("lock_secret_" + settings.userId, secretString)
              .apply()

            // Update local Room DB and authentication repository atomically
            onUpdateLockFormatAndSecret(settings.userId, selectedType.name, secretString)

            currentLockType = selectedType
            showLockConfigDialog = false
            Toast.makeText(context, "Lock format updated to ${selectedType.label}", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Save Lock")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLockConfigDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

    if (showDeveloperDialog) {
    DeveloperArchitectureDialog(onDismiss = { showDeveloperDialog = false })
  }
}

/**
 * Prototype Setting Item Card matching the WhatsApp/Signal style row
 */
@Composable
private fun PrototypeSettingItemCard(
  index: Int,
  title: String,
  subtitle: String,
  icon: ImageVector,
  isExpanded: Boolean,
  onToggle: () -> Unit,
  content: @Composable () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isExpanded) 1.5.dp else 1.dp,
      color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("setting_category_$index")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(
                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = title,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = subtitle,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = if (isExpanded) 3 else 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse" else "Expand",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          content()
        }
      }
    }
  }
}

@Composable
private fun PrototypeToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary
      )
    )
  }
}

@Composable
private fun SettingDetailRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
      .padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
private fun ThemeOptionItem(
  title: String,
  description: String,
  colorBox: Color,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(22.dp)
        .background(colorBox, RoundedCornerShape(6.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    if (isSelected) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Selected",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun RetentionRuleItem(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = isSelected,
      onClick = onClick,
      colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
