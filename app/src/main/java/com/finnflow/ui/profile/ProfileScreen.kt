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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.finnflow.ui.theme.ExpenseClay
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper

private fun fmtAmount(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "%,.0f".format(amount)
    else "%,.2f".format(amount)

private val IconEmail      = Color(0xFF3A6EA5)
private val IconCloud      = Color(0xFF2E8B94)
private val IconPrivacy    = Color(0xFF7A5C3E)
private val IconCalendar   = Color(0xFF7A4FA0)
private val IconCurrency   = Color(0xFF3E4A8A)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    fun enterEditMode() {
        draft = profile.displayName
        editing = true
    }

    fun commitEdit() {
        viewModel.saveName(draft)
        editing = false
    }

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
            Text("Profile", fontFamily = FontFamily.Serif, fontSize = 26.sp, color = Ink)
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
                                .background(IncomeGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profile.initials.ifBlank { "?" },
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(WarmPaper)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Ink)
                                .clickable { enterEditMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = WarmPaper,
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
                            val inkColor = Ink
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
                                    color = Ink,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(Ink),
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
                                    .background(Ink)
                                    .clickable { commitEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = WarmPaper,
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
                                color = Ink
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = InkMedium,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("imunir57@gmail.com", fontSize = 12.5.sp, color = InkMedium)
                }
            }

            // ── Stats triptych ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 18.dp)
                        .background(WarmCard, RoundedCornerShape(18.dp))
                        .border(1.dp, Rule, RoundedCornerShape(18.dp))
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCell(
                        label = "Income",
                        value = fmtAmount(uiState.totalIncome),
                        color = IncomeGreen,
                        currencyPrefix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Rule)
                    StatCell(
                        label = "Expense",
                        value = fmtAmount(uiState.totalExpense),
                        color = ExpenseClay,
                        currencyPrefix = "৳",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = Rule)
                    StatCell(
                        label = "Entries",
                        value = uiState.entryCount.toString(),
                        color = Ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Account section ────────────────────────────────────────────
            item { ProfileSectionHeader("Account") }
            item {
                ProfileRow(
                    icon = Icons.Default.Email,
                    iconColor = IconEmail,
                    label = "Email",
                    subtitle = "imunir57@gmail.com"
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Cloud,
                    iconColor = IconCloud,
                    label = "Cloud sync",
                    subtitle = "Sign in with Google to enable",
                    right = {
                        Box(
                            modifier = Modifier
                                .background(Rule, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "SOON",
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                color = InkMedium
                            )
                        }
                    }
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Shield,
                    iconColor = IconPrivacy,
                    label = "Privacy",
                    subtitle = "Your data is stored on this device only"
                )
            }

            // ── Preferences section ────────────────────────────────────────
            item { ProfileSectionHeader("Preferences") }
            item {
                ProfileRow(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = IconCalendar,
                    label = "Start of month",
                    right = { Text("1st", fontSize = 12.5.sp, color = InkMedium) }
                )
            }
            item {
                ProfileRow(
                    icon = Icons.Default.Payments,
                    iconColor = IconCurrency,
                    label = "Default currency",
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

            // ── Footer ─────────────────────────────────────────────────────
            item {
                Text(
                    "Member since January 2025",
                    fontSize = 11.5.sp,
                    color = InkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp)
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
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
            color = InkFaint
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
        color = InkMedium,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String? = null,
    right: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
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
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, fontSize = 11.5.sp, color = InkMedium)
            }
        }
        right?.invoke()
    }
}
