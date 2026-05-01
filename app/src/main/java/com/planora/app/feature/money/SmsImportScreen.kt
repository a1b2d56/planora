package com.planora.app.feature.money

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.planora.app.R
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.sms.SmsImportManager
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.utils.CategoryIcon
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.FormatUtils
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@Composable
fun SmsImportScreen(
    onBack: () -> Unit,
    viewModel: MoneyViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) 
    }
    var permissionRequestedCount by remember { mutableStateOf(0) }
    
    var isLoading by remember { mutableStateOf(false) }
    var parsedTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var selectedToImport by remember { mutableStateOf<Set<Transaction>>(emptySet()) }
    var importComplete by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) {
            isLoading = true
            scope.launch {
                val mgr = SmsImportManager(context, viewModel.getRepository(), viewModel.getAccountRepository(), viewModel.prefsManager)
                parsedTransactions = mgr.scanSmsForTransactions()
                selectedToImport = parsedTransactions.toSet()
                isLoading = false
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && parsedTransactions.isEmpty() && !isLoading && !importComplete) {
            isLoading = true
            val mgr = SmsImportManager(context, viewModel.getRepository(), viewModel.getAccountRepository(), viewModel.prefsManager)
            parsedTransactions = mgr.scanSmsForTransactions()
            selectedToImport = parsedTransactions.toSet()
            isLoading = false
        }
    }

    PlanoraScreen(
        title = "SMS Auto-Import",
        onBack = if (importComplete) onBack else onBack,
        scrollable = false,
        actionButtonLabel = if (importComplete) "Done" else if (hasPermission) "Import ${selectedToImport.size}" else "Grant Permission",
        onActionButtonClick = {
            when {
                importComplete -> onBack()
                !hasPermission -> {
                    permissionRequestedCount++
                    if (permissionRequestedCount > 1) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.READ_SMS)
                    }
                }
                selectedToImport.isNotEmpty() -> {
                    isLoading = true
                    selectedToImport.forEach { txn -> viewModel.saveTransaction(txn) }
                    isLoading = false
                    importComplete = true
                }
            }
        }
    ) {
        if (!hasPermission) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    iconRes = R.drawable.ic_info,
                    title = "Permission Required",
                    subtitle = "Planora needs SMS permission to automatically scan your bank messages and add expenses to your ledger."
                )
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (importComplete) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    iconRes = R.drawable.ic_check_circle,
                    title = "Import Complete!",
                    subtitle = "Successfully added ${selectedToImport.size} transactions to your ledger."
                )
            }
        } else if (parsedTransactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    iconRes = R.drawable.ic_dashboard,
                    title = "No New Transactions",
                    subtitle = "We couldn't find any recent bank SMS that aren't already in your ledger."
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Select transactions to import", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    selectedToImport = if (selectedToImport.size == parsedTransactions.size) emptySet() else parsedTransactions.toSet()
                }) {
                    Text(if (selectedToImport.size == parsedTransactions.size) "Deselect All" else "Select All")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(parsedTransactions, key = { it.hashCode() }) { txn ->
                    val isSelected = selectedToImport.contains(txn)
                    val isIncome = txn.type == TransactionType.INCOME
                    val color = if (isIncome) IncomeGreen else ExpenseRed

                    PlanoraCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedToImport = if (isSelected) {
                                selectedToImport - txn
                            } else {
                                selectedToImport + txn
                            }
                        },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(CategoryIcon.forCategory(txn.category)), null, tint = color, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${txn.category} • ${DateUtils.formatDate(txn.date)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(FormatUtils.formatCurrency(txn.amount, "₹", forcePlus = isIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
            }
        }
    }
}
