package com.example.data.local

import androidx.room.*
import com.example.data.model.MenuItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items ORDER BY id ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem)

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Delete
    suspend fun deleteMenuItem(item: MenuItem)

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getMenuItemCount(): Int
}
