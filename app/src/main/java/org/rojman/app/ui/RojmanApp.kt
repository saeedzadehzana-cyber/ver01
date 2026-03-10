package org.rojman.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.rojman.app.data.model.NewsCategory
import org.rojman.app.data.model.NewsPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RojmanApp(state: MainUiState, onAction: (MainAction) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "منو",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("همه خبرها") },
                    selected = state.selectedCategory == null,
                    onClick = {
                        onAction(MainAction.PickCategory(null))
                        scope.launch { drawerState.close() }
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "دسته‌بندی‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                state.categories.forEach { category ->
                    NavigationDrawerItem(
                        label = { Text(category.name) },
                        selected = state.selectedCategory?.id == category.id,
                        onClick = {
                            onAction(MainAction.PickCategory(category))
                            scope.launch { drawerState.close() }
                        }
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("ارسال خبر") },
                    selected = state.currentScreen == AppScreen.Submit,
                    onClick = {
                        onAction(MainAction.OpenScreen(AppScreen.Submit))
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            when (state.currentScreen) {
                                AppScreen.Latest -> state.selectedCategory?.name ?: "آخرین خبرها"
                                AppScreen.Favorites -> "موارد دلخواه"
                                AppScreen.FactCheck -> "فکت‌چک"
                                AppScreen.Submit -> "ارسال خبر"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(MainAction.Reload) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.Latest,
                        onClick = { onAction(MainAction.OpenScreen(AppScreen.Latest)) },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("خبرها") }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.Favorites,
                        onClick = { onAction(MainAction.OpenScreen(AppScreen.Favorites)) },
                        icon = { Icon(Icons.Default.Star, null) },
                        label = { Text("دلخواه") }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.FactCheck,
                        onClick = { onAction(MainAction.OpenScreen(AppScreen.FactCheck)) },
                        icon = { Icon(Icons.Default.CheckCircle, null) },
                        label = { Text("راستی‌آزمایی") }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.Submit,
                        onClick = { onAction(MainAction.OpenScreen(AppScreen.Submit)) },
                        icon = { Icon(Icons.Default.Email, null) },
                        label = { Text("ارسال") }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            when {
                state.loading -> LoadingPane(innerPadding)
                state.currentScreen == AppScreen.Submit -> SubmitScreen(innerPadding, onAction)
                state.currentScreen == AppScreen.Favorites -> NewsList(innerPadding, state.favorites, onAction)
                state.currentScreen == AppScreen.FactCheck -> NewsList(innerPadding, state.factChecks, onAction)
                else -> NewsList(innerPadding, state.posts, onAction)
            }
        }
    }
}

@Composable
private fun LoadingPane(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewsList(innerPadding: PaddingValues, posts: List<NewsPost>, onAction: (MainAction) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(post = post, onAction = onAction)
        }
    }
}

@Composable
private fun PostCard(post: NewsPost, onAction: (MainAction) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(MainAction.OpenPost(context, post)) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            post.imageUrl?.takeIf { it.isNotBlank() }?.let {
                AsyncImage(
                    model = it,
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(post.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(post.date.take(10), style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = { onAction(MainAction.ToggleFavorite(post)) }) {
                    Icon(
                        if (post.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitScreen(innerPadding: PaddingValues, onAction: (MainAction) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var senderEmail by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("خبر، گزارش یا یادداشت خود را برای تحریریه روژمان بفرستید.")
        }
        item {
            OutlinedTextField(
                value = senderEmail,
                onValueChange = { senderEmail = it },
                label = { Text("ایمیل شما") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("عنوان") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("متن خبر یا مطلب") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
        item {
            FilledTonalButton(
                onClick = {
                    onAction(MainAction.SubmitByEmail(context, subject, body, senderEmail))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = subject.isNotBlank() && body.isNotBlank()
            ) {
                Text("ارسال به info@rojman.org")
            }
        }
    }
}
