package org.rojman.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_posts ORDER BY date DESC")
    fun observeFavorites(): Flow<List<FavoritePostEntity>>

    @Query("SELECT id FROM favorite_posts")
    fun observeFavoriteIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE id = :postId")
    suspend fun deleteById(postId: Int)
}
