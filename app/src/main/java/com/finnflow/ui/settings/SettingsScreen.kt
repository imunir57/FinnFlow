package com.finnflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmPaper

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Rule)

        SettingsRow(title = "Categories", subtitle = "Manage income & expense categories", onClick = onNavigateToCategories)
        HorizontalDivider(color = Rule)
        SettingsRow(title = "Profile", subtitle = "Your personal information", onClick = {})
        HorizontalDivider(color = Rule)
        SettingsRow(title = "Currency", subtitle = "Set your preferred currency", onClick = {})
        HorizontalDivider(color = Rule)
        SettingsRow(title = "Backup", subtitle = "Backup and restore data", onClick = {})
        HorizontalDivider(color = Rule)
        SettingsRow(title = "About", subtitle = "App version and info", onClick = {})
        HorizontalDivider(color = Rule)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = InkFaint)
        }
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = InkMedium,
            modifier = Modifier.size(16.dp)
        )
    }
}
