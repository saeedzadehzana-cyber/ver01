package org.rojman.app.data.remote

import org.rojman.app.data.model.WpCategoryDto
import org.rojman.app.data.model.WpPostDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WpApiService {
    @GET("wp-json/wp/v2/posts?_embed=1")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 20,
        @Query("categories") categoryId: Int? = null,
        @Query("search") search: String? = null
    ): List<WpPostDto>

    @GET("wp-json/wp/v2/categories")
    suspend fun getCategories(
        @Query("per_page") perPage: Int = 100
    ): List<WpCategoryDto>
}
