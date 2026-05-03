package com.finnflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.profile.UserProfile
import com.finnflow.ui.theme.ExpenseClay
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper

private val IconCategories   = Color(0xFF7A5C3E)
private val IconCurrency     = Color(0xFF3E4A8A)
private val IconNotify       = Color(0xFFB5456E)
private val IconBackup       = Color(0xFF3A6EA5)
private val IconRestore      = Color(0xFF2E8B94)
private val IconExport       = Color(0xFF7A4FA0)
private val IconAppearance   = Color(0xFFD18842)
private val IconAppLock      = Color(0xFF556B74)
private val IconAbout        = Color(0xFF6E8A4A)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkMedium)
            }
            Text(
                "Settings",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item { ProfileCard(profile = profile, onClick = onNavigateToProfile) }

            item { SectionHeader("Manage") }
            item {
                SettingsRow(
                    icon = Icons.Default.Category,
                    iconColor = IconCategories,
                    label = "Categories",
                    subtitle = "Add, edit, reorder categories",
                    onClick = onNavigateToCategories
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Payments,
                    iconColor = IconCurrency,
                    label = "Currency",
                    right = {
                        Text(
                            "৳",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            color = InkMedium
                        )
                    }
                )
            }
            item {
                ToggleRow(
                    icon = Icons.Default.Notifications,
                    iconColor = IconNotify,
                    label = "Notifications",
                    subtitle = "Daily reminder · 9:00 PM",
                    initiallyOn = true
                )
            }

            item { SectionHeader("Data") }
            item {
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    iconColor = IconBackup,
                    label = "Backup",
                    subtitle = "Last backup — Apr 17, 2026"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    iconColor = IconRestore,
                    label = "Restore",
                    subtitle = "From a previous backup file"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Share,
                    iconColor = IconExport,
                    label = "Export to CSV",
                    subtitle = "Share your transactions as a spreadsheet"
                )
            }

            item { SectionHeader("App") }
            item {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    iconColor = IconAppearance,
                    label = "Appearance",
                    right = {
                        Text("System", fontSize = 12.5.sp, color = InkMedium)
                    }
                )
            }
            item {
                ToggleRow(
                    icon = Icons.Default.Lock,
                    iconColor = IconAppLock,
                    label = "App Lock",
                    subtitle = "Require fingerprint to open",
                    initiallyOn = false
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconColor = IconAbout,
                    label = "About FinnFlow",
                    subtitle = "Version 1.0.0 · Build 102"
                )
            }

            item {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseClay)
                ) {
                    Text(
                        "Sign out",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Text(
                    "FinnFlow · made for keeping count",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = InkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(WarmCard)
            .border(1.dp, Rule, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(IncomeGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                profile.initials.ifBlank { "?" },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.displayName.ifBlank { "Your Name" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(Modifier.height(2.dp))
            Text("Tap to edit profile", fontSize = 12.sp, color = InkMedium)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = InkMedium,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = InkMedium,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String? = null,
    right: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp)
    else
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon = icon, color = iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = InkMedium)
            }
        }
        when {
            right != null -> right()
            onClick != null -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = InkMedium,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String? = null,
    initiallyOn: Boolean = false
) {
    var on by remember { mutableStateOf(initiallyOn) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon = icon, color = iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = InkMedium)
            }
        }
        Switch(
            checked = on,
            onCheckedChange = { on = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = WarmPaper,
                checkedTrackColor = Ink,
                uncheckedThumbColor = WarmPaper,
                uncheckedTrackColor = Rule
            )
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}
