with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'import androidx.compose.material.icons.filled.Settings',
    'import androidx.compose.material.icons.filled.Person\nimport androidx.compose.material.icons.filled.Settings'
)
text = text.replace(
    'object Settings : Screen("settings", "Settings", Icons.Default.Settings)',
    'object Profile : Screen("profile", "Profile", Icons.Default.Person)\n  object Settings : Screen("settings", "Profile", Icons.Default.Person)'
)
text = text.replace(
    'Screen.Settings\n  )',
    'Screen.Profile\n  )'
)
text = text.replace(
    'composable(Screen.Settings.route) {',
    '''composable(Screen.Profile.route) {
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
              viewModel.logout()
            }
          )
        }

        composable(Screen.Settings.route) {'''
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

print('Updated MainActivity.kt navigation successfully!')
