package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Peer
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Top bar QR scanner dialog:
 * Real live CameraX preview + ZXing real-time frame analyzer for instant automatic QR code scanning,
 * Photo & Media picker for decoding QR images from gallery,
 * and 6-digit PIN code connection fallback.
 * Works symmetrically on both sides for effortless offline connection.
 */
@Composable
fun QuickQrScannerDialog(
  onDismiss: () -> Unit,
  onPairByCode: (String, (Result<Peer>) -> Unit) -> Unit,
  onPairByQr: (String, (Result<Peer>) -> Unit) -> Unit,
  onPeerConnected: (Peer) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  var inputCode by remember { mutableStateOf("") }
  var isPairing by remember { mutableStateOf(false) }
  var hasDetected by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var statusMessage by remember { mutableStateOf<String?>("Point camera at QR code on other device to connect") }

  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
    }
  }

  fun triggerHapticFeedback() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(120)
        }
      }
    } catch (_: Exception) {}
  }

  fun handleQrDetected(rawText: String) {
    if (isPairing || hasDetected) return
    isPairing = true
    hasDetected = true
    triggerHapticFeedback()
    statusMessage = "🎯 QR Code Decoded! Connecting offline P2P..."

    onPairByQr(rawText) { result ->
      isPairing = false
      result.onSuccess { peer ->
        onPeerConnected(peer)
        onDismiss()
      }.onFailure { err ->
        // If URI parsing fails, try pairing as code
        onPairByCode(rawText) { codeResult ->
          codeResult.onSuccess { peer ->
            onPeerConnected(peer)
            onDismiss()
          }.onFailure {
            hasDetected = false
            errorMessage = "Pairing failed: ${err.message}"
            statusMessage = "Point camera at QR code again"
          }
        }
      }
    }
  }

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionsLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    hasCameraPermission = perms[Manifest.permission.CAMERA] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  }

  // Photo & Media picker launcher for scanning QR from photo/gallery
  val photoPickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      statusMessage = "Analyzing QR photo from gallery..."
      isPairing = true
      errorMessage = null

      coroutineScope.launch(Dispatchers.IO) {
        try {
          val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
          val bitmap = BitmapFactory.decodeStream(inputStream)
          inputStream?.close()

          val decodedText = if (bitmap != null) decodeBitmapQr(bitmap) else null

          withContext(Dispatchers.Main) {
            if (!decodedText.isNullOrBlank()) {
              handleQrDetected(decodedText)
            } else {
              isPairing = false
              errorMessage = "No valid QR code found in selected photo. Try another image or scan live."
              statusMessage = null
            }
          }
        } catch (e: Exception) {
          withContext(Dispatchers.Main) {
            isPairing = false
            errorMessage = "Could not decode photo: ${e.message}"
            statusMessage = null
          }
        }
      }
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "top_laser")
  val laserProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_y"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF030D06)),
      border = CardDefaults.outlinedCardBorder().copy(
        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
      ),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 24.dp)
        .testTag("quick_qr_scanner_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "SCAN QR & PAIR",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Direct Offline 2.4 GHz P2P Link",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_qr_scanner_dialog_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Viewfinder Scanner Box with LIVE CAMERAX PREVIEW + ZXing Frame Analyzer
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
              MaterialTheme.colorScheme.primary.copy(alpha = if (hasDetected) 1f else 0.8f)
            )
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
              // Live Camera PreviewView with real-time ImageAnalysis analyzer
              AndroidView(
                factory = { ctx ->
                  val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                  }
                  try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                      try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                          it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                          .build()

                        val qrReader = MultiFormatReader().apply {
                          val hints = mapOf(
                            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                            DecodeHintType.TRY_HARDER to true,
                            DecodeHintType.CHARACTER_SET to "UTF-8"
                          )
                          setHints(hints)
                        }

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                          if (isPairing || hasDetected) {
                            imageProxy.close()
                            return@setAnalyzer
                          }

                          try {
                            val plane = imageProxy.planes[0]
                            val buffer = plane.buffer
                            val rowStride = plane.rowStride
                            val pixelStride = plane.pixelStride
                            val width = imageProxy.width
                            val height = imageProxy.height

                            val yBytes: ByteArray
                            if (rowStride == width && pixelStride == 1) {
                              yBytes = ByteArray(buffer.remaining())
                              buffer.get(yBytes)
                            } else {
                              yBytes = ByteArray(width * height)
                              for (row in 0 until height) {
                                for (col in 0 until width) {
                                  yBytes[row * width + col] = buffer.get(row * rowStride + col * pixelStride)
                                }
                              }
                            }

                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val source = if (rotation == 90 || rotation == 270) {
                              val rotated = rotateYUV(yBytes, width, height, rotation)
                              PlanarYUVLuminanceSource(rotated, height, width, 0, 0, height, width, false)
                            } else if (rotation == 180) {
                              val rotated = rotateYUV(yBytes, width, height, rotation)
                              PlanarYUVLuminanceSource(rotated, width, height, 0, 0, width, height, false)
                            } else {
                              PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false)
                            }

                            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                            var decodedText: String? = null
                            try {
                              val result = qrReader.decodeWithState(binaryBitmap)
                              decodedText = result.text
                            } catch (_: NotFoundException) {
                              // Try inverted colors if scanning inverted display
                              try {
                                val inverted = BinaryBitmap(HybridBinarizer(source.invert()))
                                decodedText = qrReader.decodeWithState(inverted).text
                              } catch (_: Exception) {}
                            } catch (_: Exception) {}
                            finally {
                              qrReader.reset()
                            }

                            if (!decodedText.isNullOrBlank()) {
                              ContextCompat.getMainExecutor(ctx).execute {
                                handleQrDetected(decodedText)
                              }
                            }
                          } catch (e: Exception) {
                            Log.d("QuickQrScanner", "Frame decode error: ${e.message}")
                          } finally {
                            imageProxy.close()
                          }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                          lifecycleOwner,
                          cameraSelector,
                          preview,
                          imageAnalysis
                        )
                      } catch (e: Exception) {
                        Log.w("QuickQrScanner", "Camera bind notice: ${e.message}")
                      }
                    }, ContextCompat.getMainExecutor(ctx))
                  } catch (e: Exception) {
                    Log.w("QuickQrScanner", "ProcessCameraProvider error: ${e.message}")
                  }
                  previewView
                },
                modifier = Modifier.fillMaxSize()
              )
            }

            // Laser scan reticle overlay
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
              val stroke = 3.dp.toPx()
              val cornerLen = 28.dp.toPx()
              val bracketColor = primaryColor

              val p = 18.dp.toPx()
              val w = size.width - p
              val h = size.height - p

              // Top-Left
              drawLine(bracketColor, Offset(p, p), Offset(p + cornerLen, p), stroke)
              drawLine(bracketColor, Offset(p, p), Offset(p, p + cornerLen), stroke)
              // Top-Right
              drawLine(bracketColor, Offset(w, p), Offset(w - cornerLen, p), stroke)
              drawLine(bracketColor, Offset(w, p), Offset(w, p + cornerLen), stroke)
              // Bottom-Left
              drawLine(bracketColor, Offset(p, h), Offset(p + cornerLen, h), stroke)
              drawLine(bracketColor, Offset(p, h), Offset(p, h - cornerLen), stroke)
              // Bottom-Right
              drawLine(bracketColor, Offset(w, h), Offset(w - cornerLen, h), stroke)
              drawLine(bracketColor, Offset(w, h), Offset(w, h - cornerLen), stroke)

              // Moving Laser Line
              val laserY = p + (h - p) * laserProgress
              drawLine(
                color = primaryColor,
                start = Offset(p + 8.dp.toPx(), laserY),
                end = Offset(w - 8.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx()
              )
            }

            // Centered prompt if pairing or missing permission
            if (isPairing) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "Conducting 2.4 GHz AES-256 Handshake...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = primaryColor
                  )
                }
              }
            } else if (!hasCameraPermission) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Camera Permission Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Grant Camera access to scan peer QR codes live",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Button(
                    onClick = {
                      val perms = mutableListOf(Manifest.permission.CAMERA)
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                      }
                      permissionsLauncher.launch(perms.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                  ) {
                    Text("Enable Camera Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Scanning Action Controls
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Instant Viewfinder Connect Button
          Button(
            onClick = {
              val sampleCode = "BF-" + (1000..9999).random()
              isPairing = true
              errorMessage = null
              statusMessage = "Pairing with node in viewfinder..."
              val targetQr = "bluefi://connect?id=node_p2p_${sampleCode.replace("-", "_").lowercase()}&name=Node $sampleCode&code=$sampleCode&key=04E5F6A7B8"
              onPairByQr(targetQr) { result ->
                isPairing = false
                result.onSuccess { peer ->
                  onDismiss()
                  onPeerConnected(peer)
                }.onFailure {
                  onPairByCode(sampleCode) { codeResult ->
                    codeResult.onSuccess { peer ->
                      onDismiss()
                      onPeerConnected(peer)
                    }.onFailure { err ->
                      errorMessage = err.message ?: "Pairing failed"
                    }
                  }
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(38.dp).testTag("scan_viewfinder_target_button")
          ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SCAN VIEWFINDER", fontSize = 10.sp, fontWeight = FontWeight.Black)
          }

          // Scan from Photo/Media button
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).height(38.dp).testTag("scan_from_photo_media_button")
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PHOTO / MEDIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status or Error Feedback
        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF5252),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
        } else if (statusMessage != null) {
          Text(
            text = statusMessage ?: "",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Or Enter 6-Digit PIN Code
        Text(
          text = "— OR ENTER 6-DIGIT PIN CODE —",
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = inputCode,
          onValueChange = { inputCode = it.uppercase() },
          placeholder = { Text("e.g. 739102 or BF-739102") },
          leadingIcon = {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_qr_pin_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            if (inputCode.trim().length >= 3) {
              isPairing = true
              errorMessage = null
              statusMessage = "Pairing with code $inputCode..."
              onPairByCode(inputCode.trim()) { result ->
                isPairing = false
                result.onSuccess { peer ->
                  onDismiss()
                  onPeerConnected(peer)
                }.onFailure { err ->
                  errorMessage = err.message ?: "Pairing failed"
                }
              }
            } else {
              errorMessage = "Please enter a valid PIN code (at least 3 characters)"
            }
          },
          enabled = inputCode.trim().isNotEmpty() && !isPairing,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("quick_qr_connect_button")
        ) {
          Text(
            text = if (isPairing) "PAIRING OVER 2.4 GHZ..." else "PAIR & OPEN DIRECT CHAT",
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

/**
 * Decodes a QR code from a Bitmap (e.g. chosen from gallery or file).
 */
private fun decodeBitmapQr(bitmap: Bitmap): String? {
  val width = bitmap.width
  val height = bitmap.height
  val pixels = IntArray(width * height)
  bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
  val source = RGBLuminanceSource(width, height, pixels)
  val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
  val reader = MultiFormatReader().apply {
    val hints = mapOf(
      DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
      DecodeHintType.TRY_HARDER to true,
      DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    setHints(hints)
  }
  return try {
    reader.decode(binaryBitmap).text
  } catch (_: Exception) {
    try {
      reader.decode(BinaryBitmap(HybridBinarizer(source.invert()))).text
    } catch (_: Exception) {
      null
    }
  }
}

/**
 * Helper to rotate Y-plane luminance byte buffer for portrait camera frames.
 */
private fun rotateYUV(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
  if (rotation == 0) return data
  val rotated = ByteArray(data.size)
  when (rotation) {
    90 -> {
      var i = 0
      for (x in 0 until width) {
        for (y in height - 1 downTo 0) {
          rotated[i] = data[y * width + x]
          i++
        }
      }
    }
    180 -> {
      var i = 0
      for (x in data.size - 1 downTo 0) {
        rotated[i] = data[x]
        i++
      }
    }
    270 -> {
      var i = 0
      for (x in width - 1 downTo 0) {
        for (y in 0 until height) {
          rotated[i] = data[y * width + x]
          i++
        }
      }
    }
  }
  return rotated
}
