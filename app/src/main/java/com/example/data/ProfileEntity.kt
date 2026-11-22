package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gameTitle: String,
    val genre: String,
    val controlsJson: String,
    val globalOpacity: Float,
    val gridSnap: Boolean,
    val isActive: Boolean,
    val updatedAt: Long
)
