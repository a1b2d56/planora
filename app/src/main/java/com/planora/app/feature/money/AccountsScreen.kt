@file:Suppress("UNUSED_VALUE")
package com.planora.app.feature.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.AccountType
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.utils.BrandIcons
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    PlanoraScreen(
        title = "My Wallets",
        onBack = onBack,
        scrollable = false,
        actionButtonLabel = "Add Wallet",
        onActionButtonClick = { showAddDialog = true }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(accounts, key = { it.id }) { acc ->
                SwipeToDeleteBox(
                    onDelete = { viewModel.delete(acc) },
                    enabled = !acc.isDefault
                ) {
                    AccountCard(acc, onClick = { onNavigateToDetail(acc.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        val scope = rememberCoroutineScope()
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddAccountSheet(
                onSave = { acc -> viewModel.save(acc) },
                onDismiss = {
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion { showAddDialog = false }
                }
            )
        }
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    PlanoraCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), containerColor = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val brandIcon = remember(account.name) { BrandIcons.getIconForBank(account.name) }
            
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (brandIcon != null) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (brandIcon != null) {
                    Icon(painterResource(brandIcon), null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
                } else {
                    Icon(painterResource(if (account.type == AccountType.CASH) R.drawable.ic_wallet else R.drawable.ic_account_balance), null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (account.isDefault) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(IncomeGreen.copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Default", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                        }
                    }
                }
                Text(account.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AddAccountSheet(onSave: (Account) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var setAsDefault by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Wallet", style = MaterialTheme.typography.titleLarge)
        PlanoraTextField(value = name, onValueChange = { name = it }, label = "Wallet Name")
        
        Text("Type", style = MaterialTheme.typography.labelMedium)
        PlanoraChipSelector(
            options = AccountType.entries.map { it.name },
            selectedOption = type.name,
            onOptionSelected = { type = AccountType.valueOf(it) }
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = setAsDefault, onCheckedChange = { setAsDefault = it })
            Text("Set as default wallet")
        }

        Button(
            onClick = {
                if (name.trim().isBlank()) return@Button
                onSave(Account(name = name.trim(), type = type, isDefault = setAsDefault))
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Save") }
    }
}
