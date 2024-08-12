package com.example.gandroidservice

import android.os.Parcel
import android.os.Parcelable

data class Song(
    val song_name: String,
    val song_artist: String,
    val song_file: String,
    val song_image: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeString(song_name)
        parcel.writeString(song_artist)
        parcel.writeString(song_file)
        parcel.writeString(song_image)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song = Song(parcel)

        override fun newArray(size: Int): Array<Song?> = arrayOfNulls(size)
    }
}
