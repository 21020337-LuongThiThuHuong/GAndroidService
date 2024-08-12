package com.example.gandroidservice

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gandroidservice.databinding.SongItemBinding

class SongAdapter(
    private val songs: List<Song>,
    private val context: Context,
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    private var selectedPosition = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SongViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SongViewHolder,
        position: Int,
    ) {
        val song = songs[position]
        holder.bind(song, selectedPosition)
        holder.itemView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)

            val intent =
                Intent(context, MusicService::class.java).apply {
                    putParcelableArrayListExtra("SONG_LIST", ArrayList(songs))
                    putExtra("SONG_POSITION", selectedPosition)
                }
            context.startService(intent)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun getSelectedPosition(): Int = selectedPosition

    fun setSelectedPosition(position: Int) {
        val previousSelectedPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelectedPosition)
        notifyItemChanged(selectedPosition)
    }

    class SongViewHolder(
        private val binding: SongItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            song: Song,
            selectedPosition: Int,
        ) {
            binding.songName.text = song.song_name
            binding.songArtist.text = song.song_artist
            binding.songStatus.visibility =
                if (adapterPosition == selectedPosition) View.VISIBLE else View.GONE
            val imageResourceId =
                binding.root.context.resources.getIdentifier(
                    song.song_image,
                    "drawable",
                    binding.root.context.packageName,
                )
            if (imageResourceId != 0) {
                binding.songImg.setImageResource(imageResourceId)
            } else {
                binding.songImg.setImageResource(R.drawable.ic_launcher_background) // Default image
            }
        }
    }
}
