package com.finnflow.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.category.CategoryScreen
import com.finnflow.ui.category.SubCategoryScreen
import com.finnflow.ui.components.BottomNavBar
import com.finnflow.ui.home.HomeScreen
import com.finnflow.ui.stats.CategoryDetailScreen
import com.finnflow.ui.stats.StatsScreen
import com.finnflow.ui.transaction.TransactionFormScreen
import com.finnflow.ui.yearly.YearlyScreen
import java.time.LocalDate

@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onEditTransaction = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
