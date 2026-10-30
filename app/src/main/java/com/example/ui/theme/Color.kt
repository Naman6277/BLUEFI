package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// 4 DISTINCT WORKING THEMES PALETTES
// =========================================================================

// 1. Classic Dark Navy Slate (Default)
val NavySlateBackground = Color(0xFF0F172A)
val NavySlateCards = Color(0xFF1E293B)
val NavySlateAccent = Color(0xFF2563EB)

// 2. Jet Black Cyber
val JetBlackBackground = Color(0xFF0A0A0A)
val JetBlackCards = Color(0xFF18181B)
val JetBlackAccent = Color(0xFFFF2D95)

// 3. Cerulean Blue
val CeruleanBackground = Color(0xFF002B36)
val CeruleanCards = Color(0xFF004052)
val CeruleanAccent = Color(0xFF007BA7)

// 4. Emerald Mesh (OLED)
val EmeraldBackground = Color(0xFF051A10)
val EmeraldCards = Color(0xFF0A2E1C)
val EmeraldAccent = Color(0xFF10B981)

// Common text & utility colors
val IceWhite = Color(0xFFF8FAFC)
val MutedSteel = Color(0xFF94A3B8)
val CyberBorderOutline = Color(0xFF334155)

// Default fallbacks
val CanvasBackground = NavySlateBackground
val DeepSlateNavy = NavySlateBackground
val CardContainer = NavySlateCards
val DarkCobalt = NavySlateCards
val FastElectricBlue = NavySlateAccent
val FastBluePrimaryVariant = Color(0xFF1D4ED8)

val StatusMintGreen = Color(0xFF10B981)
val StatusCyan = Color(0xFF00F5D4)
val TextPrimary = IceWhite
val TextMuted = MutedSteel
val BorderOutline = CyberBorderOutline

val FastBlueBg = CanvasBackground
val FastBlueSurface = CardContainer
val FastBlueSurfaceVariant = CardContainer
val FastBluePrimary = FastElectricBlue
val FastBlueSecondary = StatusCyan
val FastBlueBorder = BorderOutline
val FastBlueText = TextPrimary
val FastBlueTextMuted = TextMuted

val PacketMuxColor = Color(0xFFFFB300)
val PacketDemuxColor = Color(0xFFAB47BC)
val WaveformBlue = FastElectricBlue
val BurnWarningRed = Color(0xFFEF4444)
val EncryptionShieldGold = Color(0xFFFFD700)
