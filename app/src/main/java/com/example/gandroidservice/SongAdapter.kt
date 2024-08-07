package com.example.gandroidservice

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gandroidservice.databinding.SongItemBinding

class SongAdapter(private val songs: List<Song>, private val context: Context) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION // Variable to keep track of selected position

    inner class SongViewHolder(private val binding: SongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, isSelected: Boolean) {
            binding.songName.text = song.song_name
            binding.songArtist.text = song.song_artist
            val imageResourceId = binding.root.context.resources.getIdentifier(
                song.song_image, "drawable", binding.root.context.packageName)
            binding.songImg.setImageResource(imageResourceId)

            // Show or hide the song_status based on selection
            binding.songStatus.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                val intent = Intent(context, MusicService::class.java).apply {
                    putExtra("SONG_NAME", song.song_name)
                    putExtra("SONG_ARTIST", song.song_artist)
                    putExtra("SONG_FILE", song.song_file.substringBeforeLast("."))
                    putExtra("SONG_IMAGE", song.song_image)
                }
                context.startService(intent)

                // Update selected position and notify the adapter
                notifyItemChanged(selectedPosition) // Reset the previously selected item
                selectedPosition = adapterPosition
                notifyItemChanged(selectedPosition) // Highlight the newly selected item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val isSelected = position == selectedPosition
        holder.bind(songs[position], isSelected)
    }

    override fun getItemCount() = songs.size
}
