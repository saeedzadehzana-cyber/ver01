package org.rojman.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import org.rojman.app.data.local.AppDatabase
import org.rojman.app.data.local.FavoritePostEntity
import org.rojman.app.data.model.NewsCategory
import org.rojman.app.data.model.NewsPost
import org.rojman.app.data.remote.WpApiService
import org.rojman.app.util.cleanHtmlPreview
import org.rojman.app.util.htmlToText
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.dataStore by preferencesDataStore("rojman_prefs")

class AppRepository private constructor(private val context: Context) {

    private val favoriteDao = AppDatabase.get(context).favoriteDao()

    private val api: WpApiService by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WpApiService::class.java)
    }

    suspend fun getLatestPosts(categoryId: Int? = null): List<NewsPost> = withContext(Dispatchers.IO) {
        val favoriteIds = favoriteDao.observeFavoriteIds().first().toSet()
        api.getPosts(categoryId = categoryId).map {
            NewsPost(
                id = it.id,
                title = it.title.rendered.htmlToText(),
                excerpt = it.excerpt.rendered.cleanHtmlPreview(),
                content = it.content.rendered.htmlToText(),
                date = it.date,
                link = it.link,
                imageUrl = it.embedded?.featuredMedia?.firstOrNull()?.source_url,
                categoryIds = it.categories,
                isFavorite = it.id in favoriteIds,
                source = "news"
            )
        }
    }

    suspend fun getCategories(): List<NewsCategory> = withContext(Dispatchers.IO) {
        api.getCategories().filter { it.count > 0 }.sortedByDescending { it.count }.map {
            NewsCategory(id = it.id, name = it.name, slug = it.slug, count = it.count)
        }
    }

    suspend fun getFactChecks(): List<NewsPost> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(FACTCHECK_URL).get()
        doc.select("article").take(10).mapIndexed { index, article ->
            val anchor = article.selectFirst("h2 a, h3 a, .entry-title a, a[href]")
            val title = anchor?.text()?.trim().orEmpty()
            val link = anchor?.absUrl("href").orEmpty()
            val excerpt = article.selectFirst("p")?.text()?.trim().orEmpty()
            val image = article.selectFirst("img")?.absUrl("src")
            val time = article.selectFirst("time")?.attr("datetime") ?: ""
            NewsPost(
                id = -1000 - index,
                title = title.ifBlank { "Fact Check" },
                excerpt = excerpt,
                content = excerpt,
                date = time,
                link = if (link.isBlank()) FACTCHECK_URL else link,
                imageUrl = image,
                categoryIds = emptyList(),
                source = "factcheck"
            )
        }
    }

    fun observeFavorites(): Flow<List<NewsPost>> = favoriteDao.observeFavorites().map { rows ->
        rows.map {
            NewsPost(
                id = it.id,
                title = it.title,
                excerpt = it.excerpt,
                content = it.content,
                date = it.date,
                link = it.link,
                imageUrl = it.imageUrl,
                categoryIds = emptyList(),
                isFavorite = true
            )
        }
    }

    suspend fun toggleFavorite(post: NewsPost) {
        if (post.isFavorite) {
            favoriteDao.deleteById(post.id)
        } else {
            favoriteDao.insert(
                FavoritePostEntity(
                    id = post.id,
                    title = post.title,
                    excerpt = post.excerpt,
                    content = post.content,
                    date = post.date,
                    link = post.link,
                    imageUrl = post.imageUrl
                )
            )
        }
    }

    suspend fun storeLatestSeenPostId(postId: Int) {
        context.dataStore.edit { it[LATEST_POST_ID] = postId }
    }

    suspend fun getLatestSeenPostId(): Int? = context.dataStore.data.map { it[LATEST_POST_ID] }.first()

    companion object {
        const val BASE_URL = "https://rojman.org/"
        const val FACTCHECK_URL = "https://rojman.org/factcheck"
        private val LATEST_POST_ID = intPreferencesKey("latest_post_id")

        @Volatile private var INSTANCE: AppRepository? = null

        fun get(context: Context): AppRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
