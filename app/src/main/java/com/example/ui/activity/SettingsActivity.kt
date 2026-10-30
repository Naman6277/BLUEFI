package com.example.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.BluefiApplication
import com.example.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dedicated SettingsActivity providing a distinct full-screen Activity destination.
 * Supports smooth slide animations on entry/exit and BluetoothLeAdvertiser stealth privacy toggle.
 */
class SettingsActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
    val switchStealth = findViewById<SwitchCompat>(R.id.switch_stealth_advertising)
    val tvProfileUser = findViewById<TextView>(R.id.tv_profile_username)
    val tvProfileAvatar = findViewById<TextView>(R.id.tv_profile_avatar)

    toolbar.setNavigationOnClickListener {
      finish()
      overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    val repository = (application as BluefiApplication).repository
    val userSettings = repository.userSettings.value

    val avatarEmojis = listOf("🛸", "⚡", "👾", "🛡️", "🔮", "🛰️")
    tvProfileUser.text = "Username: @" + userSettings.username
    tvProfileAvatar.text = "Avatar Icon: " + avatarEmojis.getOrElse(userSettings.avatarIndex) { "🛸" }

    switchStealth.isChecked = userSettings.isRadarVisible

    switchStealth.setOnCheckedChangeListener { _, isChecked ->
      repository.setRadarVisibility(isChecked)
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        repository.userSettings.collectLatest { settings ->
          switchStealth.isChecked = settings.isRadarVisible
          tvProfileUser.text = "Username: @" + settings.username
          tvProfileAvatar.text = "Avatar Icon: " + avatarEmojis.getOrElse(settings.avatarIndex) { "🛸" }
        }
      }
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
  }
}
