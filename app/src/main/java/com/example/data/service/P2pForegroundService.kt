package com.example.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.BluefiApplication
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * P2pForegroundService (formerly BluefiMeshService):
 * Background Socket Persistence & 1-Minute Idle Disconnect Timer.
 *
 * 1. Keeps background sockets active when user navigates to Home Screen using
 *    non-dismissable notification (setOngoing(true)).
 * 2. 1-Minute Idle Timer in Background:
 *    When app is in background (onStop()), monitors socket message traffic.
 *    If NO messages sent/received for 60 SECONDS while in background, temporarily pauses
 *    socket background polling to conserve battery.
 * 3. Auto-Resume on App Launch:
 *    When user opens/launches Bluefy app (onResume()), immediately re-activates
 *    service and resumes full P2P socket communication.
 */
class P2pForegroundService : Service() {

  companion object {
    private const val TAG = "P2pForegroundService"
    const val CHANNEL_ID = "p2p_foreground_service_channel"
    const val CHANNEL_NAME = "Bluefi P2P Mesh Communication"
    const val NOTIFICATION_ID = 24201

    const val ACTION_START = "com.example.bluefi.ACTION_START_P2P_SERVICE"
    const val ACTION_STOP = "com.example.bluefi.ACTION_STOP_P2P_SERVICE"
    const val ACTION_UPDATE_STATUS = "com.example.bluefi.ACTION_UPDATE_P2P_STATUS"
    const val ACTION_RESET_IDLE = "com.example.bluefi.ACTION_RESET_P2P_IDLE"
    const val ACTION_APP_BACKGROUNDED = "com.example.bluefi.ACTION_APP_BACKGROUNDED"
    const val ACTION_APP_FOREGROUNDED = "com.example.bluefi.ACTION_APP_FOREGROUNDED"

    const val EXTRA_STATUS_TEXT = "extra_status_text"

    // 60-second idle disconnect timer in background to conserve battery
    const val BACKGROUND_IDLE_TIMEOUT_MS = 60 * 1000L

    var isRunning = false
      private set

    var isBatterySaverMode = false

    var isAppInBackground = false
      private set

    var isPollingPaused = false
      private set

    fun start(context: Context, statusText: String = "2.4 GHz P2P Mesh Active • Zero Cloud") {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_STATUS_TEXT, statusText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(context, intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to start P2pForegroundService: ${e.message}")
      }
    }

    fun notifyActivity(context: Context) {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_RESET_IDLE
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to notify activity in P2pForegroundService: ${e.message}")
      }
    }

    fun onAppBackgrounded(context: Context) {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_APP_BACKGROUNDED
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to send ACTION_APP_BACKGROUNDED: ${e.message}")
      }
    }

    fun onAppForegrounded(context: Context) {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_APP_FOREGROUNDED
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to send ACTION_APP_FOREGROUNDED: ${e.message}")
      }
    }

    fun updateStatus(context: Context, statusText: String) {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_UPDATE_STATUS
          putExtra(EXTRA_STATUS_TEXT, statusText)
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to update status in P2pForegroundService: ${e.message}")
      }
    }

    fun stop(context: Context) {
      try {
        val intent = Intent(context, P2pForegroundService::class.java).apply {
          action = ACTION_STOP
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to stop P2pForegroundService: ${e.message}")
      }
    }
  }

  private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
  private var backgroundIdleJob: Job? = null
  private var currentStatus: String = "2.4 GHz P2P Mesh Active • Zero Cloud"

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: ACTION_START
    when (action) {
      ACTION_STOP -> {
        Log.i(TAG, "ACTION_STOP: Disconnecting sockets and stopping foreground service")
        cleanupAndShutdownSockets()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning = false
        return START_NOT_STICKY
      }
      ACTION_APP_BACKGROUNDED -> {
        isAppInBackground = true
        Log.d(TAG, "App in background: Started 60-second idle timer")
        startBackgroundIdleCountdown()
      }
      ACTION_APP_FOREGROUNDED -> {
        isAppInBackground = false
        backgroundIdleJob?.cancel()
        if (isPollingPaused) {
          Log.i(TAG, "App foregrounded: Resuming P2P socket polling immediately")
          resumeSocketPolling()
        }
        updateNotificationStatus("2.4 GHz P2P Mesh Active • Connected")
      }
      ACTION_RESET_IDLE -> {
        if (isAppInBackground) {
          startBackgroundIdleCountdown()
        }
      }
      ACTION_UPDATE_STATUS -> {
        currentStatus = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: currentStatus
        updateNotificationStatus(currentStatus)
      }
      ACTION_START -> {
        currentStatus = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: currentStatus
        startForegroundWithConnectedDeviceType(currentStatus)
        isRunning = true
      }
    }
    return START_STICKY
  }

  /**
   * 1-Minute Idle Timer in Background:
   * If no messages are sent or received for 60 seconds while app is in background,
   * temporarily pauses socket background polling to conserve battery.
   */
  private fun startBackgroundIdleCountdown() {
    backgroundIdleJob?.cancel()
    backgroundIdleJob = serviceScope.launch {
      val timeoutMs = if (isBatterySaverMode) 30 * 1000L else BACKGROUND_IDLE_TIMEOUT_MS
      delay(timeoutMs)
      if (isAppInBackground) {
        Log.i(TAG, "Background inactivity reached (${timeoutMs / 1000}s). Pausing socket background polling to save battery.")
        pauseSocketPolling()
        updateNotificationStatus("P2P Mesh Idle (Battery Saver Active) • Opens instantly on launch")
      }
    }
  }

  private fun pauseSocketPolling() {
    isPollingPaused = true
    try {
      val app = applicationContext as? BluefiApplication
      // Repository pauses background beacon/scan broadcasts while keeping connection sockets open
      app?.repository?.pauseBackgroundBeaconing()
    } catch (e: Exception) {
      Log.w(TAG, "Error pausing polling: ${e.message}")
    }
  }

  private fun resumeSocketPolling() {
    isPollingPaused = false
    try {
      val app = applicationContext as? BluefiApplication
      app?.repository?.resumeBackgroundBeaconing()
      app?.repository?.startAutoHardwareDiscovery()
    } catch (e: Exception) {
      Log.w(TAG, "Error resuming polling: ${e.message}")
    }
  }

  private fun updateNotificationStatus(text: String) {
    currentStatus = text
    if (isRunning) {
      val notification = buildForegroundNotification(currentStatus)
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.notify(NOTIFICATION_ID, notification)
    }
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    Log.i(TAG, "onTaskRemoved(): Bluefi was swiped away from Recent Apps. Cleaning up sockets.")
    cleanupAndShutdownSockets()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    isRunning = false
  }

  private fun cleanupAndShutdownSockets() {
    try {
      val app = applicationContext as? BluefiApplication
      app?.repository?.shutdownAllSockets()
    } catch (e: Exception) {
      Log.e(TAG, "Error during cleanupAndShutdownSockets: ${e.message}")
    }
  }

  private fun startForegroundWithConnectedDeviceType(status: String) {
    val notification = buildForegroundNotification(status)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }
        startForeground(NOTIFICATION_ID, notification, fgsType)
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (e: Exception) {
      Log.w(TAG, "startForeground with type failed, falling back: ${e.message}")
      try {
        startForeground(NOTIFICATION_ID, notification)
      } catch (e2: Exception) {
        Log.e(TAG, "Fatal fallback startForeground failed: ${e2.message}")
      }
    }
  }

  private fun buildForegroundNotification(statusText: String): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingOpenIntent = PendingIntent.getActivity(
      this,
      0,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, P2pForegroundService::class.java).apply {
      action = ACTION_STOP
    }
    val pendingStopIntent = PendingIntent.getService(
      this,
      1,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Bluefi Offline P2P Mesh")
      .setContentText(statusText)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setContentIntent(pendingOpenIntent)
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Disconnect & Stop",
        pendingStopIntent
      )
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Keeps peer-to-peer radio mesh sockets alive in background"
        setShowBadge(false)
      }
      val nm = getSystemService(NotificationManager::class.java)
      nm?.createNotificationChannel(channel)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    isRunning = false
    Log.d(TAG, "P2pForegroundService destroyed")
  }
}
