package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wheels")
data class WheelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val namesString: String, // Newline-delimited names, e.g. "Alice\nBob\nCharlie"
    val colorTheme: String = "Rainbow", // "Rainbow", "Cosmic", "Neon", "Pastel"
    val centerEmoji: String = "🎯",
    val spinDuration: Int = 5, // in seconds
    val lastModified: Long = System.currentTimeMillis()
) {
    // Helper property to get list of names
    val namesList: List<String>
        get() = namesString.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
