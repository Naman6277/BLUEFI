package com.example

import android.app.Application
import com.example.data.repository.BluefiRepository

class BluefiApplication : Application() {

  lateinit var repository: BluefiRepository
    private set

  override fun onCreate() {
    super.onCreate()
    com.example.data.notification.BluefiNotificationManager.createNotificationChannel(this)
    repository = BluefiRepository(this)
  }
}
