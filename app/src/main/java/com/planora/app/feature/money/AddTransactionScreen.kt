package com.planora.app.feature.money

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.core.data.database.entities.EXPENSE_CATEGORIES
import com.planora.app.core.data.database.entities.INCOME_CATEGORIES
import com.planora.app.core.data.database.entities.INVESTMENT_CATEGORIES
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.ui.components.*
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
    val accounts = uiState.accounts

    var amount       by remember { mutableStateOf("") }
    var type         by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category     by remember { mutableStateOf("Food") }
    var note         by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var accountId    by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(accounts) {
        if (accountId == -1L && accounts.isNotEmpty()) {
            accountId = accounts.firstOrNull { it.isDefault }?.id ?: accounts.first().id
        }
    }

    LaunchedEffect(transactionId) {
        if (isEditing) {
            viewModel.getTransactionById(transactionId)?.let { t ->
                amount = t.amount.toString(); type = t.type
                category = t.category; note = t.note; selectedDate = t.date
                accountId = t.accountId
            }
        }
    }

    PlanoraScreen(
        title = if (isEditing) "Edit Transaction" else "Add Transaction",
        onBack = onBack,
        actionButtonLabel = if (isEditing) "Update" else "Add Transaction",
        onActionButtonClick = {
            val amt = amount.trim().toDoubleOrNull() ?: return@PlanoraScreen
            if (amt <= 0.0) return@PlanoraScreen
            val finalAcc = if (accountId == -1L) 1L else accountId
            
            val trimmedCategory = category.trim().ifBlank { if (type == TransactionType.INCOME) "Salary" else "Food" }
            
            scope.launch {
                if (isEditing) {
                    val existing = viewModel.getTransactionById(transactionId) ?: return@launch
                    viewModel.updateTransaction(
                        existing.copy(amount = amt, type = type, category = trimmedCategory, note = note.trim(), date = selectedDate, accountId = finalAcc)
                    )
                } else {
                    viewModel.addTransaction(
                        Transaction(amount = amt, type = type, category = trimmedCategory, note = note.trim(), date = selectedDate, accountId = finalAcc)
                    )
                }
                onBack()
            }
        }
    ) {
        PlanoraToggleGroup(
            options = listOf(
                TransactionType.EXPENSE to "Expense", 
                TransactionType.INCOME to "Income",
                TransactionType.INVESTMENT to "Investment"
            ),
            selectedOption = type,
            onOptionSelected = { 
                type = it
                category = when(it) {
                    TransactionType.INCOME -> "Salary"
                    TransactionType.INVESTMENT -> "Stocks"
                    else -> "Food"
                }
            },
            activeColor = when(type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.INVESTMENT -> MaterialTheme.colorScheme.primary
                else -> ExpenseRed
            }
        )

        PlanoraTextField(
            value = amount, onValueChange = { amount = it }, label = "Amount",
            leadingIcon = { Text(sym, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        if (accounts.isNotEmpty()) {
            Text("Wallet", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { acc ->
                    CategoryChip(
                        label = acc.name,
                        selected = accountId == acc.id,
                        onClick = { accountId = acc.id }
                    )
                }
            }
        }

        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        PlanoraChipSelector(
            options = when(type) {
                TransactionType.INCOME -> INCOME_CATEGORIES
                TransactionType.INVESTMENT -> INVESTMENT_CATEGORIES
                else -> EXPENSE_CATEGORIES
            },
            selectedOption = category,
            onOptionSelected = { category = it }
        )

        PlanoraTextField(value = note, onValueChange = { note = it }, label = "Note (Optional)", maxLines = 3)

        PlanoraDatePickerField(
            selectedDate = selectedDate,
            onDateSelected = { it?.let { selectedDate = it } },
            label = "Transaction Date",
            showClearButton = false
        )
    }
}

