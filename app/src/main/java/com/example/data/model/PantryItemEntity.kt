package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey val name: String,
    val category: String,
    val dateAdded: Long = System.currentTimeMillis()
)
