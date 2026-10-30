package com.example.ui.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.BluefiApplication
import com.example.R
import com.example.data.mesh.RemotePeerNode
import com.example.ui.adapter.ChatMessageAdapter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dedicated ChatActivity binding messages to a native RecyclerView ListAdapter.
 * Strictly binds remotePeerNode.username and displayName to the top chat header,
 * ensuring Device A displays "ani" and Device B displays "baba".
 */
class ChatActivity : AppCompatActivity() {

  companion object {
    const val EXTRA_PEER_ID = "extra_peer_id"
    const val EXTRA_PEER_NAME = "extra_peer_name"
    const val EXTRA_PEER_USER = "extra_peer_user"
    const val EXTRA_PEER_AVATAR = "extra_peer_avatar"
  }

  private lateinit var adapter: ChatMessageAdapter
  private lateinit var recyclerView: RecyclerView
  private lateinit var etInput: EditText
  private lateinit var btnSend: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_chat)

    val peerId = intent.getStringExtra(EXTRA_PEER_ID) ?: ""
    val initialPeerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Peer Device"
    val initialPeerUser = intent.getStringExtra(EXTRA_PEER_USER) ?: "@peer"
    val initialPeerAvatar = intent.getStringExtra(EXTRA_PEER_AVATAR) ?: "🛸"

    val toolbar = findViewById<MaterialToolbar>(R.id.chat_toolbar)
    val tvAvatar = findViewById<TextView>(R.id.tv_chat_avatar)
    val tvDisplayName = findViewById<TextView>(R.id.tv_chat_display_name)
    val tvUsername = findViewById<TextView>(R.id.tv_chat_username)

    tvAvatar.text = initialPeerAvatar
    tvDisplayName.text = initialPeerName
    tvUsername.text = if (initialPeerUser.startsWith("@")) initialPeerUser else "@$initialPeerUser"

    toolbar.setNavigationOnClickListener {
      finish()
      overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    recyclerView = findViewById(R.id.recycler_chat_messages)
    etInput = findViewById(R.id.et_chat_input)
    btnSend = findViewById(R.id.btn_chat_send)

    val layoutManager = LinearLayoutManager(this).apply {
      stackFromEnd = true
    }
    recyclerView.layoutManager = layoutManager
    adapter = ChatMessageAdapter()
    recyclerView.adapter = adapter

    val repository = (application as BluefiApplication).repository

    // Observe peer record from Room DB to ensure remote peer's username is strictly bound in header
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        repository.allPeers.collectLatest { peers ->
          val remote = peers.find { it.id == peerId }
          if (remote != null) {
            tvDisplayName.text = remote.displayName
            tvUsername.text = "@${remote.username}"
            val avatarEmojis = listOf("🛸", "⚡", "👾", "🛡️", "🔮", "🛰️")
            tvAvatar.text = avatarEmojis.getOrElse(remote.avatarIndex) { remote.displayName.take(1) }
          }
        }
      }
    }

    // Observe incoming socket/database updates via Kotlin Coroutines StateFlow posted to main thread
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        repository.getMessagesForPeer(peerId).collectLatest { messages ->
          adapter.submitList(messages) {
            if (messages.isNotEmpty()) {
              recyclerView.scrollToPosition(messages.size - 1)
            }
          }
        }
      }
    }

    btnSend.setOnClickListener {
      val text = etInput.text.toString().trim()
      if (text.isNotBlank()) {
        lifecycleScope.launch {
          repository.sendMessage(peerId, text)
          etInput.setText("")
        }
      }
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
  }
}
