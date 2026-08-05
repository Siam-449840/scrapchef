package com.scrapchef.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scrapchef.android.data.model.PantryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY category ASC, name ASC")
    fun getAllPantryItems(): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItemEntity)

    @Delete
    suspend fun deletePantryItem(item: PantryItemEntity)
    
    @Query("DELETE FROM pantry_items WHERE name = :name")
    suspend fun deletePantryItemByName(name: String)
}
