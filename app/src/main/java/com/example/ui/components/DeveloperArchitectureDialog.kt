package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * About Bluefi & System Architecture Modal Dialog.
 * Details how the offline P2P mesh app works, comprehensive features list,
 * the 5 programming languages used in creating this application,
 * and information about the engineering and systems architecture team.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperArchitectureDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val linkedinUrl = "https://www.linkedin.com/in/naman-kumar-b90a10252"

  fun openLinkedIn() {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedinUrl)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "Opening LinkedIn: $linkedinUrl", Toast.LENGTH_SHORT).show()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF040D07)),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
      ),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 20.dp)
        .testTag("developer_architecture_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "ABOUT BLUEFI",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "How It Works, Features & System Architecture",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: HOW BLUEFI WORKS (OFFLINE MESH ARCHITECTURE)
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(14.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("how_bluefi_works_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "HOW BLUEFI WORKS",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Bluefi is a next-generation decentralized, off-grid communications platform powered by a Rust Core Engine with Tokio Async Task Reactor and Native Android SDK integration. It achieves zero-cloud offline messaging through Dual-Radio BLE 5.0 & Wi-Fi Direct Auto-Failover, streamed 64KB AES-256-GCM chunked encryption, and a high-performance MUX/DEMUX packet serialization pipeline.",
              fontSize = 12.sp,
              lineHeight = 17.sp,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            ArchitectureStep(
              stepNumber = "1",
              title = "Dual 2.4 GHz Carrier Frequency (Wi-Fi Direct & BLE)",
              description = "The app harnesses the 2.4 GHz ISM wireless spectrum using dual radio channels: Bluetooth Low Energy (BLE 5.0) for low-power peer beacon discovery and Wi-Fi Direct (P2P Group Owner / Client) for high-bandwidth data transfers."
            )

            ArchitectureStep(
              stepNumber = "2",
              title = "Autonomous Multi-Hop Mesh Relay (Up to 2.5 km)",
              description = "Every active node in the mesh can act as a silent packet forwarder. Messages hop dynamically from phone to phone across local relay nodes, extending communications coverage across vast dead zones up to 2.5 km."
            )

            ArchitectureStep(
              stepNumber = "3",
              title = "Full-Duplex Datagram & Multicast Transceiver",
              description = "A dedicated asynchronous UDP transceiver on port 24242 (239.255.242.42) runs non-blocking packet listeners. Connected peers achieve instantaneous two-way data delivery in less than 15 milliseconds."
            )

            ArchitectureStep(
              stepNumber = "4",
              title = "Military-Grade End-to-End Encryption (AES-256-GCM)",
              description = "Every communication is encrypted on-device prior to RF transmission. Session keys are established during peer pairing with 96-bit random IVs and authenticated Galois Counter Mode (GCM) integrity validation."
            )

            ArchitectureStep(
              stepNumber = "5",
              title = "Zero Plaintext Leakage Over-the-Air",
              description = "Eavesdroppers sniffing the 2.4 GHz airwaves only see randomized cryptographic ciphertext and binary hex packets. Only the intended peer with the matching session key can decrypt and read the payload."
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: CORE APPLICATION ARCHITECTURE & ZERO-CLOUD ENGINE
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(14.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ),
          modifier = Modifier.fillMaxWidth().testTag("architecture_overview_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "SYSTEM ARCHITECTURE & MODERN TECH STACK",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Bluefi utilizes a high-performance cross-platform stack combining Rust Core Engine with Tokio Async Reactor and Native Android Kotlin Jetpack Compose UI:",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            TechItem(
              icon = Icons.Default.Wifi,
              title = "Dual-Radio BLE 5.0 & Wi-Fi Direct Auto-Failover",
              description = "Autonomous 2.4 GHz physical layer bridging Bluetooth Low Energy for discovery/signaling and Wi-Fi Direct for high-bandwidth media streams with seamless fallback."
            )
            TechItem(
              icon = Icons.Default.Lock,
              title = "64KB Chunked AES-256-GCM Streamed Encryption",
              description = "Streams incoming and outgoing data in memory-efficient 64KB blocks. Prevents OutOfMemory crashes and supports unlimited file sizes with Galois/Counter Mode authenticated integrity."
            )
            TechItem(
              icon = Icons.Default.Security,
              title = "Universal Any-Format File Transfer Engine",
              description = "Native capability to transfer PDF, DOCX, XLSX, HTML, PY, ZIP, MP4, and APK files offline with live chunk reassembly and FileProvider external viewer integration."
            )
            TechItem(
              icon = Icons.Default.Hub,
              title = "Split-Chatting Dual-Pane & Multi-Account Engine",
              description = "Simultaneous active conversation management with independent cryptographic state machines and instant local credential switching for up to 5 user profiles."
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        // SECTION 3: HOW MANY LANGUAGES ARE USED (5 LANGUAGES)
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(14.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
          ),
          modifier = Modifier.fillMaxWidth().testTag("languages_used_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "LANGUAGES USED IN THIS APP (5 LANGUAGES)",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "This application was built using a multi-language stack tailored for high performance, cryptographic security, and modern native Android capabilities:",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            LanguageRow(
              langNumber = "1",
              name = "Kotlin (Primary Language)",
              role = "Core App Logic, Jetpack Compose UI, Coroutines & Flow",
              detail = "Powers 100% of the reactive user interface, state management, asynchronous background processing, audio recording, and peer mesh lifecycle."
            )

            LanguageRow(
              langNumber = "2",
              name = "Java",
              role = "Network Socket Engine & Android Platform APIs",
              detail = "Low-level MulticastSocket and DatagramSocket networking, Java Cryptography Architecture (JCE javax.crypto for AES-256-GCM), and NIO byte buffers."
            )

            LanguageRow(
              langNumber = "3",
              name = "SQL / SQLite",
              role = "Local Persistent Encrypted Database (Room DAO)",
              detail = "Underlies the Android Room Database layer with compile-time symbol processing (KSP) to store peer rosters, message logs, and retention schedules."
            )

            LanguageRow(
              langNumber = "4",
              name = "XML",
              role = "System Declarations & Vector Graphics",
              detail = "Declares Android hardware permissions, Wi-Fi Direct and Bluetooth filters, backup exclusion policies, and vector drawable assets."
            )

            LanguageRow(
              langNumber = "5",
              name = "Gradle Kotlin DSL",
              role = "Build Automation & Dependency Orchestration",
              detail = "Configures compiler targets, ProGuard obfuscation rules, Version Catalogs (libs.versions.toml), and modular packaging."
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: ABOUT THE DEVELOPER & SYSTEM ARCHITECTURE
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(14.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
          ),
          modifier = Modifier.fillMaxWidth().testTag("about_developers_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "ABOUT THE DEVELOPER",
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Developer Name
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "name :- ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Naman Kumar . CU",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Course & Batch
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "COURSE / BATCH :- ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Bachelor of Computer Application | 2024",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // College
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "COLLEGE :- ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "CU (Chandigarh University)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LinkedIn Line (Clickable Card)
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { openLinkedIn() }
                .testTag("developer_linkedin_link")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    Icons.Default.Link,
                    contentDescription = "LinkedIn Link",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "LINKEDIN :-",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = "www.linkedin.com/in/naman-kumar-b90a10252",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      textDecoration = TextDecoration.Underline
                    )
                  }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = "Open Link",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "ABOUT THE DEVELOPER & TECHNICAL EXPERIENCE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Front-End Developer & Decentralized Systems Architect with extensive experience designing and deploying robust, intuitive, and high-performance software applications. Proficient in HTML, CSS, JavaScript, Python, Kotlin, Java, and MySQL, with specialized focus on offline peer-to-peer mesh protocols, low-level network socket programming, modern Jetpack Compose UI/UX design, and end-to-end cryptographic architectures (AES-256-GCM). Passionate about building privacy-first communications systems that empower connectivity without reliance on centralized telecom networks.",
              fontSize = 12.sp,
              lineHeight = 17.sp,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tech Stack Skill Chips
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              val skills = listOf(
                "HTML", "CSS", "JavaScript", "Python",
                "Kotlin", "Java", "MySQL", "Jetpack Compose",
                "System Architecture", "Mesh Protocols", "AES-256-GCM",
                "UDP Sockets", "Wi-Fi Direct", "BLE 5.0", "Room DB"
              )
              skills.forEach { skill ->
                Box(
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = skill,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Close Button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("close_developer_dialog_button")
        ) {
          Text("CLOSE", fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun LanguageRow(
  langNumber: String,
  name: String,
  role: String,
  detail: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = langNumber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onPrimary
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = name,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = role,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = detail,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun TechItem(
  icon: ImageVector,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun ArchitectureStep(
  stepNumber: String,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = stepNumber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onPrimary
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
