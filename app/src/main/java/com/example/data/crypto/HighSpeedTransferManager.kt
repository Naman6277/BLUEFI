package com.example.data.crypto

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * HighSpeedTransferManager:
 * High-Efficiency Buffered Download & Stream Pipeline.
 *
 * 1. Socket Buffer Optimization:
 *    Uses 256 KB high-throughput memory buffers for all file streaming operations:
 *    BufferedInputStream(inputStream, 256 * 1024)
 *    BufferedOutputStream(outputStream, 256 * 1024)
 *
 * 2. Throttled UI Progress Updates:
 *    Limits UI progress bar updates to at most once every 100ms or on 5% progress
 *    milestones, preventing Main Thread jank and frame drops during fast P2P transfers.
 *
 * 3. Background Public Storage Saving:
 *    Executes file copying and MediaScannerConnection.scanFile() on Dispatchers.IO,
 *    delivering zero-latency toasts and ensuring active chat messaging is never blocked.
 */
object HighSpeedTransferManager {
  private const val TAG = "HighSpeedTransfer"
  const val STREAM_BUFFER_SIZE = 256 * 1024 // 256 KB memory buffer for maximum throughput

  // Tracking timestamps and progress levels for 100ms / 5% throttled UI updates
  private val lastUiUpdateTimes = ConcurrentHashMap<String, Long>()
  private val lastUiUpdatePercents = ConcurrentHashMap<String, Int>()

  /**
   * Determines if a UI progress update should be dispatched.
   * Dispatches if:
   * - 100% complete OR
   * - At least 100ms has elapsed since the last UI update OR
   * - Progress has jumped by >= 5%
   */
  fun shouldUpdateUi(transferId: String, currentPercent: Int): Boolean {
    val now = System.currentTimeMillis()
    val lastTime = lastUiUpdateTimes[transferId] ?: 0L
    val lastPercent = lastUiUpdatePercents[transferId] ?: 0

    if (currentPercent >= 100 || (now - lastTime >= 100) || (currentPercent - lastPercent >= 5)) {
      lastUiUpdateTimes[transferId] = now
      lastUiUpdatePercents[transferId] = currentPercent
      return true
    }
    return false
  }

  fun clearTransferTracking(transferId: String) {
    lastUiUpdateTimes.remove(transferId)
    lastUiUpdatePercents.remove(transferId)
  }

  /**
   * High-speed buffered stream copy with 256 KB buffer and throttled progress callbacks.
   */
  suspend fun copyStreamBuffered(
    transferId: String,
    input: InputStream,
    output: OutputStream,
    totalBytes: Long,
    onProgress: (percent: Int, bytesRead: Long) -> Unit
  ): Long = withContext(Dispatchers.IO) {
    val bufferedIn = if (input is BufferedInputStream) input else BufferedInputStream(input, STREAM_BUFFER_SIZE)
    val bufferedOut = if (output is BufferedOutputStream) output else BufferedOutputStream(output, STREAM_BUFFER_SIZE)

    val buffer = ByteArray(STREAM_BUFFER_SIZE)
    var totalRead = 0L
    var bytes: Int

    while (bufferedIn.read(buffer).also { bytes = it } != -1) {
      bufferedOut.write(buffer, 0, bytes)
      totalRead += bytes

      if (totalBytes > 0) {
        val currentPercent = ((totalRead.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
        if (shouldUpdateUi(transferId, currentPercent)) {
          withContext(Dispatchers.Main) {
            onProgress(currentPercent, totalRead)
          }
        }
      }
    }

    bufferedOut.flush()
    withContext(Dispatchers.Main) {
      onProgress(100, totalRead)
    }
    clearTransferTracking(transferId)
    totalRead
  }

  /**
   * Asynchronous Background Public Saving:
   * Copies file to public directory and triggers MediaScannerConnection on Dispatchers.IO.
   */
  fun saveToPublicStorageAndScanAsync(
    context: Context,
    sourceFile: File?,
    originalFileName: String,
    fileExtension: String = originalFileName.substringAfterLast(".", "bin"),
    onComplete: ((File?) -> Unit)? = null
  ) {
    if (sourceFile == null || !sourceFile.exists()) {
      Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
      onComplete?.invoke(null)
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      val savedFile = try {
        val category = PublicStorageManager.categorizeExtension(fileExtension)
        val targetDir = PublicStorageManager.getTargetPublicDirectory(context, fileExtension)
        val destFile = File(targetDir, originalFileName)

        if (!targetDir.exists()) targetDir.mkdirs()

        // 256 KB buffered disk-to-disk copy
        BufferedInputStream(FileInputStream(sourceFile), STREAM_BUFFER_SIZE).use { bis ->
          BufferedOutputStream(FileOutputStream(destFile), STREAM_BUFFER_SIZE).use { bos ->
            val buf = ByteArray(STREAM_BUFFER_SIZE)
            var n: Int
            while (bis.read(buf).also { n = it } != -1) {
              bos.write(buf, 0, n)
            }
            bos.flush()
          }
        }

        // Asynchronous MediaScanner indexing
        MediaScannerConnection.scanFile(
          context.applicationContext,
          arrayOf(destFile.absolutePath),
          null
        ) { path, uri ->
          Log.d(TAG, "Asynchronously registered file in MediaStore: path=$path, uri=$uri")
        }

        withContext(Dispatchers.Main) {
          Toast.makeText(
            context,
            "Saved to ${category.folderName}/Bluefy! 📁",
            Toast.LENGTH_SHORT
          ).show()
        }

        destFile
      } catch (e: Exception) {
        Log.e(TAG, "Background save failed: ${e.message}")
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        null
      }

      withContext(Dispatchers.Main) {
        onComplete?.invoke(savedFile)
      }
    }
  }
}
