package com.example.data.crypto

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * MediaFileOpener:
 * WhatsApp-style file and media opening redirect with explicit completion verification,
 * dynamic MIME resolution, native Intent.createChooser ("Open with:"), and
 * missing app catch with a helpful install prompt.
 */
object MediaFileOpener {
  private const val TAG = "MediaFileOpener"

  /**
   * Opens any file using WhatsApp-style Intent.createChooser flow.
   *
   * @param context Application/Activity context
   * @param publicFile Completed target file (must be 100% downloaded)
   * @param explicitMimeType Optional explicit MIME type override
   * @param isFullyDownloaded Flag ensuring file reassembly is 100% complete
   */
  fun openWithChooser(
    context: Context,
    publicFile: File?,
    explicitMimeType: String? = null,
    isFullyDownloaded: Boolean = true
  ) {
    FastFileOpener.openWithChooser(context, publicFile, explicitMimeType, isFullyDownloaded)
  }

  fun openFile(context: Context, file: File, explicitMimeType: String? = null) {
    FastFileOpener.openWithChooser(context, file, explicitMimeType, isFullyDownloaded = true)
  }
}
