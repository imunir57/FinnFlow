package com.finnflow.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Yearly : Screen("yearly")
    object Settings : Screen("settings")
    object AddTransaction : Screen("transaction/add")
    object EditTransaction : Screen("transaction/edit/{transactionId}") {
        fun createRoute(id: Long) = "transaction/edit/$id"
    }
    object Categories : Screen("categories")
    object SubCategories : Screen("subcategories/{categoryId}") {
        fun createRoute(categoryId: Long) = "subcategories/$categoryId"
    }
    object Onboarding : Screen("onboarding")
    object Profile : Screen("profile")
    object CategoryDetail : Screen("stats/category/{categoryId}/{from}/{to}/{type}") {
        fun createRoute(
            categoryId: Long,
            from: String,
            to: String,
            type: String
        ) = "stats/category/$categoryId/$from/$to/$type"
    }
}
