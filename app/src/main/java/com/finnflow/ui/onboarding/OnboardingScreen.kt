package com.finnflow.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.WarmPaper
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.navigateHome.collectLatest { onFinished() }
    }

    Scaffold(containerColor = WarmPaper) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Hero card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF1A2820),
                                0.4f to Color(0xFF1E1916),
                                1.0f to Color(0xFF241410)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(28.dp)
            ) {
                Text(
                    "৳",
                    fontSize = 120.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-20).dp)
                )
                Column {
                    Text(
                        "Welcome to",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "FinnFlow",
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Track your money, your way.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Text(
                "What should we call you?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Your name", color = InkFaint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.onGetStarted(name)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IncomeGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.onGetStarted(name) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = viewModel::onSkip) {
                Text("Skip for now", color = InkFaint, fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))

            // Future: Google Sign-In (for sync / backup)
            Text(
                "Sign in with Google coming soon — for sync & backup",
                fontSize = 11.sp,
                color = InkFaint,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
