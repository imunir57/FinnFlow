package com.finnflow.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnflow.data.logger.SecureLogger
import com.finnflow.ui.category.CategoryScreen
import com.finnflow.ui.category.SubCategoryScreen
import com.finnflow.ui.components.BottomNavBar
import com.finnflow.ui.home.HomeScreen
import com.finnflow.ui.lock.AppLockScreen
import com.finnflow.ui.onboarding.OnboardingScreen
import com.finnflow.ui.profile.ProfileScreen
import com.finnflow.ui.settings.AboutScreen
import com.finnflow.ui.settings.SettingsScreen
import com.finnflow.ui.insights.InsightsScreen
import com.finnflow.ui.stats.CategoryDetailScreen
import com.finnflow.ui.stats.StatsScreen
import com.finnflow.ui.theme.FinnFlowTheme
import com.finnflow.ui.transaction.TransactionFormScreen
import com.finnflow.ui.yearly.YearlyScreen

private const val TAG = "MainNavHost"
private val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Stats.route,
    Screen.Yearly.route,
    Screen.Settings.route
)

@Composable
fun MainNavHost(mainViewModel: MainViewModel = hiltViewModel()) {
    val onboardingDone by mainViewModel.hasCompletedOnboarding.collectAsState()
    val themeMode by mainViewModel.themeMode.collectAsState()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val appLockEnabled by mainViewModel.appLockEnabled.collectAsState()
    val isUnlocked by mainViewModel.isUnlocked.collectAsState()
    val currency by mainViewModel.currency.collectAsState()
    val currencyFormat = remember(currency) { CurrencyFormat(currency) }

    SecureLogger.d(TAG, "NavHost state - onboardingDone: $onboardingDone, appLockEnabled: $appLockEnabled, isUnlocked: $isUnlocked, themeMode: $themeMode")

    // Users who enable App Lock opt into the stricter posture: no screenshots and a
    // blanked-out recents thumbnail. Left off otherwise so screenshots keep working.
    val window = (LocalContext.current as? Activity)?.window
    LaunchedEffect(appLockEnabled, window) {
        if (window == null) return@LaunchedEffect
        if (appLockEnabled) {
            SecureLogger.d(TAG, "Setting FLAG_SECURE for app lock")
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            SecureLogger.d(TAG, "Clearing FLAG_SECURE - app lock disabled")
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Wait until DataStore is read before rendering anything
    if (onboardingDone == null) {
        SecureLogger.d(TAG, "DataStore still loading, deferring render")
        return
    }

    // Currency is ambient presentation context, exactly like the theme: resolved once here
    // from the profile rather than threaded through every ViewModel and composable.
    CompositionLocalProvider(LocalCurrencyFormat provides currencyFormat) {
    FinnFlowTheme(darkTheme = isDarkTheme) {
    if (appLockEnabled && !isUnlocked) {
        SecureLogger.d(TAG, "Rendering AppLockScreen - app locked")
        AppLockScreen(onUnlocked = mainViewModel::onUnlocked)
        return@FinnFlowTheme
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    LaunchedEffect(currentRoute) {
        SecureLogger.d(TAG, "Navigation: $currentRoute, showBottomBar: $showBottomBar")
    }

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone == true) Screen.Home.route else Screen.Onboarding.route,
            // `padding` offsets the content but does not consume the insets it came from, so a
            // screen with its own Scaffold would apply the status/navigation bar inset a second
            // time and sit too low. Consuming here fixes every such screen at once.
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            composable(Screen.Onboarding.route) {
                SecureLogger.d(TAG, "Navigating to Onboarding")
                OnboardingScreen(
                    onFinished = {
                        SecureLogger.d(TAG, "Onboarding finished, navigating to Home")
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                SecureLogger.d(TAG, "Navigating to Home")
                HomeScreen(
                    onAddTransaction = {
                        SecureLogger.d(TAG, "User navigating to AddTransaction")
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onEditTransaction = { id ->
                        SecureLogger.d(TAG, "User navigating to EditTransaction for id=$id")
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    },
                    onNavigateToProfile = {
                        SecureLogger.d(TAG, "User navigating to Profile")
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                SecureLogger.d(TAG, "Navigating to Stats")
                StatsScreen(
                    onNavigateToCategory = { categoryId, from, to, type ->
                        SecureLogger.d(TAG, "User navigating to CategoryDetail for categoryId=$categoryId")
                        navController.navigate(
                            Screen.CategoryDetail.createRoute(
                                categoryId = categoryId,
                                from = from.toString(),
                                to = to.toString(),
                                type = type.name
                            )
                        )
                    },
                    onNavigateToInsights = {
                        SecureLogger.d(TAG, "User navigating to Insights")
                        navController.navigate(Screen.Insights.route)
                    }
                )
            }

            composable(Screen.Insights.route) {
                SecureLogger.d(TAG, "Navigating to Insights")
                InsightsScreen(onBack = {
                    SecureLogger.d(TAG, "User navigating back from Insights")
                    navController.popBackStack()
                })
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
                SecureLogger.d(TAG, "Navigating to CategoryDetail: $categoryName")
                CategoryDetailScreen(
                    categoryName = categoryName,
                    onNavigateBack = {
                        SecureLogger.d(TAG, "User navigating back from CategoryDetail")
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Yearly.route) {
                SecureLogger.d(TAG, "Navigating to Yearly")
                YearlyScreen()
            }

            composable(Screen.Settings.route) {
                SecureLogger.d(TAG, "Navigating to Settings")
                SettingsScreen(
                    onNavigateToCategories = {
                        SecureLogger.d(TAG, "User navigating to Categories from Settings")
                        navController.navigate(Screen.Categories.route)
                    },
                    onNavigateToProfile = {
                        SecureLogger.d(TAG, "User navigating to Profile from Settings")
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToAbout = {
                        SecureLogger.d(TAG, "User navigating to About from Settings")
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            composable(Screen.Profile.route) {
                SecureLogger.d(TAG, "Navigating to Profile")
                ProfileScreen(onBack = {
                    SecureLogger.d(TAG, "User navigating back from Profile")
                    navController.popBackStack()
                })
            }

            composable(Screen.About.route) {
                SecureLogger.d(TAG, "Navigating to About")
                AboutScreen(onBack = {
                    SecureLogger.d(TAG, "User navigating back from About")
                    navController.popBackStack()
                })
            }

            composable(Screen.Categories.route) {
                SecureLogger.d(TAG, "Navigating to Categories")
                CategoryScreen(
                    onNavigateBack = {
                        SecureLogger.d(TAG, "User navigating back from Categories")
                        navController.popBackStack()
                    },
                    onNavigateToSubCategories = { catId ->
                        SecureLogger.d(TAG, "User navigating to SubCategories for catId=$catId")
                        navController.navigate(Screen.SubCategories.createRoute(catId))
                    }
                )
            }

            composable(Screen.AddTransaction.route) {
                SecureLogger.d(TAG, "Navigating to AddTransaction")
                TransactionFormScreen(onNavigateBack = {
                    SecureLogger.d(TAG, "User navigating back from AddTransaction")
                    navController.popBackStack()
                })
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { this.type = NavType.LongType })
            ) {
                SecureLogger.d(TAG, "Navigating to EditTransaction")
                TransactionFormScreen(onNavigateBack = {
                    SecureLogger.d(TAG, "User navigating back from EditTransaction")
                    navController.popBackStack()
                })
            }

            composable(
                route = Screen.SubCategories.route,
                arguments = listOf(navArgument("categoryId") { this.type = NavType.LongType })
            ) {
                SecureLogger.d(TAG, "Navigating to SubCategories")
                SubCategoryScreen(onNavigateBack = {
                    SecureLogger.d(TAG, "User navigating back from SubCategories")
                    navController.popBackStack()
                })
            }
        }
    }
    }
    }
}
