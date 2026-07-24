package com.finnflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmPaper

/** A single selectable option shown in an [OptionPickerSheet]. */
data class PickerOption(
    val value: String,
    val label: String,
    val subtitle: String? = null
)

/**
 * Reusable bottom sheet listing labeled options with the currently selected
 * one checked. Used for both the Currency and Appearance settings rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPickerSheet(
    options: List<PickerOption>,
    selectedValue: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmPaper
    ) {
        LazyColumn {
            items(options, key = { it.value }) { option ->
                val isSelected = option.value == selectedValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option.value) }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            option.label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = Ink
                        )
                        if (option.subtitle != null) {
                            Text(option.subtitle, fontSize = 12.sp, color = InkMedium)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Ink)
                    }
                }
                Divider(color = Rule, thickness = 0.5.dp)
            }
        }
    }
}
