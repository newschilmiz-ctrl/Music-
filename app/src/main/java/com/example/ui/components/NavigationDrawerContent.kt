package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomNavScreen
import com.example.data.model.LibraryTab

sealed class DrawerDestination {
    object Home : DrawerDestination()
    object AllSongs : DrawerDestination()
    object Downloads : DrawerDestination()
    object Albums : DrawerDestination()
    object Artists : DrawerDestination()
    object Playlists : DrawerDestination()
    object Favorites : DrawerDestination()
    object RecentlyPlayed : DrawerDestination()
    object Settings : DrawerDestination()
    object About : DrawerDestination()
}

@Composable
fun NavigationDrawerContent(
    currentDestination: DrawerDestination,
    onNavigate: (DrawerDestination) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("navigation_drawer_content")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Music Player",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Offline Audio Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Items as requested:
            // Home, All Songs, Downloads, Albums, Artists, Playlists, Favorites, Recently Played, Settings, About
            DrawerItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentDestination is DrawerDestination.Home,
                onClick = { onNavigate(DrawerDestination.Home) }
            )

            DrawerItem(
                label = "All Songs",
                icon = Icons.Default.MusicNote,
                selected = currentDestination is DrawerDestination.AllSongs,
                onClick = { onNavigate(DrawerDestination.AllSongs) }
            )

            DrawerItem(
                label = "Downloads",
                icon = Icons.Default.Download,
                selected = currentDestination is DrawerDestination.Downloads,
                onClick = { onNavigate(DrawerDestination.Downloads) }
            )

            DrawerItem(
                label = "Albums",
                icon = Icons.Default.Album,
                selected = currentDestination is DrawerDestination.Albums,
                onClick = { onNavigate(DrawerDestination.Albums) }
            )

            DrawerItem(
                label = "Artists",
                icon = Icons.Default.Person,
                selected = currentDestination is DrawerDestination.Artists,
                onClick = { onNavigate(DrawerDestination.Artists) }
            )

            DrawerItem(
                label = "Playlists",
                icon = Icons.Default.QueueMusic,
                selected = currentDestination is DrawerDestination.Playlists,
                onClick = { onNavigate(DrawerDestination.Playlists) }
            )

            DrawerItem(
                label = "Favorites",
                icon = Icons.Default.Favorite,
                selected = currentDestination is DrawerDestination.Favorites,
                onClick = { onNavigate(DrawerDestination.Favorites) }
            )

            DrawerItem(
                label = "Recently Played",
                icon = Icons.Default.History,
                selected = currentDestination is DrawerDestination.RecentlyPlayed,
                onClick = { onNavigate(DrawerDestination.RecentlyPlayed) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            DrawerItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                selected = currentDestination is DrawerDestination.Settings,
                onClick = { onNavigate(DrawerDestination.Settings) }
            )

            DrawerItem(
                label = "About",
                icon = Icons.Default.Info,
                selected = currentDestination is DrawerDestination.About,
                onClick = { onNavigate(DrawerDestination.About) }
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("drawer_item_${label.lowercase().replace(" ", "_")}")
    )
}
