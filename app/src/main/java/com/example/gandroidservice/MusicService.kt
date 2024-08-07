package com.example.gandroidservice

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val ACTION_UPDATE_UI = "com.example.gandroidservice.UPDATE_UI"
        const val ACTION_PLAY_PAUSE = "com.example.gandroidservice.PLAY_PAUSE"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            else -> {
                val songName = intent?.getStringExtra("SONG_NAME")
                val songArtist = intent?.getStringExtra("SONG_ARTIST")
                val songFile = intent?.getStringExtra("SONG_FILE")
                val songImage = intent?.getStringExtra("SONG_IMAGE")

                val notificationIntent = Intent(this, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

                val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(songName)
                    .setContentText(songArtist)
                    .setSmallIcon(R.drawable.sasageyo)
                    .setContentIntent(pendingIntent)
                    .build()

                startForeground(1, notification)

                // Phát nhạc
                if (songFile != null) {
                    val resId = resources.getIdentifier(songFile, "raw", packageName)
                    if (resId != 0) {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer.create(this, resId)
                        mediaPlayer?.start()
                        isPlaying = true

                        // Gửi broadcast để cập nhật UI
                        val updateUIIntent = Intent(ACTION_UPDATE_UI).apply {
                            putExtra("SONG_NAME", songName)
                            putExtra("SONG_ARTIST", songArtist)
                            putExtra("SONG_IMAGE", songImage)
                            putExtra("IS_PLAYING", isPlaying)
                        }
                        sendBroadcast(updateUIIntent)
                    } else {
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            mediaPlayer?.start()
            isPlaying = true
        }

        // Gửi broadcast để cập nhật UI
        val updateUIIntent = Intent(ACTION_UPDATE_UI).apply {
            putExtra("IS_PLAYING", isPlaying)
        }
        sendBroadcast(updateUIIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
