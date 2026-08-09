package com.finnflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The icon vocabulary a category can be given, and the lookup from a stored `iconName`.
 *
 * Shared rather than screen-local because more than one screen draws a category's icon — the
 * Categories editor picks from [CATEGORY_ICON_KEYS], while Compare and any other consumer only
 * needs [categoryIconFor] to resolve what was stored.
 */
val CATEGORY_ICON_KEYS = listOf(
    "utensils", "car", "home", "heart", "book", "bag",
    "film", "phone", "sparkle", "gift", "bank", "wallet",
    "briefcase", "laptop", "trending", "dots"
)

val CATEGORY_ICON_MAP: Map<String, ImageVector> = mapOf(
    "utensils"  to Icons.Default.Restaurant,
    "car"       to Icons.Default.DirectionsCar,
    "home"      to Icons.Default.Home,
    "heart"     to Icons.Default.Favorite,
    "book"      to Icons.AutoMirrored.Filled.MenuBook,
    "bag"       to Icons.Default.ShoppingBag,
    "film"      to Icons.Default.Movie,
    "phone"     to Icons.Default.Phone,
    "sparkle"   to Icons.Default.AutoAwesome,
    "gift"      to Icons.Default.CardGiftcard,
    "bank"      to Icons.Default.AccountBalance,
    "wallet"    to Icons.Default.AccountBalanceWallet,
    "briefcase" to Icons.Default.Work,
    "laptop"    to Icons.Default.Laptop,
    "trending"  to Icons.AutoMirrored.Filled.TrendingUp,
    "dots"      to Icons.Default.MoreHoriz,
)

/** Falls back to a neutral glyph for an unset or unrecognised [key]. */
fun categoryIconFor(key: String?): ImageVector =
    if (key.isNullOrEmpty()) Icons.Default.MoreHoriz
    else CATEGORY_ICON_MAP[key] ?: Icons.Default.MoreHoriz
