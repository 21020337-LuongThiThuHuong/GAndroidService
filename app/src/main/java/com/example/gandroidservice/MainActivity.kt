package com.example.gandroidservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gandroidservice.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var songReceiver: BroadcastReceiver
    private lateinit var progressReceiver: BroadcastReceiver
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val songs = loadSongs()
        val songAdapter = SongAdapter(songs, this)

        binding.songCount.text = "${songs.size} bài hát"

        binding.songList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.playnpauseButton.setOnClickListener {
            val intent =
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_PAUSE
                }

            if (isPlaying) {
                binding.playnpauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
            } else {
                binding.playnpauseButton.setImageResource(R.drawable.baseline_pause_24)
            }
            startService(intent)
        }

        binding.skipButton.setOnClickListener {
            val intent =
                Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_SKIP_TO_NEXT
                    putParcelableArrayListExtra("SONG_LIST", ArrayList(songs))
                    putExtra("SONG_POSITION", songAdapter.getSelectedPosition())
                }
            startService(intent)

            songAdapter.setSelectedPosition((songAdapter.getSelectedPosition() + 1) % songs.size)
        }

        binding.randomButton.setOnClickListener {
            val randomPosition = Random.nextInt(songs.size)
            val intent =
                Intent(this, MusicService::class.java).apply {
                    putParcelableArrayListExtra("SONG_LIST", ArrayList(songs))
                    putExtra("SONG_POSITION", randomPosition)
                }
            startService(intent)
            songAdapter.setSelectedPosition(randomPosition)
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.volumeUpButton.setOnClickListener {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
        }

        binding.volumeDownButton.setOnClickListener {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        }

        // Initialize and register the songReceiver
        songReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    intent?.let {
                        val songName = it.getStringExtra("SONG_NAME")
                        val songArtist = it.getStringExtra("SONG_ARTIST")
                        val songImage = it.getStringExtra("SONG_IMAGE")
                        val songPosition = it.getIntExtra("SONG_POSITION", -1)
                        isPlaying = it.getBooleanExtra("IS_PLAYING", false)

                        if (songPosition != -1) {
                            songAdapter.setSelectedPosition(songPosition)
                        }

                        if (songName != null && songArtist != null && songImage != null) {
                            // Update the play song bar UI
                            binding.playSongName.text = songName
                            binding.playSongArtist.text = songArtist

                            // Get image resource identifier
                            val imageResourceId =
                                resources.getIdentifier(songImage, "drawable", packageName)
                            if (imageResourceId != 0) {
                                binding.playSongImg.setImageResource(imageResourceId)
                            } else {
                                binding.playSongImg.setImageResource(R.drawable.ic_launcher_background)
                            }

                            // Show the play song bar
                            binding.playSongBar.visibility = View.VISIBLE

                            // Update play/pause button icon
                            updatePlayPauseButton()
                        } else {
                            // Log the error or handle it appropriately
                        }
                    }
                }
            }
        registerReceiver(songReceiver, IntentFilter(MusicService.ACTION_UPDATE_UI))

        // Initialize and register the progressReceiver
        progressReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    intent?.let {
                        val progress = it.getIntExtra("PROGRESS", 0)
                        val duration = it.getIntExtra("DURATION", 0)
                        binding.seekBar.max = duration
                        binding.seekBar.progress = progress
                        binding.seekBar.visibility = View.VISIBLE
                    }
                }
            }
        registerReceiver(progressReceiver, IntentFilter(MusicService.ACTION_UPDATE_PROGRESS))

        // Set listener for seek bar changes
        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        val seekIntent =
                            Intent(this@MainActivity, MusicService::class.java).apply {
                                action = MusicService.ACTION_SEEK_TO
                                putExtra("SEEK_TO_POSITION", it.progress)
                            }
                        startService(seekIntent)
                    }
                }
            },
        )
    }

    private fun updatePlayPauseButton() {
        if (isPlaying) {
            binding.playnpauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            binding.playnpauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }
    }

    private fun loadSongs(): List<Song> {
        val inputStream = resources.openRawResource(R.raw.songs)
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<List<Song>>() {}.type
        return Gson().fromJson(reader, type)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(songReceiver)
        unregisterReceiver(progressReceiver)
    }
}
