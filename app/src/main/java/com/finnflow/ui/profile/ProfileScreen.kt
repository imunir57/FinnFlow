package com.finnflow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

private const val TAG = "ProfileScreen"

private val memberSinceFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val money = LocalCurrencyFormat.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    // Sign-in state comes from isSignedIn, never from email — an ID token without an email
    // claim would otherwise render "Not signed in" next to a signed-in checkmark.
    val accountLabel = when {
        !profile.isSignedIn -> "Not signed in"
        profile.email.isNotBlank() -> profile.email
        else -> "Signed in with Google"
    }
    val cloudSyncSubtitle = when {
        !profile.isSignedIn -> "Sign in with Google to enable"
        profile.email.isNotBlank() -> "Signed in as ${profile.email}"
        else -> "Signed in with Google"
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun enterEditMode() {
        SecureLogger.d(TAG, "User entered profile name edit mode")
        draft = profile.displayName
        editing = true
    }

    fun commitEdit() {
        SecureLogger.d(TAG, "User committed profile name edit, length=${draft.length}")
        focusManager.clearFocus()
        viewModel.saveName(draft)
        editing = false
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Profile", fontFamily = FontFamily.Serif, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Avatar + name ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 14.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(98.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profile.initials.ifBlank { "?" },
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { enterEditMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (editing) {
                        Row(
                            modifier = Modifier.widthIn(max = 280.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val inkColor = MaterialTheme.colorScheme.onSurface
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .drawBehind {
                                        drawLine(
                                            color = inkColor,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                    .padding(bottom = 4.dp),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { commitEdit() })
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { commitEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.clickable { enterEditMode() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                profile.displayName.ifBlank { "Your Name" },
                                fontFamily = FontFamily.Serif,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(accountLabel, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Stats triptych ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 18.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCell(
                        label = "Income",
                        value = money.amount(uiState.totalIncome),
                        color = FinnFlowTheme.colors.income,
                        currencyPrefix = money.symbol,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
                    StatCell(
                        label = "Expense",
                        value = money.amount(uiState.totalExpense),
                        color = FinnFlowTheme.colors.expense,
                        currencyPrefix = money.symbol,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
                    StatCell(
                        label = "Entries",
                        value = uiState.entryCount.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Account section ────────────────────────────────────────────
            item { ProfileSectionHeader("Account") }
            item {
                ProfileRow(
                    icon = Icons.Default.Email,
                    iconColor = FinnFlowTheme.colors.adapt(AccentEmail),
                    label = "Email",
                    subtitle = accountLabel
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Cloud,
                    iconColor = FinnFlowTheme.colors.adapt(AccentCloud),
                    label = "Cloud sync",
                    subtitle = cloudSyncSubtitle,
                    right = if (profile.isSignedIn) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FinnFlowTheme.colors.adapt(AccentCloud),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "SIGN IN",
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = if (profile.isSignedIn) null else {
                        {
                            SecureLogger.d(TAG, "User tapped Google Sign-In button")
                            viewModel.onSignInWithGoogle(context)
                        }
                    }
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Shield,
                    iconColor = FinnFlowTheme.colors.adapt(AccentPrivacy),
                    label = "Privacy",
                    subtitle = "Your data is stored on this device only"
                )
            }

            // ── Preferences section ────────────────────────────────────────
            item { ProfileSectionHeader("Preferences") }
            item {
                ProfileRow(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = FinnFlowTheme.colors.adapt(AccentCalendar),
                    label = "Start of month",
                    right = { Text("1st", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Payments,
                    iconColor = FinnFlowTheme.colors.adapt(AccentCurrency),
                    label = "Default currency",
                    right = {
                        Text(
                            money.symbol,
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // ── Footer ─────────────────────────────────────────────────────
            // Omitted rather than guessed at when there is nothing to date the account from.
            uiState.memberSince?.let { memberSince ->
                item {
                    Text(
                        "Member since ${memberSince.format(memberSinceFormatter)}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    color: Color,
    currencyPrefix: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (currencyPrefix != null) {
                Text(
                    currencyPrefix,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = color.copy(alpha = 0.55f),
                    modifier = Modifier.padding(end = 1.dp, bottom = 1.dp)
                )
            }
            Text(
                value,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                color = color,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
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
private fun ProfileRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String? = null,
    right: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp)
    } else {
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        right?.invoke()
    }
}
