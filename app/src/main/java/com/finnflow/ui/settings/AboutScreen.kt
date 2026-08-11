package com.finnflow.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.finnflow.BuildConfig
import com.finnflow.R

/** Size the launcher icon is drawn at — also the size it gets rasterized to. */
private val AppIconSize = 76.dp

@Composable
fun AboutScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Text(
                "About",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The app's own launcher icon, not a stand-in glyph. It ships its own background and
            // corner radius, so it is clipped to match rather than dropped onto a tinted circle.
            //
            // Rasterized through the drawable rather than loaded with painterResource: from API
            // 26 the launcher icon resolves to the <adaptive-icon> XML, which painterResource
            // rejects outright ("Only VectorDrawables and rasterized asset types are supported").
            // AdaptiveIconDrawable masks its own layers when it draws, so the bitmap already has
            // the icon's shape.
            val context = LocalContext.current
            val iconSizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
            val appIcon = remember(context, iconSizePx) {
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                    ?.toBitmap(width = iconSizePx, height = iconSizePx)
                    ?.asImageBitmap()
            }
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(AppIconSize)
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                // Nothing to draw is better than nothing at all: the screen keeps its shape and
                // the version details below stay where they are.
                Box(
                    modifier = Modifier
                        .size(AppIconSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "FinnFlow",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Version ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "FinnFlow · made for keeping count",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        ) {
            Text(
                "© ${java.time.Year.now().value} FinnFlow. All rights reserved.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}
