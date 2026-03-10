package org.rojman.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.rojman.app.data.AppRepository
import org.rojman.app.data.model.NewsCategory
import org.rojman.app.data.model.NewsPost

sealed interface AppScreen {
    data object Latest : AppScreen
    data object Favorites : AppScreen
    data object FactCheck : AppScreen
    data object Submit : AppScreen
}

data class MainUiState(
    val loading: Boolean = true,
    val currentScreen: AppScreen = AppScreen.Latest,
    val categories: List<NewsCategory> = emptyList(),
    val selectedCategory: NewsCategory? = null,
    val posts: List<NewsPost> = emptyList(),
    val factChecks: List<NewsPost> = emptyList(),
    val favorites: List<NewsPost> = emptyList(),
    val error: String? = null
)

sealed interface MainAction {
    data object Reload : MainAction
    data class OpenScreen(val screen: AppScreen) : MainAction
    data class PickCategory(val category: NewsCategory?) : MainAction
    data class ToggleFavorite(val post: NewsPost) : MainAction
    data class OpenPost(val context: Context, val post: NewsPost) : MainAction
    data class SubmitByEmail(
        val context: Context,
        val subject: String,
        val body: String,
        val senderEmail: String
    ) : MainAction
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { favorites ->
                _uiState.update { state ->
                    state.copy(
                        favorites = favorites,
                        posts = state.posts.map { post ->
                            post.copy(isFavorite = favorites.any { it.id == post.id })
                        }
                    )
                }
            }
        }
    }

    fun loadInitialData(context: Context) {
        if (_uiState.value.categories.isNotEmpty()) return
        refresh()
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.Reload -> refresh()
            is MainAction.OpenScreen -> _uiState.update { it.copy(currentScreen = action.screen) }
            is MainAction.PickCategory -> {
                _uiState.update {
                    it.copy(selectedCategory = action.category, currentScreen = AppScreen.Latest)
                }
                refreshPosts(action.category)
            }
            is MainAction.ToggleFavorite -> {
                viewModelScope.launch {
                    repository.toggleFavorite(action.post)
                    refreshPosts(_uiState.value.selectedCategory)
                }
            }
            is MainAction.OpenPost -> openUrl(action.context, action.post.link)
            is MainAction.SubmitByEmail -> submitEmail(
                action.context,
                action.subject,
                action.body,
                action.senderEmail
            )
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val categories = repository.getCategories()
                val posts = repository.getLatestPosts(_uiState.value.selectedCategory?.id)
                val factChecks = repository.getFactChecks()
                Triple(categories, posts, factChecks)
            }.onSuccess { (categories, posts, factChecks) ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        categories = categories,
                        posts = posts,
                        factChecks = factChecks,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(loading = false, error = error.message ?: "خطا در بارگذاری")
                }
            }
        }
    }

    private fun refreshPosts(category: NewsCategory?) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getLatestPosts(category?.id) }
                .onSuccess { posts -> _uiState.update { it.copy(loading = false, posts = posts) } }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun submitEmail(context: Context, subject: String, body: String, senderEmail: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:info@rojman.org")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(
                Intent.EXTRA_TEXT,
                buildString {
                    append(body)
                    append("\n\n-------------------\n")
                    append("فرستنده: ")
                    append(senderEmail.ifBlank { "نامشخص" })
                }
            )
        }
        context.startActivity(intent)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val context = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return MainViewModel(AppRepository.get(context)) as T
            }
        }
    }
}
