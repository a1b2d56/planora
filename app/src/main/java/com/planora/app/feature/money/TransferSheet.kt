package com.planora.app.feature.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.ui.components.PlanoraCard
import com.planora.app.core.ui.components.PlanoraTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    onDismiss: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf(uiState.accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(uiState.accounts.getOrNull(1)) }
    
    // Sync initial selection
    LaunchedEffect(uiState.accounts) {
        if (fromAccount == null) fromAccount = uiState.accounts.firstOrNull()
        if (toAccount == null) toAccount = uiState.accounts.getOrNull(1)
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Transfer Funds", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            // From Account
            AccountSelector("From", fromAccount, uiState.accounts) { fromAccount = it }
            
            Icon(
                painterResource(R.drawable.ic_arrow_downward), null, 
                modifier = Modifier.align(Alignment.CenterHorizontally).size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // To Account
            AccountSelector("To", toAccount, uiState.accounts) { toAccount = it }

            PlanoraTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = "Amount",
                placeholder = "0.00",
                leadingIcon = { Icon(painterResource(R.drawable.ic_attach_money), null, modifier = Modifier.size(20.dp)) }
            )

            PlanoraTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (Optional)",
                placeholder = "ATM withdrawal, etc."
            )

            Button(
                onClick = { 
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (fromAccount != null && toAccount != null) {
                        viewModel.performTransfer(fromAccount!!.id, toAccount!!.id, amt, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = amount.isNotBlank() && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Confirm Transfer")
            }
        }
    }
}

@Composable
private fun AccountSelector(
    label: String,
    selected: com.planora.app.core.data.database.entities.Account?,
    accounts: List<com.planora.app.core.data.database.entities.Account>,
    onSelect: (com.planora.app.core.data.database.entities.Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(selected?.name ?: "Select Account", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name) },
                        onClick = { onSelect(acc); expanded = false }
                    )
                }
            }
        }
    }
}
