package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object BluefiNotificationManager {

  private const val CHANNEL_ID = "bluefi_offline_messages"
  private const val CHANNEL_NAME = "Bluefi Offline P2P Messages"
  private const val CHANNEL_DESC = "Notifications for incoming encrypted messages via Bluetooth & Wi-Fi mesh"

  var isAppInForeground: Boolean = false
  var activeConversationPeerId: String? = null

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        enableLights(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun showIncomingMessageNotification(
    context: Context,
    conversationId: String,
    senderId: String,
    senderName: String,
    messageText: String,
    mediaType: String = "text"
  ) {
    // If user is actively chatting with this peer right now inside the app, do not spam system notifications
    if (isAppInForeground && activeConversationPeerId == conversationId) {
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val previewContent = when (mediaType) {
      "image" -> "📷 Sent an encrypted photo"
      "video" -> "🎥 Sent an encrypted video"
      "audio" -> "🎙️ Sent an encrypted voice note"
      "sticker" -> "Sticker: $messageText"
      else -> messageText.ifBlank { "🔐 Encrypted 2.4 GHz packet" }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("deep_link_peer_id", conversationId)
    }

    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      conversationId.hashCode(),
      intent,
      pendingIntentFlags
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("💬 $senderName (Bluefi)")
      .setContentText(previewContent)
      .setStyle(NotificationCompat.BigTextStyle().bigText(previewContent))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .build()

    try {
      val notificationId = (conversationId.hashCode() and 0x7FFFFFFF)
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Notification permission not granted
    } catch (_: Exception) {}
  }

  fun clearNotificationForPeer(context: Context, conversationId: String) {
    try {
      val notificationId = (conversationId.hashCode() and 0x7FFFFFFF)
      NotificationManagerCompat.from(context).cancel(notificationId)
    } catch (_: Exception) {}
  }
}
