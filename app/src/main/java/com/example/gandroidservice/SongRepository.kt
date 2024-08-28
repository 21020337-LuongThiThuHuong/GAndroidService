package com.example.gandroidservice

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class SongRepository(
    private val context: Context,
) {
    fun loadSongs(): List<Song> {
        val inputStream = context.resources.openRawResource(R.raw.songs)
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<List<Song>>() {}.type
        return Gson().fromJson(reader, type)
    }
}
