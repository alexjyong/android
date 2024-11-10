package chat.revolt.screens.chat.views

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.user.fetchSelf
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.NonIdealState
import chat.revolt.components.screens.settings.UserOverview
import chat.revolt.components.skeletons.UserOverviewSkeleton
import chat.revolt.internals.extensions.zero
import io.sentry.Sentry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(navController: NavController, useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    val context = LocalContext.current

    var isLoading by rememberSaveable { mutableStateOf(true) }
    var user by rememberSaveable { mutableStateOf<User?>(null) }
    LaunchedEffect(Unit) {
        val inCache = RevoltAPI.userCache[RevoltAPI.selfId]
        if (inCache != null) {
            user = inCache
            isLoading = false
        } else {
            try {
                fetchSelf().let {
                    user = it
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("OverviewScreen", "Failed to fetch self", e)
                Sentry.captureException(e)
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("You") },
                navigationIcon = {
                    if (useDrawer) {
                        IconButton(onClick = {
                            onDrawerClicked()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.menu)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.zero
            )
        },
    ) { pv ->
        if (user == null && !isLoading) {
            NonIdealState(
                icon = { size ->
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(size)
                    )
                },
                title = { Text(stringResource(R.string.overview_screen_error)) },
                description = { Text(stringResource(R.string.overview_screen_error_description)) }
            )
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 16.dp)
        ) {
            AnimatedContent(targetState = isLoading, label = "isLoading") { loading ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (loading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UserOverviewSkeleton(false)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        user?.let { user ->
                            UserOverview(user, internalPadding = false)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.size(48.dp))
            }

            AnimatedVisibility(
                visible = isLoading
            ) {
                Spacer(modifier = Modifier.height(48.dp))
            }

            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(300.dp),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "settings") {
                        OverviewScreenLink(
                            onClick = {
                                navController.navigate("settings")
                            },
                            backgroundColour = MaterialTheme.colorScheme.primaryContainer,
                            foregroundColour = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(stringResource(R.string.overview_screen_settings))
                                }
                            },
                            body = { Text(stringResource(R.string.overview_screen_settings_description)) }
                        )
                    }
                    item(key = "shareProfile") {
                        OverviewScreenLink(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.comingsoon_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                                // navController.navigate("userCard")
                            },
                            backgroundColour = MaterialTheme.colorScheme.tertiaryContainer,
                            foregroundColour = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_share_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(stringResource(R.string.overview_screen_share_profile))
                                }
                            },
                            body = { Text(stringResource(R.string.overview_screen_share_profile_description)) }
                        )
                    }

                    item(key = "changelog") {
                        OverviewScreenLink(
                            onClick = {
                                navController.navigate("settings/changelogs")
                            },
                            backgroundColour = MaterialTheme.colorScheme.errorContainer,
                            foregroundColour = MaterialTheme.colorScheme.onErrorContainer,
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_flask_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(stringResource(R.string.overview_screen_changelog))
                                }
                            },
                            body = { Text(stringResource(R.string.overview_screen_changelog_description)) }
                        )
                    }
                    item(key = "feedback") {
                        OverviewScreenLink(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.comingsoon_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                                // navController.navigate("feedback")
                            },
                            backgroundColour = MaterialTheme.colorScheme.primary,
                            foregroundColour = MaterialTheme.colorScheme.onPrimary,
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_creation_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(stringResource(R.string.overview_screen_feedback))
                                }
                            },
                            body = { Text(stringResource(R.string.overview_screen_feedback_description)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewScreenLink(
    onClick: () -> Unit,
    backgroundColour: Color,
    foregroundColour: Color,
    title: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .background(backgroundColour)
            .padding(vertical = 32.dp)
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalContentColor provides foregroundColour) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    title()
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    body()
                }
            }
        }
    }
}