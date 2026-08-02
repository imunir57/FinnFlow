package com.finnflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnflow.ui.Screen
import com.finnflow.ui.theme.FinnFlowTheme

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Stats, "Stats", Icons.Default.PieChart),
    BottomNavItem(Screen.Yearly, "Yearly", Icons.Default.CalendarMonth),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

// Sized down from the Material defaults (24.dp icon / 12.sp label) so four tabs sit
// comfortably on a ~360.dp screen without the labels wrapping or ellipsizing. The
// NavigationBarItem indicator pill wraps the icon, so it shrinks along with it.
private val NavIconSize = 21.dp
private val NavLabelSize = 10.5.sp

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = FinnFlowTheme.colors.navPill,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                selected = currentRoute == item.screen.route,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(NavIconSize)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = NavLabelSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            )
        }
    }
}
