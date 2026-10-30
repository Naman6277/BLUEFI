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
 * Foreground Service for Connected Devices (android:foregroundServiceType="connectedDevice").
 * Strictly required on modern Android (API 34 to 37) to maintain background socket connections
 * while enforcing aggressive power-efficiency features:
 *
 * 1. onTaskRemoved: Shuts down all sockets and removes notification when swiped away from Recents.
 * 2. Notification Action: Single-tap "Disconnect & Stop" action to terminate sockets immediately.
 * 3. 15-Minute Idle Timeout: Automatically disconnects sockets and stops the service if no message
 *    activity occurs for 15 minutes.
 */
class BluefiMeshService : Service() {

  companion object {
    private const val TAG = "BluefiMeshService"
    const val CHANNEL_ID = "bluefi_mesh_foreground_channel"
    const val CHANNEL_NAME = "Bluefi Mesh Socket Service"
    const val NOTIFICATION_ID = 24201

    const val ACTION_START = "com.example.bluefi.ACTION_START_MESH_SERVICE"
    const val ACTION_STOP = "com.example.bluefi.ACTION_STOP_MESH_SERVICE"
    const val ACTION_UPDATE_STATUS = "com.example.bluefi.ACTION_UPDATE_STATUS"
    const val ACTION_RESET_IDLE = "com.example.bluefi.ACTION_RESET_IDLE"
    const val EXTRA_STATUS_TEXT = "extra_status_text"

    // 15-minute idle timeout to eliminate background battery drain
    const val IDLE_TIMEOUT_MS = 15 * 60 * 1000L

    /**
     * Starts the Foreground Service safely across all Android versions (Android 6.0 to 17).
     */
    fun start(context: Context, statusText: String = "2.4 GHz RF Carrier Active • P2P Mesh") {
      try {
        val intent = Intent(context, BluefiMeshService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_STATUS_TEXT, statusText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(context, intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to start BluefiMeshService: ${e.message}")
      }
    }

    /**
     * Resets the 15-minute idle timer whenever socket messages are sent or received.
     */
    fun notifyActivity(context: Context) {
      try {
        val intent = Intent(context, BluefiMeshService::class.java).apply {
          action = ACTION_RESET_IDLE
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.d(TAG, "Notice sending reset idle to service: ${e.message}")
      }
    }

    /**
     * Updates the status message in the persistent foreground notification.
     */
    fun updateStatus(context: Context, statusText: String) {
      try {
        val intent = Intent(context, BluefiMeshService::class.java).apply {
          action = ACTION_UPDATE_STATUS
          putExtra(EXTRA_STATUS_TEXT, statusText)
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to update BluefiMeshService status: ${e.message}")
      }
    }

    /**
     * Stops the foreground service and closes all sockets.
     */
    fun stop(context: Context) {
      try {
        val intent = Intent(context, BluefiMeshService::class.java).apply {
          action = ACTION_STOP
        }
        context.startService(intent)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to stop BluefiMeshService: ${e.message}")
      }
    }
  }

  private val serviceScope = CoroutineScope(Dispatchers.IO)
  private var isRunning = false
  private var currentStatus: String = "2.4 GHz RF Carrier Active • P2P Mesh"
  private var idleJob: Job? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: ACTION_START

    when (action) {
      ACTION_STOP -> {
        Log.i(TAG, "ACTION_STOP received: User requested socket disconnection and service termination")
        cleanupAndShutdownSockets()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning = false
        return START_NOT_STICKY
      }

      ACTION_RESET_IDLE -> {
        resetIdleTimer()
      }

      ACTION_UPDATE_STATUS -> {
        currentStatus = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: currentStatus
        if (isRunning) {
          val notification = buildForegroundNotification(currentStatus)
          val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
          nm?.notify(NOTIFICATION_ID, notification)
        }
        resetIdleTimer()
      }

      ACTION_START -> {
        currentStatus = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: currentStatus
        startForegroundWithConnectedDeviceType(currentStatus)
        isRunning = true
        resetIdleTimer()
      }
    }

    return START_STICKY
  }

  /**
   * Resets the 15-minute inactivity idle timer.
   * If no messages are transmitted or received within 15 minutes, sockets and service are torn down.
   */
  private fun resetIdleTimer() {
    idleJob?.cancel()
    idleJob = serviceScope.launch {
      delay(IDLE_TIMEOUT_MS)
      Log.i(TAG, "Idle timeout reached (15 minutes of inactivity). Automatically terminating service and closing sockets to prevent battery drain.")
      cleanupAndShutdownSockets()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  /**
   * Recent Apps Cleanup:
   * When the user swipes Bluefi away from the Recent Apps screen, Android invokes onTaskRemoved().
   * Closes all active Bluetooth RFCOMM and Wi-Fi Direct sockets, cancels reconnect loops,
   * removes the persistent notification, and stops the service.
   */
  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    Log.i(TAG, "onTaskRemoved triggered: App swiped away from Recents screen. Cleaning up all sockets and stopping service.")
    cleanupAndShutdownSockets()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun cleanupAndShutdownSockets() {
    idleJob?.cancel()
    idleJob = null
    try {
      val app = applicationContext as? BluefiApplication
      app?.repository?.shutdownAllSockets()
    } catch (e: Exception) {
      Log.w(TAG, "Error in cleanupAndShutdownSockets: ${e.message}")
    }
  }

  private fun startForegroundWithConnectedDeviceType(status: String) {
    val notification = buildForegroundNotification(status)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // Android 14+ (API 34 to 37): Connected device foreground service type
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
      Log.i(TAG, "BluefiMeshService running in foreground (connectedDevice)")
    } catch (e: SecurityException) {
      Log.e(TAG, "SecurityException starting foreground service: ${e.message}")
      try {
        startForeground(NOTIFICATION_ID, notification)
      } catch (e2: Exception) {
        Log.e(TAG, "Fallback startForeground failed: ${e2.message}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting foreground service: ${e.message}")
    }
  }

  /**
   * Builds the persistent notification with a single-tap "Disconnect & Stop" action button.
   */
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

    // Action button: Disconnect & Stop
    val stopIntent = Intent(this, BluefiMeshService::class.java).apply {
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
        description = "Keeps Bluetooth and Wi-Fi Direct chat sockets alive in the background"
        setShowBadge(false)
      }
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.createNotificationChannel(channel)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    cleanupAndShutdownSockets()
    isRunning = false
    Log.i(TAG, "BluefiMeshService destroyed")
  }
}
