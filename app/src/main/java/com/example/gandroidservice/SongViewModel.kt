package com.example.gandroidservice

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

class SongViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = SongRepository(application)

    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _currentSong = MutableLiveData<Song>()
    val currentSong: LiveData<Song> = _currentSong

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private var currentPosition = 0

    init {
        _songs.value = repository.loadSongs()
    }

    fun playSong(
        song: Song,
        position: Int,
    ) {
        _currentSong.value = song
        currentPosition = position
        _isPlaying.value = true
        startMusicService(song, position)
    }

    fun togglePlayPause() {
        _isPlaying.value = _isPlaying.value?.not()
        val intent =
            Intent(getApplication(), MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PAUSE
            }
        getApplication<Application>().startService(intent)
    }

    fun skipToNext() {
        _songs.value?.let { songList ->
            if (songList.isNotEmpty()) {
                currentPosition = (currentPosition + 1) % songList.size
                val nextSong = songList[currentPosition]
                _currentSong.value = nextSong
                _isPlaying.value = true

                val intent =
                    Intent(getApplication(), MusicService::class.java).apply {
                        action = MusicService.ACTION_SKIP_TO_NEXT
                        putParcelableArrayListExtra("SONG_LIST", ArrayList(songList))
                        putExtra("SONG_POSITION", currentPosition)
                    }
                getApplication<Application>().startService(intent)
            }
        }
    }

    private fun startMusicService(
        song: Song,
        position: Int,
    ) {
        val intent =
            Intent(getApplication(), MusicService::class.java).apply {
                putParcelableArrayListExtra("SONG_LIST", ArrayList(_songs.value))
                putExtra("SONG_POSITION", position)
            }
        getApplication<Application>().startService(intent)
    }

    fun adjustVolume(increase: Boolean) {
        val intent =
            Intent(getApplication(), MusicService::class.java).apply {
                action =
                    if (increase) MusicService.ACTION_VOLUME_UP else MusicService.ACTION_VOLUME_DOWN
            }
        getApplication<Application>().startService(intent)
    }

    fun seekTo(position: Int) {
        val intent =
            Intent(getApplication(), MusicService::class.java).apply {
                action = MusicService.ACTION_SEEK_TO
                putExtra("SEEK_TO_POSITION", position)
            }
        getApplication<Application>().startService(intent)
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun playRandomSong() {
        _songs.value?.let { songList ->
            if (songList.isNotEmpty()) {
                currentPosition = Random.nextInt(songList.size)
                val randomSong = songList[currentPosition]
                _currentSong.value = randomSong
                _isPlaying.value = true

                val intent =
                    Intent(getApplication(), MusicService::class.java).apply {
                        action = MusicService.ACTION_PLAY_RANDOM
                        putParcelableArrayListExtra("SONG_LIST", ArrayList(songList))
                        putExtra("SONG_POSITION", currentPosition)
                    }
                getApplication<Application>().startService(intent)
            }
        }
    }
}
