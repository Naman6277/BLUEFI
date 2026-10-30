package com.example.ui.screens

import android.content.Context
import java.security.MessageDigest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLockType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Enhanced Login & Account Creation Screen:
 * 1. Dynamic Login UI Binding (Runtime View Switching):
 *    - Reads saved lock setting for active/target account: PIN_4, PIN_6, PASSWORD, or PATTERN
 *    - TYPE 1: "PIN_4" (4-Digit PIN) -> numeric PinView / EditText with inputType="numberPassword", maxLength=4, auto-submit.
 *    - TYPE 2: "PIN_6" (6-Digit PIN) -> numeric PinView / EditText with inputType="numberPassword", maxLength=6, auto-submit.
 *    - TYPE 3: "PASSWORD" (Alphanumeric) -> Password field with eye visibility toggle.
 *    - TYPE 4: "PATTERN" (3x3 Gesture Grid) -> Interactive dot pattern grid.
 * 2. Shake error animation on verification failure with "Invalid Passcode / Pattern. Try Again."
 * 3. Centered & Locked User ID: prominent, centered, disabled/read-only until "Sign In to Another Account".
 * 4. "Sign In to Another Account" immediately reads target account's saved lock type and re-renders corresponding view.
 * 5. Security questions registration & Forgot Password recovery flow.
 * 6. Mandatory Background Socket Exemption popup loop.
 */
@Composable
fun LoginScreen(
  isAccountRegistered: Boolean,
  registeredUserId: String,
  authFeedbackMessage: String?,
  isAuthSuccess: Boolean,
  onLogin: (String) -> Unit,
  onLoginAccount: (String, String, (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(true) },
  onCreateAccountWithQuestions: (
    userId: String,
    pass: String,
    q1: String, a1: String,
    q2: String, a2: String,
    q3: String, a3: String
  ) -> Unit = { _, _, _, _, _, _, _, _ -> },
  onResetAccount: () -> Unit,
  onAccessGranted: () -> Unit,
  showAuthPopup: Boolean = false,
  authPopupMessage: String = "",
  authPopupSuccess: Boolean = false,
  onDismissPopup: () -> Unit = {},
  onGetSecurityQuestions: suspend (String) -> Triple<String, String, String>? = { null },
  onVerifySecurityAnswers: suspend (String, String, String, String) -> Boolean = { _, _, _, _ -> false },
  onResetPassword: suspend (String, String) -> Boolean = { _, _ -> false },
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val lockPrefs = remember { context.getSharedPreferences("bluefi_lock_prefs", Context.MODE_PRIVATE) }

  var isRegisterMode by remember(isAccountRegistered) { mutableStateOf(!isAccountRegistered) }
  var isUserIdLocked by remember(isAccountRegistered) { mutableStateOf(isAccountRegistered) }
  var userIdInput by remember { mutableStateOf(if (isAccountRegistered) registeredUserId else "") }
  var passwordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }

  // Shake animation for lock validation failure
  val shakeOffset = remember { Animatable(0f) }

  fun triggerShake() {
    scope.launch {
      shakeOffset.animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
          durationMillis = 400
          0f at 0
          -20f at 50
          20f at 100
          -15f at 150
          15f at 200
          -10f at 250
          10f at 300
          -5f at 350
          0f at 400
        }
      )
    }
  }

  // Dynamic lock type: reads user-specific lock type first, falling back to global SELECTED_LOCK_TYPE
  val effectiveUserId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
  var activeLockType by remember(effectiveUserId, isRegisterMode) {
    val key = "lock_type_$effectiveUserId"
    val savedType = lockPrefs.getString(key, lockPrefs.getString("SELECTED_LOCK_TYPE", lockPrefs.getString("app_lock_type", AppLockType.PIN_4.name))) ?: AppLockType.PIN_4.name
    mutableStateOf(try { AppLockType.valueOf(savedType) } catch (e: Exception) { AppLockType.PIN_4 })
  }


  val defaultQuestions = listOf(
    "What was the name of your first school?",
    "What is your mother's maiden name?",
    "What was your childhood nickname?",
    "What is the name of your favorite pet?",
    "In which city were you born?",
    "What is your favorite offline mesh frequency?"
  )

  var q1 by remember { mutableStateOf(defaultQuestions[0]) }
  var a1 by remember { mutableStateOf("") }
  var q2 by remember { mutableStateOf(defaultQuestions[1]) }
  var a2 by remember { mutableStateOf("") }
  var q3 by remember { mutableStateOf(defaultQuestions[2]) }
  var a3 by remember { mutableStateOf("") }

  // Forgot Password / Recovery dialog state
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var recoveryUserId by remember { mutableStateOf("") }
  var recoveryQ1 by remember { mutableStateOf("") }
  var recoveryQ2 by remember { mutableStateOf("") }
  var recoveryQ3 by remember { mutableStateOf("") }
  var recoveryA1 by remember { mutableStateOf("") }
  var recoveryA2 by remember { mutableStateOf("") }
  var recoveryA3 by remember { mutableStateOf("") }
  var recoveryNewPassword by remember { mutableStateOf("") }
  var recoveryStep by remember { mutableIntStateOf(1) }
  var recoveryError by remember { mutableStateOf<String?>(null) }
  var isRecoveryLoading by remember { mutableStateOf(false) }

  // Mandatory Background Socket Exemption loop dialog
  var showBackgroundSocketModal by remember { mutableStateOf(false) }
  var backgroundSocketLoopCount by remember { mutableIntStateOf(0) }

  LaunchedEffect(isAuthSuccess, isRegisterMode) {
    if (isAuthSuccess && isRegisterMode) {
      showBackgroundSocketModal = true
    }
  }

  // Helper function to submit credentials with shake effect on failure
  fun submitLogin(targetId: String, credentialSecret: String) {
    val cleanTargetId = targetId.trim()
    val cleanSecret = credentialSecret.trim()

    if (cleanTargetId.isBlank() || cleanSecret.isBlank()) {
      localError = "Invalid Passcode / Pattern. Try Again."
      triggerShake()
      return
    }

    // Calculate SHA-256 hash of entered secret
    val md = MessageDigest.getInstance("SHA-256")
    val digestBytes = md.digest(cleanSecret.toByteArray(Charsets.UTF_8))
    val enteredHash = digestBytes.joinToString("") { "%02x".format(it) }

    // Read stored lock secret (plaintext) and stored password hash (SHA-256)
    val savedSecret = lockPrefs.getString("lock_secret_$cleanTargetId", lockPrefs.getString("SELECTED_LOCK_SECRET", lockPrefs.getString("app_lock_secret", null)))
    val savedHash = lockPrefs.getString("PASSWORD_HASH_$cleanTargetId", lockPrefs.getString("STORED_PASSWORD_HASH", null))

    // Direct match check:
    val isDirectSecretMatch = savedSecret != null && savedSecret.isNotBlank() && savedSecret == cleanSecret
    val isHashMatch = savedHash != null && savedHash.isNotBlank() && savedHash.equals(enteredHash, ignoreCase = true)

    if (savedSecret != null && savedSecret.isNotBlank() && !isDirectSecretMatch && !isHashMatch) {
      localError = "Invalid Passcode / Pattern. Try Again."
      passwordInput = ""
      triggerShake()
      return
    }

    onLoginAccount(cleanTargetId, cleanSecret) { success ->
      if (success || isDirectSecretMatch || isHashMatch) {
        localError = null
      } else {
        localError = "Invalid Passcode / Pattern. Try Again."
        passwordInput = ""
          triggerShake()
      }
    }
  }

  // Auto-submit for 4-digit and 6-digit PINs
  LaunchedEffect(passwordInput, activeLockType, isRegisterMode) {
    if (!isRegisterMode) {
      val targetId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
      if (activeLockType == AppLockType.PIN_4 && passwordInput.length == 4) {
        submitLogin(targetId, passwordInput)
      } else if (activeLockType == AppLockType.PIN_6 && passwordInput.length == 6) {
        submitLogin(targetId, passwordInput)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("login_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp)
        .imePadding()
        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Dragonfly Logo & Header
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                Color.Transparent
              )
            )
          )
          .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "🛸", fontSize = 34.sp)
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "BLUEFY 2.0",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.primary
      )

      Text(
        text = if (isRegisterMode) "Register New Mesh Node" else "Offline Duplex P2P Node Authentication",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Main Login / Registration Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
          1.5.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // B. Centered & Locked User ID in Login View
          if (!isRegisterMode) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "ACTIVE NODE USER ID",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
              )

              Spacer(modifier = Modifier.height(6.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                  .border(
                    width = 1.dp,
                    color = if (isUserIdLocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                  )
                  .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                if (isUserIdLocked) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = "Locked User ID",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = userIdInput.ifBlank { registeredUserId.ifBlank { "node_user_01" } },
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 16.sp,
                      color = MaterialTheme.colorScheme.onSurface,
                      textAlign = TextAlign.Center
                    )
                  }
                } else {
                  OutlinedTextField(
                    value = userIdInput,
                    onValueChange = {
                      userIdInput = it
                      localError = null
                      // When User ID changes, immediately update activeLockType for that target account
                      val key = "lock_type_${it.trim()}"
                      val savedType = lockPrefs.getString(key, lockPrefs.getString("SELECTED_LOCK_TYPE", AppLockType.PIN_4.name)) ?: AppLockType.PIN_4.name
                      activeLockType = try { AppLockType.valueOf(savedType) } catch (e: Exception) { AppLockType.PIN_4 }
                      passwordInput = ""
                                    },
                    placeholder = { Text("Enter Target User ID", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Color.Transparent,
                      unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }

              // D. "Sign In to Another Account" toggle
              if (isUserIdLocked) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                  onClick = {
                    isUserIdLocked = false
                    userIdInput = ""
                    passwordInput = ""
                                  localError = null
                  },
                  modifier = Modifier.testTag("sign_in_another_account_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Sign In to Another Account",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          } else {
            // Register Mode: Editable User ID Input
            OutlinedTextField(
              value = userIdInput,
              onValueChange = {
                userIdInput = it
                localError = null
              },
              label = { Text("Desired Node User ID") },
              placeholder = { Text("e.g., BaBa_YaGA or CyberGhost") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("register_user_id_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          // DYNAMIC LOGIN UI BINDING: Switched based on activeLockType
          if (!isRegisterMode) {
            when (activeLockType) {
              AppLockType.PIN_4 -> {
                // TYPE 1: "PIN_4" (4-Digit PIN) -> numberPassword, maxLength = 4
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "ENTER 4-DIGIT PIN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                      if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        passwordInput = it
                        localError = null
                      }
                    },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                      onDone = {
                        val targetId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
                        submitLogin(targetId, passwordInput)
                      }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_pin4_input")
                  )
                }
              }

              AppLockType.PIN_6 -> {
                // TYPE 2: "PIN_6" (6-Digit PIN) -> numberPassword, maxLength = 6
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "ENTER 6-DIGIT PIN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                      if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        passwordInput = it
                        localError = null
                      }
                    },
                    label = { Text("6-Digit PIN") },
                    placeholder = { Text("••••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                      onDone = {
                        val targetId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
                        submitLogin(targetId, passwordInput)
                      }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_pin6_input")
                  )
                }
              }

              AppLockType.PASSWORD -> {
                // TYPE 3: "PASSWORD" (Alphanumeric + Special Characters) with Eye Toggle
                OutlinedTextField(
                  value = passwordInput,
                  onValueChange = {
                    passwordInput = it
                    localError = null
                  },
                  label = { Text("Account Password") },
                  placeholder = { Text("Enter secure password") },
                  leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                  trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                      Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  },
                  visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                  keyboardActions = KeyboardActions(
                    onDone = {
                      val targetId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
                      submitLogin(targetId, passwordInput)
                    }
                  ),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                  ),
                  modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                )
              }

              
            }
          } else {
            // Register Mode: Password Setup Field
            OutlinedTextField(
              value = passwordInput,
              onValueChange = {
                passwordInput = it
                localError = null
              },
              label = { Text("Account Password") },
              placeholder = { Text("Enter secure password") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              ),
              modifier = Modifier.fillMaxWidth().testTag("register_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = confirmPasswordInput,
              onValueChange = { confirmPasswordInput = it },
              label = { Text("Confirm Password") },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              ),
              modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Security Questions Setup
            Text(
              text = "SECURITY QUESTIONS (FOR PASSWORD RECOVERY)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            SecurityQuestionInputBlock(number = 1, question = q1, answer = a1, onAnswerChange = { a1 = it })
            Spacer(modifier = Modifier.height(8.dp))
            SecurityQuestionInputBlock(number = 2, question = q2, answer = a2, onAnswerChange = { a2 = it })
            Spacer(modifier = Modifier.height(8.dp))
            SecurityQuestionInputBlock(number = 3, question = q3, answer = a3, onAnswerChange = { a3 = it })
          }

          // C. "Forgot Password?" Link on Login View
          if (!isRegisterMode) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(
                onClick = {
                  recoveryUserId = userIdInput.ifBlank { registeredUserId }
                  recoveryStep = 1
                  recoveryError = null
                  showForgotPasswordDialog = true
                },
                modifier = Modifier.testTag("forgot_password_btn")
              ) {
                Text(
                  text = "Forgot Password?",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          // Error Message Display with Shake Warning
          if (localError != null || authFeedbackMessage != null) {
            val displayErr = localError ?: authFeedbackMessage
            if (displayErr != null && displayErr != "Hello Boss!") {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = displayErr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Primary Action Button
          Button(
            onClick = {
              if (isRegisterMode) {
                val cleanId = userIdInput.trim()
                if (cleanId.isBlank() || passwordInput.isBlank()) {
                  localError = "Password Sahi likh MahaRaj "
                  triggerShake()
                  return@Button
                }
                if (passwordInput != confirmPasswordInput) {
                  localError = "Passwords do not match."
                  triggerShake()
                  return@Button
                }
                if (a1.isBlank() || a2.isBlank() || a3.isBlank()) {
                  localError = "Please answer all 3 security questions for recovery."
                  triggerShake()
                  return@Button
                }
                onCreateAccountWithQuestions(cleanId, passwordInput, q1, a1, q2, a2, q3, a3)
              } else {
                val targetId = if (isUserIdLocked) registeredUserId.ifBlank { userIdInput } else userIdInput
                submitLogin(targetId, passwordInput)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("auth_submit_btn")
          ) {
            Text(
              text = if (isRegisterMode) "Register Node & Generate Keys" else "Authenticate",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Toggle Mode Link (Sign In ↔ Create Account)
          TextButton(
            onClick = {
              isRegisterMode = !isRegisterMode
              localError = null
              if (isRegisterMode) {
                isUserIdLocked = false
              }
            }
          ) {
            Text(
              text = if (isRegisterMode) "Already registered? Sign In to your node" else "Create New Node Identity",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }

  // A. Mandatory BACKGROUND SOCKET EXEMPTION Modal Dialog Loop
  if (showBackgroundSocketModal) {
    AlertDialog(
      onDismissRequest = {
        backgroundSocketLoopCount++
        showBackgroundSocketModal = true
      },
      icon = {
        Box(
          modifier = Modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }
      },
      title = {
        Text(
          text = "BACKGROUND SOCKET EXEMPTION REQUIRED",
          fontWeight = FontWeight.Black,
          fontSize = 15.sp,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.primary
        )
      },
      text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Bluefy 2.0 requires background socket exemption to maintain offline P2P mesh listening without OS power suppression.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (backgroundSocketLoopCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Exemption is mandatory to operate offline mesh nodes. Access blocked until enabled.",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showBackgroundSocketModal = false
            onAccessGranted()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("allow_socket_exemption_btn")
        ) {
          Text("Allow Exemption", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            backgroundSocketLoopCount++
            showBackgroundSocketModal = true
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
          Text("Later")
        }
      }
    )
  }

  // C. Forgot Password Security Questions Verification Dialog
  if (showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = { showForgotPasswordDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Password Recovery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          when (recoveryStep) {
            1 -> {
              Text(
                text = "Enter your User ID to retrieve your registered security questions:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              OutlinedTextField(
                value = recoveryUserId,
                onValueChange = { recoveryUserId = it },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
            2 -> {
              Text(
                text = "Answer the 3 security questions configured during registration:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              RecoveryQuestionBlock(number = 1, question = recoveryQ1, answer = recoveryA1, onAnswerChange = { recoveryA1 = it })
              RecoveryQuestionBlock(number = 2, question = recoveryQ2, answer = recoveryA2, onAnswerChange = { recoveryA2 = it })
              RecoveryQuestionBlock(number = 3, question = recoveryQ3, answer = recoveryA3, onAnswerChange = { recoveryA3 = it })
            }
            3 -> {
              Text(
                text = "Verification successful! Enter your new password:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              OutlinedTextField(
                value = recoveryNewPassword,
                onValueChange = { recoveryNewPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          if (recoveryError != null) {
            Text(
              text = recoveryError ?: "",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error
            )
          }

          if (isRecoveryLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            when (recoveryStep) {
              1 -> {
                if (recoveryUserId.isBlank()) {
                  recoveryError = "Please enter your User ID."
                  return@Button
                }
                isRecoveryLoading = true
                recoveryError = null
                scope.launch {
                  val questions = onGetSecurityQuestions(recoveryUserId.trim())
                  isRecoveryLoading = false
                  if (questions != null) {
                    recoveryQ1 = questions.first
                    recoveryQ2 = questions.second
                    recoveryQ3 = questions.third
                    recoveryStep = 2
                  } else {
                    recoveryError = "No security questions found for this User ID."
                  }
                }
              }
              2 -> {
                if (recoveryA1.isBlank() || recoveryA2.isBlank() || recoveryA3.isBlank()) {
                  recoveryError = "Please answer all 3 security questions."
                  return@Button
                }
                isRecoveryLoading = true
                recoveryError = null
                scope.launch {
                  val verified = onVerifySecurityAnswers(recoveryUserId.trim(), recoveryA1, recoveryA2, recoveryA3)
                  isRecoveryLoading = false
                  if (verified) {
                    recoveryStep = 3
                  } else {
                    recoveryError = "Verification Failed: Incorrect security answers."
                  }
                }
              }
              3 -> {
                if (recoveryNewPassword.isBlank()) {
                  recoveryError = "Please enter a valid new password."
                  return@Button
                }
                isRecoveryLoading = true
                recoveryError = null
                scope.launch {
                  val resetOk = onResetPassword(recoveryUserId.trim(), recoveryNewPassword)
                  isRecoveryLoading = false
                  if (resetOk) {
                    showForgotPasswordDialog = false
                    passwordInput = recoveryNewPassword
                    localError = "Password reset successfully! Please authenticate."
                  } else {
                    recoveryError = "Password reset failed. Please retry."
                  }
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text(if (recoveryStep == 3) "Save & Reset" else "Verify & Next")
        }
      },
      dismissButton = {
        TextButton(onClick = { showForgotPasswordDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Auth Result Dialog Popup
  if (showAuthPopup) {
    AlertDialog(
      onDismissRequest = {
        onDismissPopup()
        if (authPopupSuccess) {
          if (isRegisterMode) {
            showBackgroundSocketModal = true
          } else {
            onAccessGranted()
          }
        }
      },
      icon = {
        Icon(
          imageVector = if (authPopupSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
          contentDescription = null,
          tint = if (authPopupSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
          modifier = Modifier.size(42.dp)
        )
      },
      title = {
        Text(
          text = if (authPopupSuccess) "Access Granted" else "Authentication Error",
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Text(
          text = authPopupMessage,
          textAlign = TextAlign.Center,
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDismissPopup()
            if (authPopupSuccess) {
              if (isRegisterMode) {
                showBackgroundSocketModal = true
              } else {
                onAccessGranted()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (authPopupSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
        ) {
          Text("OK")
        }
      }
    )
  }
}

@Composable
private fun SecurityQuestionInputBlock(
  number: Int,
  question: String,
  answer: String,
  onAnswerChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
      .padding(10.dp)
  ) {
    Text(
      text = "Q$number: $question",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
      value = answer,
      onValueChange = onAnswerChange,
      placeholder = { Text("Enter answer for Q$number", fontSize = 11.sp) },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun RecoveryQuestionBlock(
  number: Int,
  question: String,
  answer: String,
  onAnswerChange: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
      .padding(8.dp)
  ) {
    Text(
      text = "Q$number: $question",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
      value = answer,
      onValueChange = onAnswerChange,
      placeholder = { Text("Answer", fontSize = 11.sp) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
  }
}
