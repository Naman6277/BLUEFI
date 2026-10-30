package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.PeerDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.PeerEntity
import com.example.data.local.entity.UserAccountEntity

@Database(
  entities = [
    PeerEntity::class,
    MessageEntity::class,
    UserAccountEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class BluefiDatabase : RoomDatabase() {

  abstract fun peerDao(): PeerDao
  abstract fun messageDao(): MessageDao
  abstract fun userAccountDao(): UserAccountDao

  companion object {
    @Volatile
    private var INSTANCE: BluefiDatabase? = null

    fun getDatabase(context: Context): BluefiDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          BluefiDatabase::class.java,
          "bluefi_mesh_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
