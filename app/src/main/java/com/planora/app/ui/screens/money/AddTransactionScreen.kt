package com.planora.app.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.database.entities.EXPENSE_CATEGORIES
import com.planora.app.data.database.entities.INCOME_CATEGORIES
import com.planora.app.data.database.entities.Transaction
import com.planora.app.data.database.entities.TransactionType
import com.planora.app.theme.ExpenseRed
import com.planora.app.theme.IncomeGreen
import com.planora.app.ui.components.*
import com.planora.app.ui.viewmodels.MoneyViewModel
import kotlinx.coroutines.launch

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

    // Primary state tokens for the transaction form
    var amount       by remember { mutableStateOf("") }
    var type         by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category     by remember { mutableStateOf("Food") }
    var note         by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Lifecycle: Hydrate form with existing transaction data if in 'Edit' mode
    LaunchedEffect(transactionId) {
        if (isEditing) {
            viewModel.getTransactionById(transactionId)?.let { t ->
                amount = t.amount.toString(); type = t.type
                category = t.category; note = t.note; selectedDate = t.date
            }
        }
    }

    PlanoraScreen(
        title = if (isEditing) "Edit Transaction" else "Add Transaction",
        onBack = onBack,
        actionButtonLabel = if (isEditing) "Update" else "Add Transaction",
        onActionButtonClick = {
            // Validation: Ensure amount is a positive, non-zero numeric value
            val amt = amount.trim().toDoubleOrNull() ?: return@PlanoraScreen
            if (amt <= 0.0) return@PlanoraScreen
            
            // Intelligence: Fallback to context-aware default categories if blank
            val trimmedCategory = category.trim().ifBlank {
                if (type == TransactionType.INCOME) "Salary" else "Food"
            }
            val trimmedNote = note.trim()
            
            scope.launch {
                if (isEditing) {
                    val existing = viewModel.getTransactionById(transactionId) ?: return@launch
                    viewModel.updateTransaction(
                        existing.copy(
                            amount = amt, type = type, category = trimmedCategory, 
                            note = trimmedNote, date = selectedDate
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        Transaction(
                            amount = amt, type = type, category = trimmedCategory, 
                            note = trimmedNote, date = selectedDate
                        )
                    )
                }
                onBack()
            }
        }
    ) {
        // Dynamic Toggle: Expense vs Income prioritization
        PlanoraToggleGroup(
            options = listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income"),
            selectedOption = type,
            onOptionSelected = { 
                type = it
                // Intelligent default: Switch categories when type changes
                category = if (it == TransactionType.INCOME) "Salary" else "Food"
            },
            activeColor = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
        )

        PlanoraTextField(
            value = amount, onValueChange = { amount = it }, label = "Amount",
            leadingIcon = { 
                Text(sym, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) 
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Visual distinction for the Category selection area
        Text("Category", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        
        PlanoraChipSelector(
            options = if (type == TransactionType.INCOME) INCOME_CATEGORIES else EXPENSE_CATEGORIES,
            selectedOption = category,
            onOptionSelected = { category = it }
        )

        PlanoraTextField(
            value = note, onValueChange = { note = it },
            label = "Note (Optional)", singleLine = false, maxLines = 3
        )

        PlanoraDatePickerField(
            selectedDate = selectedDate,
            onDateSelected = { it?.let { selectedDate = it } },
            label = "Transaction Date",
            showClearButton = false
        )
    }
}
