package com.finnflow.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnflow.ui.category.CategoryScreen
import com.finnflow.ui.category.SubCategoryScreen
import com.finnflow.ui.components.BottomNavBar
import com.finnflow.ui.home.HomeScreen
import com.finnflow.ui.onboarding.OnboardingScreen
import com.finnflow.ui.profile.ProfileScreen
import com.finnflow.ui.settings.SettingsScreen
import com.finnflow.ui.stats.CategoryDetailScreen
import com.finnflow.ui.stats.StatsScreen
import com.finnflow.ui.transaction.TransactionFormScreen
import com.finnflow.ui.yearly.YearlyScreen

private val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Stats.route,
    Screen.Yearly.route
)

@Composable
fun MainNavHost(mainViewModel: MainViewModel = hiltViewModel()) {
    val onboardingDone by mainViewModel.hasCompletedOnboarding.collectAsState()

    // Wait until DataStore is read before rendering anything
    if (onboardingDone == null) return

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone == true) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onEditTransaction = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateToCategory = { categoryId, from, to, type ->
                        navController.navigate(
                            Screen.CategoryDetail.createRoute(
                                categoryId = categoryId,
                                from = from.toString(),
                                to = to.toString(),
                                type = type.name
                            )
                        )
                    }
                )
            }

            composable(
                route = Screen.CategoryDetail.route,
                arguments = listOf(
                    navArgument("categoryId") { this.type = NavType.LongType },
                    navArgument("from") { this.type = NavType.StringType },
                    navArgument("to") { this.type = NavType.StringType },
                    navArgument("type") { this.type = NavType.StringType }
                )
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("type") ?: ""
                CategoryDetailScreen(
                    categoryName = categoryName,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Yearly.route) { YearlyScreen() }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubCategories = { catId ->
                        navController.navigate(Screen.SubCategories.createRoute(catId))
                    }
                )
            }

            composable(Screen.AddTransaction.route) {
                TransactionFormScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { this.type = NavType.LongType })
            ) {
                TransactionFormScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.SubCategories.route,
                arguments = listOf(navArgument("categoryId") { this.type = NavType.LongType })
            ) {
                SubCategoryScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
