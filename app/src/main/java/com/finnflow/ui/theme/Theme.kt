package com.finnflow.ui.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun FinnFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmScheme,
        content = content
    )
}
