package com.scrapchef.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scrapchef.android.data.model.FavoriteRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT * FROM favorites WHERE name = :name LIMIT 1")
    suspend fun getFavoriteByName(name: String): FavoriteRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteRecipeEntity): Long

    /**
     * Upserts a favorite by name — replaces the row if a favorite with the same
     * name already exists (preventing duplicates from concurrent toggle calls).
     */
    @androidx.room.Transaction
    suspend fun upsertFavoriteByName(favorite: FavoriteRecipeEntity) {
        val existing = getFavoriteByName(favorite.name)
        if (existing != null) {
            deleteFavoriteByName(favorite.name)
        }
        insertFavorite(favorite)
    }

    @Query("DELETE FROM favorites WHERE name = :name")
    suspend fun deleteFavoriteByName(name: String)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
}
