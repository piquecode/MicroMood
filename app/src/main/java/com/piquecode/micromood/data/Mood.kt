package com.piquecode.micromood.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class Mood(
    @PrimaryKey val date: Date,
    val mood: Int,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "tags")
    val tags: String? = null  // Comma-separated tags: "happy,excited,grateful"
)