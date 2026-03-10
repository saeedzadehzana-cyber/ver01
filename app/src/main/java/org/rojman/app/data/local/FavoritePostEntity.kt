package org.rojman.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_posts")
data class FavoritePostEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val link: String,
    val imageUrl: String?
)
