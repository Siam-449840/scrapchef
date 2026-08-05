package com.scrapchef.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrapchef.android.ui.theme.LocalExtendedColors
import com.scrapchef.android.ui.theme.LocalScrapChefTokens

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person

sealed class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object AiChef : BottomTab(
        route = "landing",
        title = "AI Chef",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "tab_ai_chef"
    )

    data object Bookmarks : BottomTab(
        route = "favorites",
        title = "Saved",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "tab_bookmarks"
    )

    data object PantryGuide : BottomTab(
        route = "pantry",
        title = "Pantry",
        selectedIcon = Icons.Filled.Kitchen,
        unselectedIcon = Icons.Outlined.Kitchen,
        testTag = "tab_pantry_guide"
    )

    data object Profile : BottomTab(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "tab_profile"
    )
}

@Composable
fun PantryBottomBar(
    currentRoute: String,
    favoritesCount: Int,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalScrapChefTokens.current
    val extendedColors = LocalExtendedColors.current
    val tabs = remember { listOf(BottomTab.AiChef, BottomTab.Bookmarks, BottomTab.PantryGuide, BottomTab.Profile) }

    Surface(
        shape = RoundedCornerShape(tokens.Radius.xl),
        color = extendedColors.surfaceContainerHigh,
        shadowElevation = tokens.Elevation.lg,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = tokens.Spacing.lg, vertical = tokens.Spacing.md)
            .testTag("pantry_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.Spacing.sm, vertical = tokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route

                BottomTabItem(
                    tab = tab,
                    isSelected = isSelected,
                    favoritesCount = if (tab == BottomTab.Bookmarks) favoritesCount else 0,
                    onClick = {
                        if (currentRoute != tab.route) {
                            onTabSelected(tab.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: BottomTab,
    isSelected: Boolean,
    favoritesCount: Int,
    onClick: () -> Unit
) {
    val tokens = LocalScrapChefTokens.current

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        animationSpec = tween(durationMillis = 100),
        label = "tab_container"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 100),
        label = "tab_content"
    )

    val animatedIconSize by animateDpAsState(
        targetValue = if (isSelected) 22.dp else tokens.IconSize.md,
        animationSpec = tween(durationMillis = 100),
        label = "tab_icon_size"
    )

    Surface(
        shape = RoundedCornerShape(tokens.Radius.lg),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.Radius.lg))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .semantics { role = Role.Tab }
            .testTag(tab.testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(tokens.TouchTarget.min)
                .padding(horizontal = tokens.Spacing.lg)
        ) {
            val iconContent: @Composable () -> Unit = {
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.title,
                    tint = contentColor,
                    modifier = Modifier.size(animatedIconSize)
                )
            }

            if (tab == BottomTab.Bookmarks && favoritesCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text(
                                text = favoritesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                ) {
                    iconContent()
                }
            } else {
                iconContent()
            }

            // Simple visibility — no slide animation to avoid per-tab jank
            if (isSelected) {
                Row {
                    Spacer(modifier = Modifier.width(tokens.Spacing.sm))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}
