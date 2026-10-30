package com.example.data.crypto

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.BluefiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * P2pReceiverProgressState:
 * Tracks guaranteed 1% -> 100% receiver reassembly progress for files and media,
 * preventing 0% stuck progress states.
 */
data class P2pReceiverProgressState(
  val transferId: String,
  val fileName: String,
  val extension: String,
  val totalBytes: Long,
  val totalBytesReceived: Long,
  val currentChunk: Int,
  val totalChunks: Int,
  val progressPercent: Int,
  val statusText: String,
  val isComplete: Boolean,
  val localFile: File? = null,
  val contentUri: Uri? = null,
  val mimeType: String = "*/*"
)

/**
 * P2pChunkedReceiver:
 * Streamed P2P Chunk Receiver Engine with Defensive Socket Metadata & 0% Stall Prevention.
 *
 * 1. Valid Metadata Header Verification:
 *    - Reads packet header prior to reading payload bytes.
 *    - Validates total file length (`totalBytes > 0`).
 * 2. Immediate Initial Progress Emission (1% Bootstrap):
 *    - Emits progress = 1% immediately upon stream initiation / first chunk.
 * 3. Safe Floating-Point Progress Calculation:
 *    - Uses double precision calculation `((totalBytesReceived.toDouble() / totalBytes.toDouble()) * 100)`.
 *    - Enforces 100ms throttling between UI updates to prevent thread contention.
 * 4. Stream Completion Enforcement:
 *    - Explicitly flushes and closes streams on Dispatchers.IO.
 *    - Updates local Room DB record to status = DELIVERED.
 */
object P2pChunkedReceiver {
  private const val TAG = "P2pChunkedReceiver"

  private val _receiverProgressMap = ConcurrentHashMap<String, MutableStateFlow<P2pReceiverProgressState>>()

  fun getReceiverProgressFlow(transferId: String, fileName: String = "file"): StateFlow<P2pReceiverProgressState> {
    return _receiverProgressMap.getOrPut(transferId) {
      MutableStateFlow(
        P2pReceiverProgressState(
          transferId = transferId,
          fileName = fileName,
          extension = fileName.substringAfterLast(".", "bin"),
          totalBytes = 0L,
          totalBytesReceived = 0L,
          currentChunk = 0,
          totalChunks = 1,
          progressPercent = 1, // 1% Bootstrap to prevent 0% UI hang
          statusText = "Loading... 1%",
          isComplete = false
        )
      )
    }.asStateFlow()
  }

  fun updateReceiverProgress(transferId: String, state: P2pReceiverProgressState) {
    val flow = _receiverProgressMap.getOrPut(transferId) { MutableStateFlow(state) }
    flow.value = state

    // Synchronize to SystemProgressManager & UniversalFileTransferManager
    SystemProgressManager.reportProgress(
      transferId = transferId,
      percentage = state.progressPercent,
      statusText = state.statusText,
      forceUpdate = state.isComplete || state.progressPercent == 1
    )

    UniversalFileTransferManager.updateProgress(
      transferId,
      FileTransferProgressState(
        transferId = state.transferId,
        isSender = false,
        fileName = state.fileName,
        extension = state.extension,
        fileSizeBytes = state.totalBytes,
        currentChunk = state.currentChunk,
        totalChunks = state.totalChunks,
        progressPercent = state.progressPercent,
        statusText = state.statusText,
        isComplete = state.isComplete,
        localFile = state.localFile,
        contentUri = state.contentUri,
        mimeType = state.mimeType
      )
    )
  }

  fun getLatestState(transferId: String): P2pReceiverProgressState? {
    return _receiverProgressMap[transferId]?.value
  }

  /**
   * Defensive socket stream reader preventing 0% stuck progress:
   * 1. Reads packet header prior to reading payload bytes.
   * 2. Bootstraps to 1% immediately.
   * 3. Calculates guarded floating-point progress (1% to 100%).
   * 4. Updates UI every 100ms or on completion.
   */
  suspend fun receiveStreamWithGuardedProgress(
    socketInput: DataInputStream,
    destinationFile: File,
    onProgress: (Int) -> Unit
  ) = withContext(Dispatchers.IO) {
    // 1. Read total file length first
    val totalBytes = try {
      socketInput.readLong()
    } catch (e: Exception) {
      Log.e(TAG, "Failed reading totalBytes header: ${e.message}")
      -1L
    }

    if (totalBytes <= 0L) {
      Log.w(TAG, "Invalid totalBytes ($totalBytes). Emitting 1% fallback.")
      withContext(Dispatchers.Main) {
        onProgress(1) // Fallback to 1% while calculating size
      }
    } else {
      // 2. Immediate Initial Progress Emission (1% Bootstrap)
      withContext(Dispatchers.Main) {
        onProgress(1)
      }
    }

    val fileOut = FileOutputStream(destinationFile)
    val bufferedOut = BufferedOutputStream(fileOut, 64 * 1024)
    var totalBytesReceived = 0L
    var lastUiUpdateMs = 0L

    val buffer = ByteArray(64 * 1024)
    var bytesRead = 0

    try {
      while ((totalBytes <= 0L || totalBytesReceived < totalBytes) &&
        socketInput.read(buffer).also { bytesRead = it } != -1
      ) {
        bufferedOut.write(buffer, 0, bytesRead)
        totalBytesReceived += bytesRead

        val currentTime = System.currentTimeMillis()
        // Force update UI every 100ms or when complete
        if (currentTime - lastUiUpdateMs > 100 || (totalBytes > 0L && totalBytesReceived >= totalBytes)) {
          lastUiUpdateMs = currentTime

          // Safe Floating-Point Progress Calculation (1% to 100%)
          val calcProgress = if (totalBytes > 0L) {
            ((totalBytesReceived.toDouble() / totalBytes.toDouble()) * 100.0)
              .coerceIn(1.0, 100.0)
              .toInt()
          } else 1

          withContext(Dispatchers.Main) {
            onProgress(calcProgress)
          }
        }
      }

      bufferedOut.flush()
    } finally {
      bufferedOut.close()
    }

    withContext(Dispatchers.Main) {
      onProgress(100)
    }
  }

  /**
   * Reassembles chunks into a temporary file with a 256KB buffer, smoothly reports progress
   * from 1% to 100%, flushes and closes streams on Dispatchers.IO, and registers the file.
   */
  suspend fun receiveChunkedStream(
    context: Context,
    messageId: String,
    header: FileHeaderPacket,
    chunkReceiverBlock: suspend (onChunk: suspend (chunkIndex: Int, chunkData: ByteArray) -> Unit) -> Unit
  ): File? = withContext(Dispatchers.IO) {
    try {
      val tempDir = context.cacheDir
      val tempFile = File(tempDir, "p2p_chunk_recv_${header.fileUuid}.tmp")
      var totalBytesReceived = 0L
      var receivedChunks = 0

      // 1% Initial Bootstrap: Notify UI immediately that transfer started
      SystemProgressManager.bootstrapProgress(messageId)
      updateReceiverProgress(
        messageId,
        P2pReceiverProgressState(
          transferId = messageId,
          fileName = header.fileName,
          extension = header.extension,
          totalBytes = header.fileSizeBytes,
          totalBytesReceived = 0L,
          currentChunk = 0,
          totalChunks = header.totalChunks,
          progressPercent = 1,
          statusText = "Loading... 1%",
          isComplete = false,
          mimeType = header.mimeType
        )
      )

      val fos = FileOutputStream(tempFile)
      val bufferedOutputStream = BufferedOutputStream(fos, HighSpeedTransferManager.STREAM_BUFFER_SIZE)
      var lastUiUpdateMs = System.currentTimeMillis()

      try {
        chunkReceiverBlock { _, chunkData ->
          bufferedOutputStream.write(chunkData)
          totalBytesReceived += chunkData.size
          receivedChunks++

          val currentTime = System.currentTimeMillis()
          // Safe Floating-Point Progress Calculation (1% to 99%)
          val progress = if (header.fileSizeBytes > 0) {
            ((totalBytesReceived.toDouble() / header.fileSizeBytes.toDouble()) * 100.0)
              .coerceIn(1.0, 99.0)
              .toInt()
          } else {
            ((receivedChunks.toDouble() / header.totalChunks.toDouble()) * 100.0)
              .coerceIn(1.0, 99.0)
              .toInt()
          }

          if (currentTime - lastUiUpdateMs > 100 || receivedChunks == header.totalChunks) {
            lastUiUpdateMs = currentTime
            updateReceiverProgress(
              messageId,
              P2pReceiverProgressState(
                transferId = messageId,
                fileName = header.fileName,
                extension = header.extension,
                totalBytes = header.fileSizeBytes,
                totalBytesReceived = totalBytesReceived,
                currentChunk = receivedChunks,
                totalChunks = header.totalChunks,
                progressPercent = progress,
                statusText = "Loading... $progress%",
                isComplete = false,
                mimeType = header.mimeType
              )
            )
          }
        }
      } finally {
        // Enforce explicit flush and close on Dispatchers.IO
        bufferedOutputStream.flush()
        bufferedOutputStream.close()
      }

      // Promote reassembled file to public storage
      val finalPublicFile = PublicStorageManager.saveAndScanPublicFile(context, tempFile, header.fileName)

      val contentUri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalPublicFile)
      } catch (_: Exception) {
        Uri.fromFile(finalPublicFile)
      }

      // Guaranteed 100% emission with "Complete"
      SystemProgressManager.markComplete(messageId)
      updateReceiverProgress(
        messageId,
        P2pReceiverProgressState(
          transferId = messageId,
          fileName = header.fileName,
          extension = header.extension,
          totalBytes = header.fileSizeBytes,
          totalBytesReceived = header.fileSizeBytes,
          currentChunk = header.totalChunks,
          totalChunks = header.totalChunks,
          progressPercent = 100,
          statusText = "Complete",
          isComplete = true,
          localFile = finalPublicFile,
          contentUri = contentUri,
          mimeType = header.mimeType
        )
      )

      // Update local Room DB entity state to completed
      try {
        val db = BluefiDatabase.getDatabase(context)
        val existing = db.messageDao().getMessageById(messageId)
        if (existing != null) {
          val updatedMeta = "file_meta|${header.fileName}|${header.extension}|${header.fileSizeBytes}|${UniversalFileTransferManager.formatFileSize(header.fileSizeBytes)}|${finalPublicFile.absolutePath}|${header.mimeType}"
          db.messageDao().updateMessage(
            existing.copy(
              status = "DELIVERED",
              mediaContent = updatedMeta
            )
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Room DB message update skipped for $messageId: ${e.message}")
      }

      finalPublicFile
    } catch (e: Exception) {
      Log.e(TAG, "Error in receiveChunkedStream: ${e.message}", e)
      null
    }
  }
}
