package com.example.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.ui.BluefiViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.BluefiTheme
import com.example.util.DynamicFontManager
import com.example.util.DynamicThemeManager

/**
 * LoginActivity: Dedicated authentication activity providing dynamic PIN validation (PIN_4, PIN_6),
 * Alphanumeric password support, 3 security questions password recovery, and multi-account switching.
 */
class LoginActivity : ComponentActivity() {

  private val viewModel: BluefiViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    DynamicThemeManager.applyThemeToActivity(this)
    DynamicFontManager.initialize(this)
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
      val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
      val isAccountRegistered = androidx.compose.runtime.remember(isLoggedIn) { viewModel.isAccountRegistered() }
      val registeredUserId = androidx.compose.runtime.remember(isLoggedIn) { viewModel.getRegisteredUserId() }
      val authFeedbackMessage by viewModel.authFeedbackMessage.collectAsStateWithLifecycle()
      val isAuthSuccess by viewModel.isAuthSuccess.collectAsStateWithLifecycle()
      val showAuthPopup by viewModel.showAuthPopup.collectAsStateWithLifecycle()
      val authPopupMessage by viewModel.authPopupMessage.collectAsStateWithLifecycle()
      val authPopupSuccess by viewModel.authPopupSuccess.collectAsStateWithLifecycle()

      val activeFont by DynamicFontManager.activeFontState.collectAsStateWithLifecycle()

      BluefiTheme(themeOption = userSettings.theme, fontStyle = activeFont) {
        LoginScreen(
          isAccountRegistered = isAccountRegistered,
          registeredUserId = registeredUserId,
          authFeedbackMessage = authFeedbackMessage,
          isAuthSuccess = isAuthSuccess,
          showAuthPopup = showAuthPopup,
          authPopupMessage = authPopupMessage,
          authPopupSuccess = authPopupSuccess,
          onDismissPopup = {
            viewModel.dismissAuthPopup()
          },
          onLogin = { enteredPass ->
            viewModel.verifyLoginPassword(enteredPass)
          },
          onLoginAccount = { targetUserId, enteredPass, callback ->
            viewModel.loginToAccount(targetUserId, enteredPass, callback)
          },
          onCreateAccountWithQuestions = { newUserId, newPass, q1, a1, q2, a2, q3, a3 ->
            viewModel.createAccount(newUserId, newPass, q1, a1, q2, a2, q3, a3)
          },
          onResetAccount = {
            viewModel.resetAccount()
          },
          onAccessGranted = {
            viewModel.dismissAuthPopup()
            viewModel.grantAccess()
            startActivity(Intent(this, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
          },
          onGetSecurityQuestions = { uid ->
            viewModel.getSecurityQuestions(uid)
          },
          onVerifySecurityAnswers = { uid, a1, a2, a3 ->
            viewModel.verifySecurityAnswers(uid, a1, a2, a3)
          },
          onResetPassword = { uid, newPass ->
            viewModel.resetPasswordWithAnswers(uid, newPass)
          }
        )
      }
    }
  }

  companion object {
    fun start(context: Context) {
      val intent = Intent(context, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      context.startActivity(intent)
    }
  }
}
