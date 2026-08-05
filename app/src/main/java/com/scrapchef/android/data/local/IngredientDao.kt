package com.scrapchef.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scrapchef.android.data.model.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY canonical ASC")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE canonical LIKE '%' || :query || '%' OR synonymsJson LIKE '%' || :query || '%' LIMIT 20")
    suspend fun searchIngredients(query: String): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<IngredientEntity>)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun getCount(): Int
}
