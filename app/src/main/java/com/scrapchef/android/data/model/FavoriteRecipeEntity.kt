package com.scrapchef.android.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["name"], unique = true)]
)
data class FavoriteRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val recipeJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
