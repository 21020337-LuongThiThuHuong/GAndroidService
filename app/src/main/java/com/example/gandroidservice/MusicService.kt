package com.example.gandroidservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class MusicService : Service() {
    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val ACTION_UPDATE_UI = "com.example.gandroidservice.UPDATE_UI"
        const val ACTION_PLAY_PAUSE = "com.example.gandroidservice.PLAY_PAUSE"
        const val ACTION_UPDATE_PROGRESS = "com.example.gandroidservice.UPDATE_PROGRESS"
        const val ACTION_SEEK_TO = "com.example.gandroidservice.SEEK_TO"
        const val ACTION_SKIP_TO_NEXT = "com.example.gandroidservice.SKIP_TO_NEXT"
        const val ACTION_VOLUME_UP = "com.example.gandroidservice.VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.example.gandroidservice.VOLUME_DOWN"
        const val ACTION_PLAY_RANDOM = "com.example.gandroidservice.PLAY_RANDOM"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = Handler()
    private val updateProgressTask =
        object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val progress = it.currentPosition
                    val duration = it.duration
                    val progressIntent =
                        Intent(ACTION_UPDATE_PROGRESS).apply {
                            putExtra("PROGRESS", progress)
                            putExtra("DURATION", duration)
                        }
                    sendBroadcast(progressIntent)

                    updateNotification()
                }
                handler.postDelayed(this, 1000)
            }
        }

    private var songList: List<Song> = emptyList()
    private var currentPosition: Int = 0
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_SEEK_TO -> {
                val seekToPosition = intent.getIntExtra("SEEK_TO_POSITION", 0)
                mediaPlayer?.seekTo(seekToPosition)
            }

            ACTION_SKIP_TO_NEXT -> skipToNext()
            ACTION_VOLUME_UP -> adjustVolume(true)
            ACTION_VOLUME_DOWN -> adjustVolume(false)
            ACTION_PLAY_RANDOM -> playRandomSong()
            else -> {
                songList = intent?.getParcelableArrayListExtra<Song>("SONG_LIST") ?: emptyList()
                currentPosition = intent?.getIntExtra("SONG_POSITION", 0) ?: 0
                playSong(songList[currentPosition])
            }
        }
        return START_NOT_STICKY
    }

    private fun playRandomSong() {
        if (songList.isNotEmpty()) {
            currentPosition = Random.nextInt(songList.size)
            val randomSong = songList[currentPosition]
            playSong(randomSong)

            // Send broadcast to update the adapter
            val updateUIIntent =
                Intent(ACTION_UPDATE_UI).apply {
                    putExtra("SONG_POSITION", currentPosition)
                    putExtra("IS_PLAYING", isPlaying)
                }
            sendBroadcast(updateUIIntent)
        } else {
            Log.e("MusicService", "No songs available to play")
        }
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

        updateNotification()
        val updateUIIntent =
            Intent(ACTION_UPDATE_UI).apply {
                putExtra("IS_PLAYING", isPlaying)
            }
        sendBroadcast(updateUIIntent)
    }

    private fun skipToNext() {
        if (songList.isNotEmpty()) {
            currentPosition = (currentPosition + 1) % songList.size
            playSong(songList[currentPosition])

            sendUpdateUIBroadcast()
        }
    }

    private fun sendUpdateUIBroadcast() {
        val updateUIIntent =
            Intent(ACTION_UPDATE_UI).apply {
                putExtra("SONG_NAME", songList[currentPosition].song_name)
                putExtra("SONG_ARTIST", songList[currentPosition].song_artist)
                putExtra("SONG_IMAGE", songList[currentPosition].song_image)
                putExtra("SONG_POSITION", currentPosition)
                putExtra("IS_PLAYING", isPlaying)
            }
        sendBroadcast(updateUIIntent)
    }

    private fun adjustVolume(increase: Boolean) {
        val streamType = AudioManager.STREAM_MUSIC
        val volume =
            if (increase) {
                audioManager.getStreamVolume(streamType) + 1
            } else {
                audioManager.getStreamVolume(streamType) - 1
            }
        audioManager.setStreamVolume(
            streamType,
            volume.coerceIn(0, audioManager.getStreamMaxVolume(streamType)),
            0,
        )

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun playSong(song: Song) {
        val songName = song.song_name
        val songArtist = song.song_artist
        val songFile = song.song_file.substringBeforeLast(".")
        val songImage = song.song_image

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationLayout =
            RemoteViews(packageName, R.layout.noti_customize).apply {
                setTextViewText(R.id.notification_title, songName)
                setTextViewText(R.id.notification_artist, songArtist)

                val imageResId = resources.getIdentifier(songImage, "drawable", packageName)
                if (imageResId != 0) {
                    setImageViewResource(R.id.notification_img, imageResId)
                }

                val playPauseIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_PLAY_PAUSE
                    }
                val playPausePendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        1,
                        playPauseIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent)

                val skipIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_SKIP_TO_NEXT
                    }
                val skipPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        2,
                        skipIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.notification_skip, skipPendingIntent)

                val volumeUpIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_VOLUME_UP
                    }
                val volumeUpPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        3,
                        volumeUpIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.volume_up_button, volumeUpPendingIntent)

                val volumeDownIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_VOLUME_DOWN
                    }
                val volumeDownPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        4,
                        volumeDownIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.volume_down_button, volumeDownPendingIntent)

                val progress = mediaPlayer?.currentPosition ?: 0
                val maxProgress = mediaPlayer?.duration ?: 100
                setProgressBar(R.id.notification_progress, maxProgress, progress, false)
            }

        val notification: Notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.sasageyo)
                .setContentIntent(pendingIntent)
                .setCustomContentView(notificationLayout)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()

        startForeground(1, notification)

        // Handle playing the song
        val resId = resources.getIdentifier(songFile, "raw", packageName)
        if (resId != 0) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
            isPlaying = true

            mediaPlayer?.setOnCompletionListener {
                skipToNext()
            }

            val updateUIIntent =
                Intent(ACTION_UPDATE_UI).apply {
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

    private fun updateNotification() {
        val notificationLayout =
            RemoteViews(packageName, R.layout.noti_customize).apply {
                val song = songList[currentPosition]
                setTextViewText(R.id.notification_title, song.song_name)
                setTextViewText(R.id.notification_artist, song.song_artist)
                val imageResId = resources.getIdentifier(song.song_image, "drawable", packageName)
                if (imageResId != 0) {
                    setImageViewResource(R.id.notification_img, imageResId)
                }

                if (isPlaying) {
                    setImageViewResource(R.id.notification_play_pause, R.drawable.baseline_pause_24)
                } else {
                    setImageViewResource(
                        R.id.notification_play_pause,
                        R.drawable.baseline_play_arrow_24,
                    )
                }

                val progress = mediaPlayer?.currentPosition ?: 0
                val maxProgress = mediaPlayer?.duration ?: 100
                setProgressBar(R.id.notification_progress, maxProgress, progress, false)

                // Setup play/pause button action
                val playPauseIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_PLAY_PAUSE
                    }
                val playPausePendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        1,
                        playPauseIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent)

                // Setup skip button action
                val skipIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_SKIP_TO_NEXT
                    }
                val skipPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        2,
                        skipIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.notification_skip, skipPendingIntent)

                // Setup volume up button action
                val volumeUpIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_VOLUME_UP
                    }
                val volumeUpPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        3,
                        volumeUpIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.volume_up_button, volumeUpPendingIntent)

                // Setup volume down button action
                val volumeDownIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_VOLUME_DOWN
                    }
                val volumeDownPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        4,
                        volumeDownIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.volume_down_button, volumeDownPendingIntent)

                // Setup play random button action
                val playRandomIntent =
                    Intent(this@MusicService, MusicService::class.java).apply {
                        action = ACTION_PLAY_RANDOM
                    }
                val playRandomPendingIntent =
                    PendingIntent.getService(
                        this@MusicService,
                        5,
                        playRandomIntent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                setOnClickPendingIntent(R.id.random_button, playRandomPendingIntent)
            }

        val notification: Notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.sasageyo)
                .setCustomContentView(notificationLayout)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacks(updateProgressTask)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
