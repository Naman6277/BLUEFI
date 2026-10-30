package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * ApkShareManager: Extracts the local application base APK from sourceDir,
 * copies it to the shared cache as `Bluefy2.0_Offline.apk`, creates a FileProvider URI,
 * and launches native Bluetooth / Nearby Share intent sheet for zero-internet distribution.
 */
object ApkShareManager {
  private const val TAG = "ApkShareManager"
  private const val SHARED_APK_NAME = "Bluefy2.0_Offline.apk"

  fun shareNativeApk(context: Context) {
    try {
      val sourceApkPath = context.applicationInfo.sourceDir
      if (sourceApkPath.isNullOrBlank()) {
        Toast.makeText(context, "Cannot locate base APK source", Toast.LENGTH_SHORT).show()
        return
      }

      val sourceFile = File(sourceApkPath)
      if (!sourceFile.exists()) {
        Toast.makeText(context, "Base APK file not found on device", Toast.LENGTH_SHORT).show()
        return
      }

      // Copy to app's cache directory or external files directory for sharing
      val cacheDir = context.externalCacheDir ?: context.cacheDir
      val targetApk = File(cacheDir, SHARED_APK_NAME)

      FileInputStream(sourceFile).use { input ->
        FileOutputStream(targetApk).use { output ->
          input.copyTo(output)
          output.flush()
        }
      }

      val apkUri: Uri = try {
        FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          targetApk
        )
      } catch (e: Exception) {
        Log.e(TAG, "FileProvider generation failed, falling back to file uri: ${e.message}")
        Uri.fromFile(targetApk)
      }

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.android.package-archive"
        putExtra(Intent.EXTRA_STREAM, apkUri)
        putExtra(Intent.EXTRA_SUBJECT, "Bluefy 2.0 Offline Mesh APK")
        putExtra(Intent.EXTRA_TEXT, "Install Bluefy 2.0 to chat offline over 2.4 GHz Bluetooth & Wi-Fi Direct mesh without internet.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(shareIntent, "Share Bluefy 2.0 APK via Bluetooth / Nearby Share").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to share APK: ${e.message}")
      Toast.makeText(context, "Error sharing APK: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }
}
