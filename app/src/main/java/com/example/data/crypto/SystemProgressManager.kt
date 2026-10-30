package com.example.data.crypto

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * SystemProgressManager:
 * Centralized thread-safe progress state machine for system initialization
 * and real-time chunked file transfer pipelines.
 *
 * Guarantees immediate bootstrap (1%), eliminates 0% stalls, enforces 100ms
 * throttling, and reliably delivers final 100% completion events.
 */
object SystemProgressManager {
  private const val TAG = "SystemProgressManager"

  // Active file transfer progress tracking: transferId -> MutableStateFlow<Int>
  private val _progressMap = ConcurrentHashMap<String, MutableStateFlow<Int>>()
  private val _statusTextMap = ConcurrentHashMap<String, MutableStateFlow<String>>()

  // System initialization progress (0% - 100%)
  private val _systemInitProgress = MutableStateFlow(100)
  val systemInitProgress: StateFlow<Int> = _systemInitProgress.asStateFlow()

  // Track last UI update timestamp to prevent main thread flooding
  private val _lastUpdateTimestamp = ConcurrentHashMap<String, Long>()

  fun getProgressFlow(transferId: String): StateFlow<Int> {
    return _progressMap.getOrPut(transferId) {
      MutableStateFlow(0)
    }.asStateFlow()
  }

  fun getStatusTextFlow(transferId: String): StateFlow<String> {
    return _statusTextMap.getOrPut(transferId) {
      MutableStateFlow("Loading... 0%")
    }.asStateFlow()
  }

  fun getProgress(transferId: String): Int {
    return _progressMap[transferId]?.value ?: 0
  }

  /**
   * Immediately bootstraps transfer progress to at least 1% upon receiving metadata or chunk 1,
   * preventing 0% stuck progress in RecyclerView items.
   */
  fun bootstrapProgress(transferId: String) {
    reportProgress(transferId, 1, "Loading... 1%", forceUpdate = true)
  }

  /**
   * Reports guarded progress percentage (1..100) with optional 100ms throttle check.
   */
  fun reportProgress(
    transferId: String,
    percentage: Int,
    statusText: String? = null,
    forceUpdate: Boolean = false
  ) {
    val clamped = percentage.coerceIn(0, 100)
    val now = System.currentTimeMillis()
    val lastTime = _lastUpdateTimestamp[transferId] ?: 0L

    if (forceUpdate || clamped == 100 || clamped == 1 || now - lastTime >= 100) {
      _lastUpdateTimestamp[transferId] = now

      val flow = _progressMap.getOrPut(transferId) { MutableStateFlow(clamped) }
      flow.value = clamped

      val text = statusText ?: if (clamped >= 100) "Complete" else "Loading... $clamped%"
      val textFlow = _statusTextMap.getOrPut(transferId) { MutableStateFlow(text) }
      textFlow.value = text

      Log.d(TAG, "Progress updated for $transferId: $clamped% ($text)")
    }
  }

  /**
   * Safe floating-point calculation avoiding division by zero and enforcing 1% to 100%.
   */
  fun calculateGuardedProgress(totalBytesReceived: Long, totalBytes: Long): Int {
    if (totalBytes <= 0L) return 1
    if (totalBytesReceived >= totalBytes) return 100
    val rawPercentage = (totalBytesReceived.toDouble() / totalBytes.toDouble()) * 100.0
    return rawPercentage.coerceIn(1.0, 99.0).toInt()
  }

  /**
   * Completes transfer with explicit 100% emission.
   */
  fun markComplete(transferId: String) {
    reportProgress(transferId, 100, "Complete", forceUpdate = true)
  }

  /**
   * System initialization progress update.
   */
  fun updateSystemInitProgress(percent: Int) {
    _systemInitProgress.value = percent.coerceIn(0, 100)
  }

  fun clearTransfer(transferId: String) {
    _progressMap.remove(transferId)
    _statusTextMap.remove(transferId)
    _lastUpdateTimestamp.remove(transferId)
  }
}
