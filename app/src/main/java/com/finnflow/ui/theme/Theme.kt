package com.finnflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmScheme = lightColorScheme(
    primary              = Ink,
    onPrimary            = WarmPaper,
    primaryContainer     = WarmCard,
    onPrimaryContainer   = Ink,
    secondary            = IncomeGreen,
    onSecondary          = WarmPaper,
    secondaryContainer   = Color(0xFFCCE8D6),
    onSecondaryContainer = Color(0xFF0D3D21),
    tertiary             = ExpenseClay,
    onTertiary           = WarmPaper,
    tertiaryContainer    = Color(0xFFF5D5CC),
    onTertiaryContainer  = Color(0xFF4A1508),
    background           = WarmPaper,
    onBackground         = Ink,
    surface              = WarmPaper,
    onSurface            = Ink,
    surfaceVariant       = WarmCard,
    onSurfaceVariant     = InkMedium,
    outline              = Rule,
    outlineVariant       = Rule,
    error                = ExpenseClay,
    onError              = WarmPaper,
    errorContainer       = Color(0xFFF5D5CC),
    onErrorContainer     = Color(0xFF4A1508),
)

private val DarkScheme = darkColorScheme(
    primary              = IvoryInk,
    onPrimary            = DarkPaper,
    primaryContainer     = DarkCard,
    onPrimaryContainer   = IvoryInk,
    secondary            = IncomeGreenDark,
    onSecondary          = DarkPaper,
    secondaryContainer   = Color(0xFF14361F),
    onSecondaryContainer = Color(0xFFCCE8D6),
    tertiary             = ExpenseClayDark,
    onTertiary           = DarkPaper,
    tertiaryContainer    = Color(0xFF4A1508),
    onTertiaryContainer  = Color(0xFFF5D5CC),
    background           = DarkPaper,
    onBackground         = IvoryInk,
    surface              = DarkPaper,
    onSurface            = IvoryInk,
    surfaceVariant       = DarkCard,
    onSurfaceVariant     = IvoryInkMedium,
    outline              = DarkRule,
    outlineVariant       = DarkRule,
    error                = ExpenseClayDark,
    onError              = DarkPaper,
    errorContainer       = Color(0xFF4A1508),
    onErrorContainer     = Color(0xFFF5D5CC),
)

/**
 * @param darkTheme resolved dark/light flag. Defaults to the system setting so
 * existing call sites keep working; callers that support the in-app theme
 * picker (system/light/dark) should resolve [darkTheme] themselves and pass
 * it explicitly.
 */
@Composable
fun FinnFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else WarmScheme,
        content = content
    )
}
