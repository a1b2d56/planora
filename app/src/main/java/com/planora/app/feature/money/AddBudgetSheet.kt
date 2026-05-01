package com.planora.app.feature.money

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.planora.app.core.data.database.entities.Budget
import com.planora.app.core.data.database.entities.EXPENSE_CATEGORIES
import com.planora.app.core.ui.components.PlanoraChipSelector
import com.planora.app.core.ui.components.PlanoraTextField

@Composable
fun AddBudgetSheet(
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var limitStr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Set Monthly Budget", style = MaterialTheme.typography.titleLarge)
        
        Text("Category", style = MaterialTheme.typography.labelMedium)
        PlanoraChipSelector(
            options = EXPENSE_CATEGORIES,
            selectedOption = category,
            onOptionSelected = { category = it }
        )

        PlanoraTextField(
            value = limitStr,
            onValueChange = { limitStr = it },
            label = "Monthly Limit",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Button(
            onClick = {
                val amt = limitStr.trim().toDoubleOrNull() ?: return@Button
                if (amt <= 0.0) return@Button
                viewModel.upsert(
                    Budget(
                        category = category,
                        monthlyLimit = amt,
                        month = viewModel.currentMonth,
                        year = viewModel.currentYear
                    )
                )
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Save Budget") }
    }
}
