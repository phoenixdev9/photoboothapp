package com.example.photoboothapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity
data class Session(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "session_name") val sessionName: String?,
//    @ColumnInfo(name = "photo_paths") val photoPaths: List<String> = emptyList() // Store photo paths as a list
)