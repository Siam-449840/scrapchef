package com.scrapchef.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scrapchef.android.data.model.FavoriteRecipeEntity
import com.scrapchef.android.data.model.IngredientEntity
import com.scrapchef.android.data.model.PantryItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [IngredientEntity::class, FavoriteRecipeEntity::class, PantryItemEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun pantryDao(): PantryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scrapchef_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.ingredientDao())
                    }
                }
            }

            suspend fun populateDatabase(
                ingredientDao: IngredientDao
            ) {
                if (ingredientDao.getCount() == 0) {
                    ingredientDao.insertAll(SeedData.INGREDIENTS)
                }
            }
        }
    }
}
