package org.rojman.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WpRendered(
    @SerialName("rendered") val rendered: String = ""
)

@Serializable
data class WpGuid(
    @SerialName("rendered") val rendered: String = ""
)

@Serializable
data class WpMedia(
    val source_url: String? = null
)

@Serializable
data class WpEmbedded(
    @SerialName("wp:featuredmedia") val featuredMedia: List<WpMedia> = emptyList()
)

@Serializable
data class WpPostDto(
    val id: Int,
    val date: String = "",
    val slug: String = "",
    val link: String = "",
    val title: WpRendered = WpRendered(),
    val excerpt: WpRendered = WpRendered(),
    val content: WpRendered = WpRendered(),
    val categories: List<Int> = emptyList(),
    @SerialName("_embedded") val embedded: WpEmbedded? = null
)

@Serializable
data class WpCategoryDto(
    val id: Int,
    val count: Int = 0,
    val name: String = "",
    val slug: String = ""
)

data class NewsPost(
    val id: Int,
    val title: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val link: String,
    val imageUrl: String?,
    val categoryIds: List<Int>,
    val isFavorite: Boolean = false,
    val source: String = "news"
)

data class NewsCategory(
    val id: Int,
    val name: String,
    val slug: String,
    val count: Int
)
