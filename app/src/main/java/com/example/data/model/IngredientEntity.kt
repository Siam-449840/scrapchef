package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey val canonical: String,
    val synonymsJson: String, // JSON array of synonyms e.g. ["scallion", "spring onion"]
    val category: String = "General"
)
