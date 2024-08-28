package com.example.gandroidservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gandroidservice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SongViewModel
    private lateinit var songReceiver: BroadcastReceiver
    private lateinit var progressReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(SongViewModel::class.java)

        setupUI()
        observeViewModel()
        setupReceivers()
    }

    private fun setupUI() {
        val songs = viewModel.songs.value ?: emptyList()
        val songAdapter =
            SongAdapter(songs, this) { song, position ->
                viewModel.playSong(song, position)
            }

        binding.songCount.text = "${songs.size} bài hát"

        binding.songList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }

        binding.playnpauseButton.setOnClickListener { viewModel.togglePlayPause() }
        binding.skipButton.setOnClickListener { viewModel.skipToNext() }
        binding.randomButton.setOnClickListener { viewModel.playRandomSong() }
        binding.volumeUpButton.setOnClickListener { viewModel.adjustVolume(true) }
        binding.volumeDownButton.setOnClickListener { viewModel.adjustVolume(false) }

        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let { viewModel.seekTo(it.progress) }
                }
            },
        )
    }

    private fun observeViewModel() {
        viewModel.currentSong.observe(this) { song ->
            updatePlaySongBar(song)
        }

        viewModel.isPlaying.observe(this) { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }
    }

    private fun setupReceivers() {
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
                        val isPlaying = it.getBooleanExtra("IS_PLAYING", false)

                        if (songPosition != -1) {
                            (binding.songList.adapter as? SongAdapter)?.setSelectedPosition(songPosition)
                        }

                        if (songName != null && songArtist != null && songImage != null) {
                            updatePlaySongBar(Song(songName, songArtist, "", songImage))
                            viewModel.updateIsPlaying(isPlaying)
                        }
                    }
                }
            }
        registerReceiver(songReceiver, IntentFilter(MusicService.ACTION_UPDATE_UI))

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
    }

    private fun updatePlaySongBar(song: Song) {
        binding.playSongName.text = song.song_name
        binding.playSongArtist.text = song.song_artist

        val imageResourceId = resources.getIdentifier(song.song_image, "drawable", packageName)
        if (imageResourceId != 0) {
            binding.playSongImg.setImageResource(imageResourceId)
        } else {
            binding.playSongImg.setImageResource(R.drawable.ic_launcher_background)
        }

        binding.playSongBar.visibility = View.VISIBLE
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.playnpauseButton.setImageResource(R.drawable.baseline_pause_24)
        } else {
            binding.playnpauseButton.setImageResource(R.drawable.baseline_play_arrow_24)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(songReceiver)
        unregisterReceiver(progressReceiver)
    }
}
