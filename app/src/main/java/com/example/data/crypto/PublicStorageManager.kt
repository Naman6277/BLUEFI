package com.example.data.crypto

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * PublicStorageManager:
 * Dedicated public storage manager organizing files into designated phone directories:
 * - Documents (.pdf, .docx, .xlsx, .pptx, .py, .apk, .zip) -> Environment.DIRECTORY_DOWNLOADS + "/Bluefy/"
 * - Videos (.mp4, .mkv) -> Environment.DIRECTORY_MOVIES + "/Bluefy/"
 * - Images (.jpg, .png) -> Environment.DIRECTORY_PICTURES + "/Bluefy/"
 * - Audio (.m4a, .mp3) -> Environment.DIRECTORY_MUSIC + "/Bluefy/"
 *
 * Automatically triggers MediaScannerConnection.scanFile() so files appear immediately
 * in File Manager, Gallery, and Downloads with user feedback toasts ("Saved to Downloads/Bluefy!").
 */
object PublicStorageManager {
  private const val TAG = "PublicStorageManager"
  private const val SUBFOLDER = "Bluefy"

  enum class MediaTypeCategory(val folderName: String) {
    DOCUMENT("Downloads"),
    VIDEO("Movies"),
    IMAGE("Pictures"),
    AUDIO("Music")
  }

  fun categorizeExtension(extension: String): MediaTypeCategory {
    return when (extension.lowercase()) {
      "mp4", "mkv", "avi", "3gp", "mov", "webm" -> MediaTypeCategory.VIDEO
      "jpg", "jpeg", "png", "webp", "gif", "bmp" -> MediaTypeCategory.IMAGE
      "m4a", "aac", "mp3", "wav", "ogg", "opus" -> MediaTypeCategory.AUDIO
      else -> MediaTypeCategory.DOCUMENT
    }
  }

  fun getTargetPublicDirectory(context: Context, extension: String): File {
    val category = categorizeExtension(extension)
    val standardDirName = when (category) {
      MediaTypeCategory.VIDEO -> Environment.DIRECTORY_MOVIES
      MediaTypeCategory.IMAGE -> Environment.DIRECTORY_PICTURES
      MediaTypeCategory.AUDIO -> Environment.DIRECTORY_MUSIC
      MediaTypeCategory.DOCUMENT -> Environment.DIRECTORY_DOWNLOADS
    }

    val publicBase = Environment.getExternalStoragePublicDirectory(standardDirName)
    val bluefyDir = File(publicBase, SUBFOLDER)
    if (!bluefyDir.exists()) {
      val created = bluefyDir.mkdirs()
      if (!created && !bluefyDir.exists()) {
        val fallback = File(context.getExternalFilesDir(standardDirName), SUBFOLDER)
        fallback.mkdirs()
        return fallback
      }
    }
    return bluefyDir
  }

  fun saveToPublicStorageAndScan(
    context: Context,
    sourceFile: File?,
    originalFileName: String,
    fileExtension: String = originalFileName.substringAfterLast(".", "bin"),
    showToastNotification: Boolean = true
  ): File? {
    if (sourceFile == null || !sourceFile.exists()) {
      Log.w(TAG, "Cannot save: sourceFile is null or does not exist")
      Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
      return null
    }

    val category = categorizeExtension(fileExtension)
    val targetDir = getTargetPublicDirectory(context, fileExtension)
    val destFile = File(targetDir, originalFileName)

    try {
      if (!targetDir.exists()) targetDir.mkdirs()

      FileInputStream(sourceFile).use { fis ->
        FileOutputStream(destFile).use { fos ->
          fis.copyTo(fos)
          fos.flush()
        }
      }
      Log.i(TAG, "Saved file to public folder: ${destFile.absolutePath} (${destFile.length()} bytes)")

      // Trigger MediaScanner immediately
      MediaScannerConnection.scanFile(
        context.applicationContext,
        arrayOf(destFile.absolutePath),
        null
      ) { path, uri ->
        Log.d(TAG, "MediaScanner registered file: path=$path, uri=$uri")
      }

      if (showToastNotification) {
        Toast.makeText(
          context,
          "Saved to ${category.folderName}/$SUBFOLDER!",
          Toast.LENGTH_SHORT
        ).show()
      }

      return destFile
    } catch (e: Exception) {
      Log.e(TAG, "Failed saving file to public directory: ${e.message}")
      Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
      return null
    }
  }

  fun saveAndScanPublicFile(
    context: Context,
    tempFile: File,
    originalFileName: String,
    onScanned: ((File) -> Unit)? = null
  ): File {
    val extension = originalFileName.substringAfterLast(".", "bin").lowercase()
    val targetDir = getTargetPublicDirectory(context, extension)
    val destFile = File(targetDir, originalFileName)

    if (!targetDir.exists()) targetDir.mkdirs()

    val success = tempFile.renameTo(destFile)
    if (!success) {
      try {
        FileInputStream(tempFile).use { fis ->
          FileOutputStream(destFile).use { fos ->
            fis.copyTo(fos)
            fos.flush()
          }
        }
        tempFile.delete()
      } catch (e: Exception) {
        Log.e(TAG, "Error copying file to public directory: ${e.message}")
      }
    }

    Log.i(TAG, "Saved file to public device directory: ${destFile.absolutePath} (${destFile.length()} bytes)")

    try {
      MediaScannerConnection.scanFile(
        context.applicationContext,
        arrayOf(destFile.absolutePath),
        null
      ) { path, uri ->
        Log.d(TAG, "MediaScanner registered file: path=$path, uri=$uri")
        onScanned?.invoke(destFile)
      }
    } catch (e: Exception) {
      Log.w(TAG, "MediaScanner scanFile failed: ${e.message}")
    }

    return destFile
  }

  fun openFileInExternalApp(context: Context, file: File, mimeType: String = "*/*") {
    MediaFileOpener.openWithChooser(context, file, mimeType, isFullyDownloaded = true)
  }
}
