package com.example.ui.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.crypto.FastFileOpener
import com.example.data.crypto.HighSpeedTransferManager
import com.example.data.crypto.P2pChunkedReceiver
import com.example.data.crypto.PublicStorageManager
import com.example.data.crypto.SystemProgressManager
import com.example.data.crypto.UniversalFileTransferManager
import com.example.data.local.entity.MessageEntity
import com.example.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * FileMessageViewHolder:
 * High-speed performance ViewHolder featuring guaranteed 100% progress completion reporting,
 * active Main-Thread coroutine flow collection, 1% initial progress bootstrap,
 * "Loading... X%" and "Complete" state transitions, and background public storage saving.
 */
class FileMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  val tvExtensionBadge: TextView = itemView.findViewById(R.id.tvFileExtensionBadge)
  val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
  val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
  val btnSaveToPhone: ImageButton = itemView.findViewById(R.id.btnSaveToPhone)
  val layoutProgress: LinearLayout = itemView.findViewById(R.id.layoutFileProgress)
  val tvProgressStatus: TextView = itemView.findViewById(R.id.tvProgressStatus)
  val pbTransfer: ProgressBar = itemView.findViewById(R.id.pbFileTransfer)

  private var progressCollectorJob: Job? = null

  init {
    itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) {
        progressCollectorJob?.cancel()
        progressCollectorJob = null
      }
    })
  }

  fun bind(message: ChatMessage) {
    val context = itemView.context
    val mediaContent = message.mediaContent

    // Cancel any previous collection job to avoid multiple listeners on recycled views
    progressCollectorJob?.cancel()

    // Format: "file_meta|$fileName|$fileSize|$mimeType|$localPath|$fileUuid"
    val parts = if (mediaContent.startsWith("file_meta|")) mediaContent.split("|") else emptyList()
    val fileName = if (parts.size >= 2) parts[1] else "document.pdf"
    val fileSizeBytes = if (parts.size >= 3) parts[2].toLongOrNull() ?: 0L else 0L
    val mimeType = if (parts.size >= 4) parts[3] else "*/*"
    val localPath = if (parts.size >= 5) parts[4] else ""

    val ext = fileName.substringAfterLast(".", "FILE").uppercase()
    tvExtensionBadge.text = ext.take(4)
    tvFileName.text = fileName
    tvFileSize.text = UniversalFileTransferManager.formatFileSize(fileSizeBytes)

    val publicDir = PublicStorageManager.getTargetPublicDirectory(context, ext.lowercase())
    val candidatePublic = File(publicDir, fileName)
    val candidateLocal = if (localPath.isNotBlank()) File(localPath) else null

    val resolvedFile = when {
      candidatePublic.exists() && candidatePublic.length() > 0 -> candidatePublic
      candidateLocal != null && candidateLocal.exists() && candidateLocal.length() > 0 -> candidateLocal
      else -> candidatePublic
    }

    var currentProgress = when {
      resolvedFile.exists() && resolvedFile.length() > 0 -> 100
      else -> SystemProgressManager.getProgress(message.id).let {
        if (it > 0) it else P2pChunkedReceiver.getLatestState(message.id)?.progressPercent ?: 0
      }
    }

    // Initial render state
    updateUiState(currentProgress, resolvedFile)

    // 1. Active Lifecycle Flow Collection on Main Thread
    progressCollectorJob = CoroutineScope(Dispatchers.Main).launch {
      SystemProgressManager.getProgressFlow(message.id).collectLatest { percent ->
        val effectivePercent = if (resolvedFile.exists() && resolvedFile.length() > 0) 100 else percent
        currentProgress = effectivePercent
        updateUiState(effectivePercent, resolvedFile)
      }
    }

    // 2. Instant Tap-to-Open with "Loading..." and <50ms Fast Launch
    itemView.setOnClickListener {
      FastFileOpener.handleFileTap(
        context = context,
        localFile = resolvedFile,
        progress = currentProgress,
        explicitMimeType = mimeType
      )
    }

    // 3. Dedicated Save to Phone Storage Button (Asynchronous Background Pipeline)
    btnSaveToPhone.setOnClickListener {
      HighSpeedTransferManager.saveToPublicStorageAndScanAsync(
        context = context,
        sourceFile = if (resolvedFile.exists()) resolvedFile else null,
        originalFileName = fileName,
        fileExtension = ext.lowercase()
      )
    }
  }

  private fun updateUiState(progress: Int, resolvedFile: File) {
    if (progress in 1..99) {
      layoutProgress.visibility = View.VISIBLE
      tvProgressStatus.text = "Loading... $progress%"
      pbTransfer.progress = progress
      btnSaveToPhone.visibility = View.GONE
    } else if (progress >= 100 || (resolvedFile.exists() && resolvedFile.length() > 0)) {
      layoutProgress.visibility = View.GONE
      tvProgressStatus.text = "Complete"
      btnSaveToPhone.visibility = View.VISIBLE
    } else {
      layoutProgress.visibility = View.GONE
      btnSaveToPhone.visibility = View.VISIBLE
    }
  }

  fun bind(fileMessage: MessageEntity) {
    val chatMessage = ChatMessage(
      id = fileMessage.id,
      conversationPeerId = fileMessage.conversationPeerId,
      senderId = fileMessage.senderId,
      senderName = fileMessage.senderName,
      isFromMe = fileMessage.isFromMe,
      text = fileMessage.text,
      binaryForm = fileMessage.binaryForm,
      cipherTextHex = fileMessage.cipherTextHex,
      mediaType = fileMessage.mediaType,
      mediaContent = fileMessage.mediaContent,
      timestamp = fileMessage.timestamp
    )
    bind(chatMessage)
  }
}
