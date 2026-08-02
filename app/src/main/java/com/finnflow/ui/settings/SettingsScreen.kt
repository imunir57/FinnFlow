package com.finnflow.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Currency
import com.finnflow.data.profile.UserProfile
import com.finnflow.ui.components.ConfirmationDialog
import com.finnflow.ui.components.OptionPickerSheet
import com.finnflow.ui.components.PickerOption
import com.finnflow.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "SettingsScreen"


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()
    val appLockMessage by viewModel.appLockMessage.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) {
            SecureLogger.d(TAG, "CSV export cancelled by user")
            return@rememberLauncherForActivityResult
        }
        SecureLogger.d(TAG, "CSV export file selected")
        val outputStream = context.contentResolver.openOutputStream(uri) ?: run {
            SecureLogger.w(TAG, "Failed to open output stream for CSV export")
            return@rememberLauncherForActivityResult
        }
        pendingExportUri = uri
        viewModel.exportCsv(outputStream) {
            val savedUri = pendingExportUri ?: return@exportCsv
            SecureLogger.d(TAG, "CSV export completed, opening share sheet")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, savedUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share transactions CSV"))
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            SecureLogger.d(TAG, "Notification permission granted")
            viewModel.onNotificationsToggled(true)
        } else {
            SecureLogger.w(TAG, "Notification permission denied by user")
        }
    }

    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showAppearancePicker by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            SecureLogger.d(TAG, "Backup file location selected, initiating backup")
            viewModel.performBackup(context.contentResolver.openOutputStream(uri))
        } else {
            SecureLogger.d(TAG, "Backup operation cancelled by user")
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            SecureLogger.d(TAG, "Restore file selected, initiating restore operation")
            viewModel.performRestore(context.contentResolver.openInputStream(uri))
        } else {
            SecureLogger.d(TAG, "Restore operation cancelled by user")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showRestoreConfirmation) {
        ConfirmationDialog(
            title = "Restore from backup",
            message = "This replaces all current data — continue?",
            confirmLabel = "Restore",
            onConfirm = {
                SecureLogger.d(TAG, "User confirmed restore operation")
                showRestoreConfirmation = false
                restoreLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = {
                SecureLogger.d(TAG, "User cancelled restore confirmation")
                showRestoreConfirmation = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Settings",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item { ProfileCard(profile = profile, onClick = onNavigateToProfile) }

            item { SectionHeader("Manage") }
            item {
                SettingsRow(
                    icon = Icons.Default.Category,
                    iconColor = FinnFlowTheme.colors.adapt(AccentCategories),
                    label = "Categories",
                    subtitle = "Add, edit, reorder categories",
                    onClick = {
                        SecureLogger.d(TAG, "User navigated to Categories")
                        onNavigateToCategories()
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Payments,
                    iconColor = FinnFlowTheme.colors.adapt(AccentCurrency),
                    label = "Currency",
                    right = {
                        Text(
                            profile.currencySymbol,
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        SecureLogger.d(TAG, "User opened currency picker")
                        showCurrencyPicker = true
                    }
                )
            }
            item {
                ToggleRow(
                    icon = Icons.Default.Notifications,
                    iconColor = FinnFlowTheme.colors.adapt(AccentNotify),
                    label = "Notifications",
                    subtitle = "Daily reminder · 9:00 PM",
                    checked = profile.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        SecureLogger.d(TAG, "Notifications toggle changed: enabled=$enabled")
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SecureLogger.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onNotificationsToggled(enabled)
                        }
                    }
                )
            }

            item { SectionHeader("Data") }
            item {
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    iconColor = FinnFlowTheme.colors.adapt(AccentBackup),
                    label = "Backup",
                    subtitle = formatBackupTimestamp(profile.lastBackupTimestamp),
                    onClick = {
                        SecureLogger.d(TAG, "User initiated backup operation")
                        backupLauncher.launch("finnflow_backup.json")
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    iconColor = FinnFlowTheme.colors.adapt(AccentRestore),
                    label = "Restore",
                    subtitle = "From a previous backup file",
                    onClick = {
                        SecureLogger.d(TAG, "User tapped restore, showing confirmation dialog")
                        showRestoreConfirmation = true
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Share,
                    iconColor = FinnFlowTheme.colors.adapt(AccentExport),
                    label = "Export to CSV",
                    subtitle = "Share your transactions as a spreadsheet",
                    onClick = {
                        SecureLogger.d(TAG, "User initiated CSV export")
                        exportCsvLauncher.launch("finnflow-transactions-${LocalDate.now()}.csv")
                    }
                )
            }

            item { SectionHeader("App") }
            item {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    iconColor = FinnFlowTheme.colors.adapt(AccentAppearance),
                    label = "Appearance",
                    right = {
                        Text(themeModeLabel(profile.themeMode), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    onClick = {
                        SecureLogger.d(TAG, "User opened appearance/theme picker")
                        showAppearancePicker = true
                    }
                )
            }
            item {
                ToggleRow(
                    icon = Icons.Default.Lock,
                    iconColor = FinnFlowTheme.colors.adapt(AccentAppLock),
                    label = "App Lock",
                    subtitle = appLockMessage ?: "Require fingerprint to open",
                    checked = profile.appLockEnabled,
                    onCheckedChange = { enabled ->
                        SecureLogger.d(TAG, "App Lock toggle changed: enabled=$enabled")
                        viewModel.onAppLockToggled(enabled, activity)
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconColor = FinnFlowTheme.colors.adapt(AccentAbout),
                    label = "About FinnFlow",
                    subtitle = "Version ${com.finnflow.BuildConfig.VERSION_NAME} · Build ${com.finnflow.BuildConfig.VERSION_CODE}",
                    onClick = {
                        SecureLogger.d(TAG, "User navigated to About screen")
                        onNavigateToAbout()
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        SecureLogger.d(TAG, "User tapped Sign out button")
                        viewModel.onSignOut(context)
                    },
                    enabled = profile.isSignedIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinnFlowTheme.colors.expense)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (showCurrencyPicker) {
        OptionPickerSheet(
            options = Currency.entries.map { PickerOption(it.code, "${it.symbol}  ${it.displayName}") },
            selectedValue = profile.currencyCode,
            onOptionSelected = { code ->
                SecureLogger.d(TAG, "User selected currency: $code")
                viewModel.onCurrencySelected(code)
                showCurrencyPicker = false
            },
            onDismiss = {
                SecureLogger.d(TAG, "Currency picker dismissed")
                showCurrencyPicker = false
            }
        )
    }

    if (showAppearancePicker) {
        OptionPickerSheet(
            options = listOf(
                PickerOption("system", "System"),
                PickerOption("light", "Light"),
                PickerOption("dark", "Dark")
            ),
            selectedValue = profile.themeMode,
            onOptionSelected = { mode ->
                SecureLogger.d(TAG, "User selected theme mode: $mode")
                viewModel.onThemeModeSelected(mode)
                showAppearancePicker = false
            },
            onDismiss = {
                SecureLogger.d(TAG, "Appearance picker dismissed")
                showAppearancePicker = false
            }
        )
    }
}

private fun themeModeLabel(mode: String): String = when (mode) {
    "light" -> "Light"
    "dark" -> "Dark"
    else -> "System"
}

private val backupDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatBackupTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "Never backed up"
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return "Last backup — ${date.format(backupDateFormatter)}"
}

@Composable
private fun ProfileCard(profile: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                profile.initials.ifBlank { "?" },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.displayName.ifBlank { "Your Name" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text("Tap to edit profile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            right != null -> right()
            onClick != null -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon = icon, color = iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                uncheckedThumbColor = MaterialTheme.colorScheme.background,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
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
