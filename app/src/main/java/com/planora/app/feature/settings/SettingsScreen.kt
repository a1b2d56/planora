package com.planora.app.feature.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.planora.app.R
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.ui.components.*

/** Defined Row actions for settings. */
private sealed interface SettingsRowType {
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsRowType
    data class Click(val onClick: () -> Unit) : SettingsRowType
    data object Info : SettingsRowType
}

@Composable
private fun SettingsRow(
    @DrawableRes iconRes: Int,
    iconBg: Color,
    title: String,
    subtitle: String,
    type: SettingsRowType
) {
    val clickable = type is SettingsRowType.Click
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { type.onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = iconBg, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (type) {
            is SettingsRowType.Toggle -> Switch(
                checked = type.checked, onCheckedChange = type.onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            is SettingsRowType.Click -> Icon(
                painterResource(R.drawable.ic_chevron_right), null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
            )
            is SettingsRowType.Info -> Unit
        }
    }
}

@Composable
private fun SettingsDivider() =
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )

@Composable
private fun SectionLabel(title: String) =
    Text(
        text = title, 
        style = MaterialTheme.typography.labelMedium, 
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )

private sealed interface SettingsDialog { 
    data object None : SettingsDialog
    data object Currency : SettingsDialog
    data object Name : SettingsDialog
    data class PasswordExport(val targetUri: android.net.Uri) : SettingsDialog
    data class PasswordImport(val sourceUri: android.net.Uri) : SettingsDialog
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    
    val activeDialog = remember { mutableStateOf<SettingsDialog>(SettingsDialog.None) }
    
    // Offline Backup Launchers
    val offlineExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-planora")
    ) { uri ->
        uri?.let { activeDialog.value = SettingsDialog.PasswordExport(it) }
    }
    
    val offlineImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { activeDialog.value = SettingsDialog.PasswordImport(it) }
    }
    
    // Launcher for Drive consent dialog (when user hasn't granted Drive scope yet)
    val driveConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.retryPendingCloudOperation(activity)
        } else {
            viewModel.clearBackupStatus()
        }
    }
    
    val versionName = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            info.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Refresh status on entry
    LaunchedEffect(Unit) {
        viewModel.refreshAuthStatus()
    }
    
    // Observe Drive consent requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.driveConsentRequired.collect { intentSenderRequest ->
            driveConsentLauncher.launch(intentSenderRequest)
        }
    }

    LaunchedEffect(state.backupStatus) {
        when (val status = state.backupStatus) {
            is BackupStatus.Success -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.clearBackupStatus()
            }
            is BackupStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.clearBackupStatus()
            }
            else -> {}
        }
    }

    when (val dialog = activeDialog.value) {
        SettingsDialog.Currency -> {
            CurrencyDialog(
                current = state.currencySymbol,
                onDismiss = { activeDialog.value = SettingsDialog.None },
                onSave = { 
                    viewModel.setCurrencySymbol(it)
                    activeDialog.value = SettingsDialog.None 
                }
            )
        }
        SettingsDialog.Name -> {
            NameDialog(
                current = state.userName,
                onDismiss = { activeDialog.value = SettingsDialog.None },
                onSave = { 
                    viewModel.setUserName(it)
                    activeDialog.value = SettingsDialog.None 
                }
            )
        }
        is SettingsDialog.PasswordExport -> {
            PasswordDialog(
                title = "Encrypt Export",
                subtitle = "Set a secure password. You will need it to import this data.",
                buttonText = "Encrypt & Save",
                onDismiss = { activeDialog.value = SettingsDialog.None },
                onSubmit = { password ->
                    viewModel.exportToOfflineBackup(context, dialog.targetUri, password)
                    activeDialog.value = SettingsDialog.None
                }
            )
        }
        is SettingsDialog.PasswordImport -> {
            PasswordDialog(
                title = "Decrypt Offline Data",
                subtitle = "Enter the password used to encrypt this .planora backup.",
                buttonText = "Decrypt & Restore",
                onDismiss = { activeDialog.value = SettingsDialog.None },
                onSubmit = { password ->
                    viewModel.importFromOfflineBackup(context, dialog.sourceUri, password)
                    activeDialog.value = SettingsDialog.None
                }
            )
        }
        SettingsDialog.None -> { /* No dialog */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Customize your experience", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Profile Section
        SectionLabel("Profile")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeDialog.value = SettingsDialog.Name }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.userPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = state.userPhotoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (state.userName.isBlank()) "?" else state.userName.first().uppercaseChar().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.userName.ifBlank { "Set your name" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Tap to edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel("Cloud Sync")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                if (!state.isGoogleSignedIn) {
                    // Not signed in
                    SettingsRow(
                        iconRes = R.drawable.ic_person,
                        iconBg = MaterialTheme.colorScheme.primary,
                        title = "Sign In with Google",
                        subtitle = "Enable automatic Google Drive backups",
                        type = SettingsRowType.Click {
                            viewModel.signInWithGoogle(activity)
                        }
                    )
                } else {
                    // Signed in -> Show Drive options
                    SettingsRow(
                        iconRes = R.drawable.ic_person,
                        iconBg = IncomeGreen,
                        title = viewModel.authManager.currentUser?.email ?: "Google Account",
                        subtitle = "Signed in. Tap to sign out.",
                        type = SettingsRowType.Click {
                            viewModel.signOut()
                        }
                    )
                    SettingsDivider()
                    
                    if (state.backupStatus == BackupStatus.Loading) {
                        // Loading State
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Syncing with Google Drive...")
                        }
                    } else {
                        SettingsRow(
                            iconRes = R.drawable.ic_arrow_upward,
                            iconBg = MaterialTheme.colorScheme.primary,
                            title = "Back up to Cloud",
                            subtitle = "Sync secure backup to Google Drive",
                            type = SettingsRowType.Click { viewModel.backupToCloud(activity) }
                        )
                        SettingsDivider()
                        SettingsRow(
                            iconRes = R.drawable.ic_arrow_downward,
                            iconBg = MaterialTheme.colorScheme.secondary,
                            title = "Restore from Cloud",
                            subtitle = "Download layout & data",
                            type = SettingsRowType.Click { viewModel.restoreFromCloud(activity) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Themes & Appearance
        SectionLabel("Appearance")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOption("Light", R.drawable.ic_light_mode, AppTheme.LIGHT, state.appTheme, Modifier.weight(1f)) { viewModel.setAppTheme(it) }
                    ThemeOption("Dark", R.drawable.ic_amoled, AppTheme.DARK, state.appTheme, Modifier.weight(1f)) { viewModel.setAppTheme(it) }
                    ThemeOption("Midnight", R.drawable.ic_dark_mode, AppTheme.MIDNIGHT, state.appTheme, Modifier.weight(1f)) { viewModel.setAppTheme(it) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Preferences
        SectionLabel("Preferences")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                SettingsRow(
                    R.drawable.ic_notifications, MaterialTheme.colorScheme.tertiary,
                    "Notifications", "Savings goal daily reminders",
                    SettingsRowType.Toggle(state.notificationsEnabled, viewModel::setNotificationsEnabled)
                )
                SettingsDivider()
                SettingsRow(
                    R.drawable.ic_attach_money, IncomeGreen,
                    "Currency Symbol", "Currently: ${state.currencySymbol}",
                    SettingsRowType.Click { activeDialog.value = SettingsDialog.Currency }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Backup & Restore
        SectionLabel("Backup & Restore")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                SettingsRow(
                    iconRes = R.drawable.ic_arrow_upward,
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    title = "Export Local Backup",
                    subtitle = "Save a secure standalone .planora file",
                    type = SettingsRowType.Click { 
                        val hash = java.util.UUID.randomUUID().toString().substring(0, 8)
                        offlineExportLauncher.launch("planora_bkp_$hash.planora") 
                    }
                )
                SettingsDivider()
                SettingsRow(
                    iconRes = R.drawable.ic_arrow_downward,
                    iconBg = MaterialTheme.colorScheme.error,
                    title = "Import Local Backup",
                    subtitle = "Restore an encrypted .planora file",
                    type = SettingsRowType.Click { offlineImportLauncher.launch(arrayOf("application/x-planora", "*/*")) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // About
        SectionLabel("About")
        PlanoraCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column {
                SettingsRow(R.drawable.ic_person, MaterialTheme.colorScheme.primary, "Developer", "Ananmay Jha", SettingsRowType.Info)
                SettingsDivider()
                SettingsRow(R.drawable.ic_info, MaterialTheme.colorScheme.secondary, "Version", versionName, SettingsRowType.Info)
            }
        }

        Spacer(Modifier.height(40.dp + navBarPadding))
    }
}

@Composable
private fun ThemeOption(
    label: String, 
    @DrawableRes iconRes: Int, 
    theme: AppTheme,
    current: AppTheme, 
    modifier: Modifier = Modifier, 
    onSelect: (AppTheme) -> Unit
) {
    val selected = theme == current
    Surface(
        modifier = modifier.clickable { onSelect(theme) }, 
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painterResource(iconRes), null, modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        title = { Text("Your Name") },
        text = {
            PlanoraTextField(
                value = name,
                onValueChange = { name = it },
                label = "Your Name",
                placeholder = "Enter your name",
                leadingIcon = { Icon(painterResource(R.drawable.ic_person), null, modifier = Modifier.size(20.dp)) }
            )
        },
        confirmButton = { Button(onClick = { onSave(name.trim()) }, enabled = name.trim().isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CurrencyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var symbol by remember { mutableStateOf(current) }
    val options = listOf(
        "$" to "Dollars",
        "£" to "Pounds",
        "€" to "Euros",
        "¥" to "Yen / Yuan",
        "₣" to "Swiss Franc",
        "₹" to "Rupees"
    )
    
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        title = { Text("Currency Symbol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (s, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (symbol == s) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { symbol = s }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (symbol == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(symbol) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    subtitle: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onSubmit: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                PlanoraTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Encryption Password",
                    placeholder = "Enter password",
                    leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp)) }
                )
            }
        },
        confirmButton = { 
            Button(
                onClick = { onSubmit(password.toCharArray()) }, 
                enabled = password.isNotBlank()
            ) { Text(buttonText) } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
