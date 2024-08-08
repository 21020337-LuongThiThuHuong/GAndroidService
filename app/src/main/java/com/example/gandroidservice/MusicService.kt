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
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val ACTION_UPDATE_UI = "com.example.gandroidservice.UPDATE_UI"
        const val ACTION_PLAY_PAUSE = "com.example.gandroidservice.PLAY_PAUSE"
        const val ACTION_UPDATE_PROGRESS = "com.example.gandroidservice.UPDATE_PROGRESS"
        const val ACTION_SEEK_TO = "com.example.gandroidservice.SEEK_TO"
        const val ACTION_SKIP_TO_NEXT = "com.example.gandroidservice.SKIP_TO_NEXT"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = Handler()
    private val updateProgressTask = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                val progress = it.currentPosition
                val duration = it.duration
                val progressIntent = Intent(ACTION_UPDATE_PROGRESS).apply {
                    putExtra("PROGRESS", progress)
                    putExtra("DURATION", duration)
                }
                sendBroadcast(progressIntent)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private var songList: List<Song> = emptyList()
    private var currentPosition: Int = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_SEEK_TO -> {
                val seekToPosition = intent.getIntExtra("SEEK_TO_POSITION", 0)
                mediaPlayer?.seekTo(seekToPosition)
            }
            ACTION_SKIP_TO_NEXT -> skipToNext()
            else -> {
                songList = intent?.getParcelableArrayListExtra<Song>("SONG_LIST") ?: emptyList()
                currentPosition = intent?.getIntExtra("SONG_POSITION", 0) ?: 0
                playSong(songList[currentPosition])
            }
        }
        return START_NOT_STICKY
    }

    private fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPlaying = false
            handler.removeCallbacks(updateProgressTask)
        } else {
            mediaPlayer?.start()
            isPlaying = true
            handler.post(updateProgressTask)
        }

        // Send broadcast to update UI
        val updateUIIntent = Intent(ACTION_UPDATE_UI).apply {
            putExtra("IS_PLAYING", isPlaying)
        }
        sendBroadcast(updateUIIntent)
    }

    private fun skipToNext() {
        if (songList.isNotEmpty()) {
            currentPosition = (currentPosition + 1) % songList.size
            playSong(songList[currentPosition])
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun playSong(song: Song) {
        val songName = song.song_name
        val songArtist = song.song_artist
        val songFile = song.song_file.substringBeforeLast(".")
        val songImage = song.song_image

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(songName)
            .setContentText(songArtist)
            .setSmallIcon(R.drawable.sasageyo)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        val resId = resources.getIdentifier(songFile, "raw", packageName)
        if (resId != 0) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
            isPlaying = true

            // Set OnCompletionListener to automatically play the next song
            mediaPlayer?.setOnCompletionListener {
                skipToNext()
            }

            val updateUIIntent = Intent(ACTION_UPDATE_UI).apply {
                putExtra("SONG_NAME", songName)
                putExtra("SONG_ARTIST", songArtist)
                putExtra("SONG_IMAGE", songImage)
                putExtra("IS_PLAYING", isPlaying)
            }
            sendBroadcast(updateUIIntent)

            handler.post(updateProgressTask)
        } else {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacks(updateProgressTask)
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
