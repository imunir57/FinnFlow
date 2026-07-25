package com.finnflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnflow.BuildConfig
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper

@Composable
fun AboutScreen(
    onBack: () -> Unit = {}
) {
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
                "About",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "FinnFlow",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Version ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                fontSize = 13.sp,
                color = InkMedium
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "FinnFlow · made for keeping count",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = InkFaint,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(WarmCard)
                .border(1.dp, Rule, RoundedCornerShape(18.dp))
        ) {
            Text(
                "© ${java.time.Year.now().value} FinnFlow. All rights reserved.",
                fontSize = 11.5.sp,
                color = InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}
