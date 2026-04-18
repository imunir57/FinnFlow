package com.finnflow.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.note.isEmpty() && state.amount.isEmpty()) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction type selector
            TypeSelector(selectedType = state.type, onTypeSelected = viewModel::onTypeChange)

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                isError = state.amountError != null,
                supportingText = { state.amountError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date picker field
            DateField(date = state.date, onDateChange = viewModel::onDateChange)

            // Category dropdown
            if (state.categories.isNotEmpty()) {
                DropdownField(
                    label = "Category",
                    options = state.categories.map { it.id to it.name },
                    selectedId = state.categoryId,
                    onSelected = viewModel::onCategoryChange
                )
            }

            // SubCategory dropdown (only shown if subcategories exist for selected category)
            if (state.subCategories.isNotEmpty()) {
                DropdownField(
                    label = "Sub-category (optional)",
                    options = listOf(null to "— None —") + state.subCategories.map { it.id to it.name },
                    selectedId = state.subCategoryId,
                    onSelected = viewModel::onSubCategoryChange
                )
            }

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = state.isValid && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp))
                else Text("Save")
            }
        }
    }
}

@Composable
fun TypeSelector(selectedType: TransactionType, onTypeSelected: (TransactionType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
        onValueChange = {},
        label = { Text("Date") },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) { Text("Change") }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<Pair<Long?, String>>,
    selectedId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelected(id!!); expanded = false }
                )
            }
        }
    }
}
