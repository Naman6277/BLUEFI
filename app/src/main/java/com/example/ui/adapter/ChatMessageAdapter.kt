package com.example.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

  companion object {
    private const val VIEW_TYPE_OUTGOING = 1
    private const val VIEW_TYPE_INCOMING = 2

    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
      override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
      }

      override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return if (getItem(position).isFromMe) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return if (viewType == VIEW_TYPE_OUTGOING) {
      val view = inflater.inflate(R.layout.item_message_outgoing, parent, false)
      OutgoingViewHolder(view)
    } else {
      val view = inflater.inflate(R.layout.item_message_incoming, parent, false)
      IncomingViewHolder(view)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val message = getItem(position)
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    if (holder is OutgoingViewHolder) {
      holder.tvText.text = message.text
      holder.tvTime.text = timeStr
    } else if (holder is IncomingViewHolder) {
      holder.tvText.text = message.text
      holder.tvTime.text = timeStr
    }
  }

  class OutgoingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvText: TextView = itemView.findViewById(R.id.tv_message_outgoing_text)
    val tvTime: TextView = itemView.findViewById(R.id.tv_message_outgoing_time)
  }

  class IncomingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvText: TextView = itemView.findViewById(R.id.tv_message_incoming_text)
    val tvTime: TextView = itemView.findViewById(R.id.tv_message_incoming_time)
  }
}
