package com.finnflow.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.WarmPaper

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as? FragmentActivity

    fun promptAuth() {
        if (activity != null) {
            viewModel.authenticate(
                activity = activity,
                onSuccess = onUnlocked,
                onError = { /* stay locked; user can retry */ }
            )
        }
    }

    LaunchedEffect(Unit) { promptAuth() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Fingerprint,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.height(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "FinnFlow is locked",
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Authenticate to continue",
            fontSize = 13.sp,
            color = InkMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { promptAuth() },
            colors = ButtonDefaults.buttonColors(containerColor = Ink)
        ) {
            Text("Unlock")
        }
    }
}
