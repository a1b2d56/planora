package com.devil.taskzio.ui.screens.money

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.taskzio.R
import com.devil.taskzio.data.database.entities.EXPENSE_CATEGORIES
import com.devil.taskzio.data.database.entities.INCOME_CATEGORIES
import com.devil.taskzio.data.database.entities.Transaction
import com.devil.taskzio.data.database.entities.TransactionType
import com.devil.taskzio.theme.ExpenseRed
import com.devil.taskzio.theme.IncomeGreen
import com.devil.taskzio.ui.components.CategoryChip
import com.devil.taskzio.ui.components.DetailTopBar
import com.devil.taskzio.ui.components.TaskzioTextField
import com.devil.taskzio.ui.viewmodels.MoneyViewModel
import com.devil.taskzio.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("AssignedValueIsNeverRead")
@Composable
fun AddTransactionScreen(
    transactionId: Long?,
    onBack: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val isEditing = transactionId != null && transactionId > 0L
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol

    var amount       by remember { mutableStateOf("") }
    var type         by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category     by remember { mutableStateOf("Food") }
    var note         by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (isEditing) {
            viewModel.getTransactionById(transactionId)?.let { t ->
                amount = t.amount.toString(); type = t.type
                category = t.category; note = t.note; selectedDate = t.date
            }
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    val dismissDatePicker = { showDatePicker = false }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = dismissDatePicker,
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDate = it }; dismissDatePicker() }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = dismissDatePicker) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(if (isEditing) "Edit Transaction" else "Add Transaction", onBack) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)
            .imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(8.dp))

            // Income / Expense toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (t, label) ->
                    val selected = type == t
                    val color = if (t == TransactionType.INCOME) IncomeGreen else ExpenseRed
                    Surface(modifier = Modifier.weight(1f).height(52.dp).clickable {
                        type = t; category = if (t == TransactionType.INCOME) "Salary" else "Food"
                    }, shape = RoundedCornerShape(14.dp),
                        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = if (selected) BorderStroke(1.5.dp, color) else null) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            TaskzioTextField(value = amount, onValueChange = { amount = it }, label = "Amount",
                leadingIcon = { Text(sym, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

            Text("Category", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(if (type == TransactionType.INCOME) INCOME_CATEGORIES else EXPENSE_CATEGORIES) { cat ->
                    CategoryChip(cat, category == cat) { category = cat }
                }
            }

            TaskzioTextField(value = note, onValueChange = { note = it },
                label = "Note (Optional)", singleLine = false, maxLines = 3)

            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_date_range), null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DateUtils.formatDate(selectedDate), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(
                onClick = {
                    val amt = amount.trim().toDoubleOrNull() ?: return@Button
                    if (amt <= 0.0) return@Button
                    val trimmedCategory = category.trim().ifBlank {
                        if (type == TransactionType.INCOME) "Salary" else "Food"
                    }
                    val trimmedNote = note.trim()
                    scope.launch {
                        if (isEditing) {
                            val existing = viewModel.getTransactionById(transactionId) ?: return@launch
                            viewModel.updateTransaction(
                                existing.copy(
                                    amount = amt,
                                    type = type,
                                    category = trimmedCategory,
                                    note = trimmedNote,
                                    date = selectedDate
                                )
                            )
                        } else {
                            viewModel.addTransaction(
                                Transaction(
                                    amount = amt,
                                    type = type,
                                    category = trimmedCategory,
                                    note = trimmedNote,
                                    date = selectedDate
                                )
                            )
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) { Text(if (isEditing) "Update" else "Add Transaction", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}
