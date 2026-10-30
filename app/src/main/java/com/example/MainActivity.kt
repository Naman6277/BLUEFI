package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.BluefiViewModel
import com.example.ui.components.BatteryOptimizationDialog
import com.example.ui.components.HardwarePermissionsDialog
import com.example.ui.components.PacketInspectorDialog
import com.example.ui.components.RadioAlertPopupDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatsInboxScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PipelineLabScreen
import com.example.ui.screens.RadarMeshScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplitChatScreen
import com.example.ui.theme.BluefiTheme
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Home : Screen("home", "Home", Icons.Default.Home)
  object Chats : Screen("chats", "Chats", Icons.AutoMirrored.Filled.Chat)
  object SplitChat : Screen("split_chat", "Split Chat", Icons.AutoMirrored.Filled.CallSplit)
  object Profile : Screen("profile", "Profile", Icons.Default.Person)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)
  object Radar : Screen("radar", "Radar", Icons.Default.Radar)
  object Pipeline : Screen("pipeline", "Pipeline", Icons.Default.Code)
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.util.DynamicThemeManager.applyThemeToActivity(this)
    com.example.util.DynamicFontManager.initialize(this)
    enableEdgeToEdge()

    setContent {
      val viewModel: BluefiViewModel = viewModel()
      val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
      val activeFont by com.example.util.DynamicFontManager.activeFontState.collectAsStateWithLifecycle()

      BluefiTheme(themeOption = userSettings.theme, fontStyle = activeFont) {
        BluefiApp(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Auto-Resume on App Launch: immediately re-activate P2pForegroundService and resume P2P socket communication
    com.example.data.service.P2pForegroundService.onAppForegrounded(this)
    com.example.data.service.P2pForegroundService.start(this)
  }

  override fun onStop() {
    super.onStop()
    // 1-Minute Idle Timer in Background: monitor socket traffic and pause polling if inactive for 60s
    com.example.data.service.P2pForegroundService.onAppBackgrounded(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isFinishing) {
      try {
        val app = applicationContext as? BluefiApplication
        app?.repository?.shutdownAllSockets()
      } catch (_: Exception) {}
    }
  }
}

@Composable
fun BluefiApp(viewModel: BluefiViewModel) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
  val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
  val allPeers by viewModel.allPeers.collectAsStateWithLifecycle()
  val networkStats by viewModel.networkStats.collectAsStateWithLifecycle()
  val activeTrace by viewModel.activeTrace.collectAsStateWithLifecycle()
  val inspectingMessage by viewModel.inspectingMessage.collectAsStateWithLifecycle()
  val peerTypingMap by viewModel.peerTypingMap.collectAsStateWithLifecycle()

  // Split chat states
  val splitLeftPeerId by viewModel.splitLeftPeerId.collectAsStateWithLifecycle()
  val splitRightPeerId by viewModel.splitRightPeerId.collectAsStateWithLifecycle()
  val splitLeftMessages by viewModel.splitLeftMessages.collectAsStateWithLifecycle()
  val splitRightMessages by viewModel.splitRightMessages.collectAsStateWithLifecycle()

  val leftPeer = remember(allPeers, splitLeftPeerId) {
    allPeers.find { it.id == splitLeftPeerId }
  }
  val rightPeer = remember(allPeers, splitRightPeerId) {
    allPeers.find { it.id == splitRightPeerId }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  // Lifecycle observer for background notification tracking and idle timer refresh
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_START || event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        com.example.data.notification.BluefiNotificationManager.isAppInForeground = true
        com.example.data.service.P2pForegroundService.notifyActivity(context)
      } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
        com.example.data.notification.BluefiNotificationManager.isAppInForeground = false
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  var showPermissionsDialog by remember { mutableStateOf(false) }
  var showBatteryDialog by remember { mutableStateOf(false) }

  // 1. AUTOMATIC RUNTIME PERMISSIONS HANDLER
  val permissionsLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    val hasCritical = hasCriticalConnectivityPermissions(context)
    if (!hasCritical) {
      showPermissionsDialog = true
    } else {
      showPermissionsDialog = false
      viewModel.startAutoHardwareDiscovery()
      com.example.data.service.P2pForegroundService.start(context, "2.4 GHz Carrier Active • Connected P2P Socket Channels")
    }

    // After permissions, check battery optimization exemption
    if (!viewModel.isBatteryOptimizationExempted()) {
      showBatteryDialog = true
    }
  }

  // Activity result launcher for Bluetooth system enable dialog
  val enableBtLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) {
    viewModel.checkHardwareRadios()
    viewModel.startAutoHardwareDiscovery()
    com.example.data.service.P2pForegroundService.start(context, "2.4 GHz Carrier Active • Connected P2P Socket Channels")
  }

  // 2. HARDWARE STATE & PAIRING CHECKS: Automatic execution on launch
  LaunchedEffect(Unit) {
    // Check & request runtime permissions
    if (!hasCriticalConnectivityPermissions(context)) {
      permissionsLauncher.launch(getRequiredHardwarePermissions())
    } else {
      viewModel.startAutoHardwareDiscovery()
      com.example.data.service.P2pForegroundService.start(context, "2.4 GHz Carrier Active • Connected P2P Socket Channels")
      if (!viewModel.isBatteryOptimizationExempted()) {
        showBatteryDialog = true
      }
    }

    // Prompt user to enable Bluetooth if disabled
    try {
      val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
      val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
      if (adapter != null && !adapter.isEnabled) {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtLauncher.launch(enableIntent)
      }
    } catch (_: Exception) {}

    // Run hardware radio evaluation
    viewModel.checkHardwareRadios()
  }

  val isAccountRegistered = remember(isLoggedIn) { viewModel.isAccountRegistered() }
  val registeredUserId = remember(isLoggedIn) { viewModel.getRegisteredUserId() }
  val authFeedbackMessage by viewModel.authFeedbackMessage.collectAsStateWithLifecycle()
  val isAuthSuccess by viewModel.isAuthSuccess.collectAsStateWithLifecycle()
  val showAuthPopup by viewModel.showAuthPopup.collectAsStateWithLifecycle()
  val authPopupMessage by viewModel.authPopupMessage.collectAsStateWithLifecycle()
  val authPopupSuccess by viewModel.authPopupSuccess.collectAsStateWithLifecycle()
  val showRadioAlertPopup by viewModel.showRadioAlertPopup.collectAsStateWithLifecycle()
  val radioState by viewModel.hardwareRadioState.collectAsStateWithLifecycle()
  val incomingRadarConnection by viewModel.incomingRadarConnection.collectAsStateWithLifecycle()
  val pairingProgressPeer by viewModel.pairingProgressPeer.collectAsStateWithLifecycle()
  val connectionFailedNotice by viewModel.connectionFailedNotice.collectAsStateWithLifecycle()

  // Auto-shift to ChatScreen on both devices when pairing handshake completes
  LaunchedEffect(isLoggedIn) {
    if (isLoggedIn) {
      viewModel.checkHardwareRadios()
      val activity = (navController.context as? ComponentActivity)
      val deepLinkPeerId = activity?.intent?.getStringExtra("deep_link_peer_id")
      if (!deepLinkPeerId.isNullOrBlank()) {
        viewModel.selectPeer(deepLinkPeerId)
        navController.navigate("chat/$deepLinkPeerId") {
          launchSingleTop = true
        }
      }

      viewModel.autoNavigatePeerId.collect { targetPeerId ->
        viewModel.selectPeer(targetPeerId)
        navController.navigate("chat/$targetPeerId") {
          launchSingleTop = true
        }
      }
    }
  }

  if (!isLoggedIn) {
    LoginScreen(
      isAccountRegistered = isAccountRegistered,
      registeredUserId = registeredUserId,
      authFeedbackMessage = authFeedbackMessage,
      isAuthSuccess = isAuthSuccess,
      showAuthPopup = showAuthPopup,
      authPopupMessage = authPopupMessage,
      authPopupSuccess = authPopupSuccess,
      onDismissPopup = {
        viewModel.dismissAuthPopup()
      },
      onLogin = { enteredPass ->
        viewModel.verifyLoginPassword(enteredPass)
      },
      onLoginAccount = { targetUserId, enteredPass, callback ->
        viewModel.loginToAccount(targetUserId, enteredPass, callback)
      },
      onCreateAccountWithQuestions = { newUserId, newPass, q1, a1, q2, a2, q3, a3 ->
        viewModel.createAccount(newUserId, newPass, q1, a1, q2, a2, q3, a3)
      },
      onResetAccount = {
        viewModel.resetAccount()
      },
      onAccessGranted = {
        viewModel.grantAccess()
      },
      onGetSecurityQuestions = { uid ->
        viewModel.getSecurityQuestions(uid)
      },
      onVerifySecurityAnswers = { uid, a1, a2, a3 ->
        viewModel.verifySecurityAnswers(uid, a1, a2, a3)
      },
      onResetPassword = { uid, newPass ->
        viewModel.resetPasswordWithAnswers(uid, newPass)
      }
    )
    return
  }

  // First page is Home page with logo, QR code, PIN, top QR scanner, and direct communication
  val bottomNavItems = listOf(
    Screen.Home,
    Screen.Chats,
    Screen.SplitChat,
    Screen.Settings
  )

  val isChatRoute = currentRoute?.startsWith("chat/") == true
  val isSplitChatRoute = currentRoute == Screen.SplitChat.route

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (!isChatRoute && !isSplitChatRoute) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("bottom_nav_bar")
        ) {
          bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(screen.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector = screen.icon,
                  contentDescription = screen.title
                )
              },
              label = {
                Text(
                  text = screen.title,
                  fontSize = 11.sp
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier.testTag("nav_item_${screen.route}")
            )
          }
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(if (isChatRoute || isSplitChatRoute) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
    ) {
      NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
          slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
          )
        },
        exitTransition = {
          slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
          )
        },
        popEnterTransition = {
          slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
          )
        },
        popExitTransition = {
          slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
          )
        }
      ) {
        composable(Screen.Home.route) {
          HomeScreen(
            peers = allPeers,
            networkStats = networkStats,
            radioState = radioState,
            localPairingCode = viewModel.getLocalPairingCode(),
            localQrPayload = viewModel.getLocalQrPayload(),
            userSettings = userSettings,
            onPeerSelected = { peerId ->
              viewModel.selectPeer(peerId)
              navController.navigate("chat/$peerId") {
                launchSingleTop = true
              }
            },
            onOpenSplitChat = {
              navController.navigate(Screen.SplitChat.route) {
                launchSingleTop = true
              }
            },
            onOpenPairing = {
              navController.navigate(Screen.Chats.route)
            },
            onScanClicked = {
              viewModel.toggleBluetoothScan(true)
              viewModel.toggleWifiDirectDiscovery(true)
              scope.launch {
                snackbarHostState.showSnackbar("Scanning 2.4 GHz RF channels for nearby nodes...")
              }
            },
            onPairByCode = { code, callback ->
              viewModel.pairDeviceByCode(code) { result ->
                callback(result)
                result.onSuccess { connectedPeer ->
                  viewModel.selectPeer(connectedPeer.id)
                  navController.navigate("chat/${connectedPeer.id}") {
                    launchSingleTop = true
                  }
                }
              }
            },
            onPairByQr = { qrPayload, callback ->
              viewModel.pairDeviceByQr(qrPayload) { result ->
                callback(result)
                result.onSuccess { connectedPeer ->
                  viewModel.selectPeer(connectedPeer.id)
                  navController.navigate("chat/${connectedPeer.id}") {
                    launchSingleTop = true
                  }
                }
              }
            },
            onOpenBluetoothSettings = {
              viewModel.openBluetoothSettings()
            },
            onOpenWifiSettings = {
              viewModel.openWifiSettings()
            },
            onTurnOnBluetoothInternally = {
              viewModel.turnOnBluetoothInternally()
            },
            onTurnOnWifiInternally = {
              viewModel.turnOnWifiInternally()
            },
            onRefreshRadios = {
              viewModel.checkHardwareRadios()
            },
            onLockApp = {
              viewModel.lockApp()
            },
            onConnectRadarPeer = { peer ->
              viewModel.requestRadarConnect(peer)
            },
            pairingProgressPeer = pairingProgressPeer,
            onCancelPairingRequest = {
              viewModel.cancelPairingRequest()
            },
            incomingRadarConnection = incomingRadarConnection,
            onAcceptRadarConnection = { peer ->
              viewModel.acceptRadarConnect(peer)
              viewModel.selectPeer(peer.id)
              navController.navigate("chat/${peer.id}") {
                launchSingleTop = true
              }
            },
            onDeclineRadarConnection = { peer ->
              viewModel.declineRadarConnect(peer)
            },
            connectionFailedMessage = connectionFailedNotice,
            onClearConnectionFailedMessage = {
              viewModel.clearConnectionFailedNotice()
            },
            onSimulatePhonePeer = {
              viewModel.simulateNearbyPhonePeer()
            }
          )
        }

        composable(Screen.Chats.route) {
          ChatsInboxScreen(
            peers = allPeers,
            onPeerSelected = { peerId ->
              viewModel.selectPeer(peerId)
              navController.navigate("chat/$peerId") {
                launchSingleTop = true
              }
            },
            onPairByCode = { code, callback ->
              viewModel.pairDeviceByCode(code) { result ->
                callback(result)
                result.onSuccess { connectedPeer ->
                  viewModel.selectPeer(connectedPeer.id)
                  navController.navigate("chat/${connectedPeer.id}") {
                    launchSingleTop = true
                  }
                }
              }
            },
            onPairByQr = { qrPayload, callback ->
              viewModel.pairDeviceByQr(qrPayload) { result ->
                callback(result)
                result.onSuccess { connectedPeer ->
                  viewModel.selectPeer(connectedPeer.id)
                  navController.navigate("chat/${connectedPeer.id}") {
                    launchSingleTop = true
                  }
                }
              }
            },
            onMarkAllAsRead = {
              viewModel.markAllChatsAsRead()
            },
            onDeleteChat = { peerId ->
              viewModel.deleteChat(peerId)
            },
            onCreateNewChat = { name, initialMsg ->
              viewModel.createNewChat(name, initialMsg)
            }
          )
        }

        composable(
          route = "chat/{peerId}",
          arguments = listOf(navArgument("peerId") { type = NavType.StringType })
        ) { backStackEntry ->
          val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
          LaunchedEffect(peerId) {
            viewModel.selectPeer(peerId)
          }

          val context = androidx.compose.ui.platform.LocalContext.current
          androidx.compose.runtime.DisposableEffect(peerId) {
            com.example.data.notification.BluefiNotificationManager.activeConversationPeerId = peerId
            val lockPrefs = context.getSharedPreferences("bluefi_lock_prefs", android.content.Context.MODE_PRIVATE)
            onDispose {
              com.example.data.notification.BluefiNotificationManager.activeConversationPeerId = null
              val instantWipeEnabled = lockPrefs.getBoolean("instant_wipe_on_leave", false)
              if (instantWipeEnabled && peerId.isNotBlank()) {
                viewModel.clearChat(peerId)
                try {
                  context.cacheDir.deleteRecursively()
                } catch (_: Exception) {}
              }
            }
          }

          val chatPeer = remember(allPeers, peerId) {
            allPeers.find { it.id == peerId }
          }
          val messages by viewModel.selectedPeerMessages.collectAsStateWithLifecycle()
          val keyFingerprint = remember(peerId, messages.size) {
            viewModel.getSessionKeyFingerprint(peerId)
          }

          val isPeerTyping = peerTypingMap[peerId] == true

          ChatScreen(
            peer = chatPeer,
            messages = messages,
            keyFingerprint = keyFingerprint,
            isPeerTyping = isPeerTyping,
            onTyping = { isTyping ->
              viewModel.notifyLocalTyping(peerId, isTyping)
            },
            onSimulatePeerTyping = {
              viewModel.simulatePeerTyping(peerId)
            },
            onBack = { navController.popBackStack() },
            onSendMessage = { text, mediaType, mediaContent ->
              viewModel.sendMessage(peerId, text, mediaType, mediaContent)
            },
            onMessageClick = { message ->
              viewModel.openMessageInspector(message)
            },
            onClearChat = {
              viewModel.clearChat(peerId)
            },
            onVerifyAndConnectPeer = {
              viewModel.verifyAndConnectPeer(peerId)
            }
          )
        }

        composable(Screen.SplitChat.route) {
          SplitChatScreen(
            allPeers = allPeers,
            leftPeer = leftPeer,
            rightPeer = rightPeer,
            leftMessages = splitLeftMessages,
            rightMessages = splitRightMessages,
            onSelectLeftPeer = { id -> viewModel.setSplitPeers(id, splitRightPeerId) },
            onSelectRightPeer = { id -> viewModel.setSplitPeers(splitLeftPeerId, id) },
            onSendMessage = { peerId, text ->
              viewModel.sendMessage(peerId, text)
            },
            onBack = { navController.navigate(Screen.Chats.route) },
            onOpenPairing = { navController.navigate(Screen.Chats.route) }
          )
        }

        composable(Screen.Radar.route) {
          RadarMeshScreen(
            networkStats = networkStats,
            peers = allPeers,
            isRelayNodeActive = userSettings.isRelayNodeActive,
            onToggleRelayNode = { active -> viewModel.toggleRelayNode(active) },
            onTriggerScan = {
              viewModel.toggleBluetoothScan(true)
              scope.launch {
                snackbarHostState.showSnackbar("Broadcasting 2.4 GHz beacon pulse...")
              }
            }
          )
        }

        composable(Screen.Pipeline.route) {
          PipelineLabScreen(
            activeTrace = activeTrace,
            onRunTest = { testText ->
              viewModel.runPipelineDemo(testText)
            }
          )
        }

        composable(Screen.Profile.route) {
          val btDiscoveryState by viewModel.bluetoothDiscoveryState.collectAsStateWithLifecycle()
          SettingsScreen(
            settings = userSettings,
            onUpdateProfile = { name, avatar ->
              viewModel.updateProfile(name, avatar)
              scope.launch { snackbarHostState.showSnackbar("Profile updated") }
            },
            onSelectTheme = { theme ->
              viewModel.setTheme(theme)
            },
            onSelectRetentionRule = { rule ->
              viewModel.setRetentionRule(rule)
              scope.launch { snackbarHostState.showSnackbar("Retention rule applied: " + rule.label) }
            },
            onToggleBluetooth = { enabled ->
              viewModel.toggleBluetoothScan(enabled)
            },
            isAdvertising = btDiscoveryState.isAdvertising,
            onToggleAdvertising = { enabled ->
              viewModel.toggleBluetoothAdvertising(enabled)
              scope.launch {
                val msg = if (enabled) "BLE P2P Beacon advertising active" else "BLE P2P advertising stopped"
                snackbarHostState.showSnackbar(msg)
              }
            },
            onToggleWifiDirect = { enabled ->
              viewModel.toggleWifiDirectDiscovery(enabled)
            },
            onToggleRadarVisibility = { isVisible ->
              viewModel.setRadarVisibility(isVisible)
              scope.launch {
                val msg = if (isVisible) "Visible Mode: Broadcasting to nearby radars" else "Invisible Mode: Device hidden from nearby radars"
                snackbarHostState.showSnackbar(msg)
              }
            },
            onOpenPairing = {
              navController.navigate(Screen.Chats.route)
            },
            onOpenPipeline = {
              navController.navigate(Screen.Pipeline.route)
            },
            onLogout = {
              viewModel.logout()
            },
            onToggleBatterySaver = { enabled ->
              viewModel.toggleBatterySaver(enabled)
            }
          )
        }

        composable(Screen.Settings.route) {
          val btDiscoveryState by viewModel.bluetoothDiscoveryState.collectAsStateWithLifecycle()
          SettingsScreen(
            settings = userSettings,
            onBack = { navController.navigate(Screen.SplitChat.route) },
            onUpdateProfile = { name, avatar ->
              viewModel.updateProfile(name, avatar)
              scope.launch { snackbarHostState.showSnackbar("Profile updated") }
            },
            onSelectTheme = { theme ->
              viewModel.setTheme(theme)
            },
            onSelectRetentionRule = { rule ->
              viewModel.setRetentionRule(rule)
              scope.launch { snackbarHostState.showSnackbar("Retention rule applied: ${rule.label}") }
            },
            onToggleBluetooth = { enabled ->
              viewModel.toggleBluetoothScan(enabled)
            },
            isAdvertising = btDiscoveryState.isAdvertising,
            onToggleAdvertising = { enabled ->
              viewModel.toggleBluetoothAdvertising(enabled)
              scope.launch {
                val msg = if (enabled) "BLE P2P Beacon advertising active" else "BLE P2P advertising stopped"
                snackbarHostState.showSnackbar(msg)
              }
            },
            onToggleWifiDirect = { enabled ->
              viewModel.toggleWifiDirectDiscovery(enabled)
            },
            onToggleRadarVisibility = { isVisible ->
              viewModel.setRadarVisibility(isVisible)
              scope.launch {
                val msg = if (isVisible) "Visible Mode: Broadcasting to nearby radars" else "Invisible Mode: Device hidden from nearby radars"
                snackbarHostState.showSnackbar(msg)
              }
            },
            onOpenPairing = {
              navController.navigate(Screen.Chats.route)
            },
            onOpenPipeline = {
              navController.navigate(Screen.Pipeline.route)
            },
            onLogout = {
              com.example.data.service.P2pForegroundService.stop(context)
              viewModel.logout()
            },
            onGetSecurityQuestions = { uid ->
              viewModel.getSecurityQuestions(uid)
            },
            onVerifySecurityAnswers = { uid, a1, a2, a3 ->
              viewModel.verifySecurityAnswers(uid, a1, a2, a3)
            },
            onUpdateLockFormatAndSecret = { uid, lockType, passcode ->
              viewModel.updateLockFormatAndSecret(uid, lockType, passcode)
            },
            onToggleBatterySaver = { enabled ->
              viewModel.toggleBatterySaver(enabled)
            }
          )
        }
      }

      // Dialog Overlay: 9-Step Packet Inspector
      if (inspectingMessage != null) {
        PacketInspectorDialog(
          message = inspectingMessage!!,
          trace = activeTrace,
          onDismiss = { viewModel.closeMessageInspector() }
        )
      }

      // Dialog Overlay: Blinking Radio Alert Popup ("wifi or bluetooth ON kr lo Darling")
      if (showRadioAlertPopup) {
        RadioAlertPopupDialog(
          isBluetoothEnabled = radioState.isBluetoothEnabled,
          isWifiEnabled = radioState.isWifiEnabled,
          onTurnOnBluetooth = { viewModel.turnOnBluetoothInternally() },
          onTurnOnWifi = { viewModel.turnOnWifiInternally() },
          onCheckAgain = { viewModel.checkHardwareRadios() },
          onDismiss = { viewModel.dismissRadioAlertPopup() }
        )
      }

      // Dialog Overlay: Hardware & Runtime Permissions Explanation Dialog
      if (showPermissionsDialog) {
        HardwarePermissionsDialog(
          onGrantPermissions = {
            permissionsLauncher.launch(getRequiredHardwarePermissions())
          },
          onOpenSettings = {
            try {
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
              }
              context.startActivity(intent)
            } catch (_: Exception) {}
          },
          onDismiss = {
            showPermissionsDialog = false
          }
        )
      }

      // Dialog Overlay: Battery & Background Optimization Exemption Dialog
      if (showBatteryDialog) {
        BatteryOptimizationDialog(
          onRequestExemption = {
            viewModel.requestBatteryOptimizationExemption()
          },
          onDismiss = {
            showBatteryDialog = false
          }
        )
      }
    }
  }
}

/**
 * Returns required hardware and connectivity permissions based on the active Android OS version.
 */
private fun getRequiredHardwarePermissions(): Array<String> {
  val permissions = mutableListOf<String>()
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // Android 12+ (API 31+) modern Bluetooth permissions
    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
    permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
  } else {
    // Android 11 and lower legacy location permissions required for BLE scanning
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
  }

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Android 13+ (API 33+) Wi-Fi Direct & Nearby Devices
    permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
  } else {
    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
  }

  // Camera & Audio for QR scanner and voice notes
  permissions.add(Manifest.permission.CAMERA)
  permissions.add(Manifest.permission.RECORD_AUDIO)

  return permissions.toTypedArray()
}

/**
 * Checks if the critical Bluetooth & Wi-Fi Direct permissions have been granted by the user.
 */
private fun hasCriticalConnectivityPermissions(context: Context): Boolean {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    val adv = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    if (!scan || !connect || !adv) return false
  } else {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return false
  }

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val nearby = ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
    if (!nearby) return false
  }

  return true
}

