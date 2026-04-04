package com.devil.taskzio.ui.screens.welcome

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devil.taskzio.R
import com.devil.taskzio.ui.viewmodels.SettingsViewModel

@Composable
fun WelcomeScreen(
    onComplete: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val nameValid = name.trim().length >= 2
    
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    fun handleGoogleSignIn() {
        isLoading = true
        viewModel.signInWithGoogle(activity) {
            isLoading = false
            step = 2
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_splash_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(62.dp)
                    )
                }
            }

            AnimatedContent(targetState = step, transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically { it / 3 }).togetherWith(fadeOut(tween(200)))
            }, label = "step") { s ->
                when (s) {
                    0 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Welcome to Taskzio",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Your personal productivity companion.\nTasks, money, savings and notes — all in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        
                        Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Get Started", modifier = Modifier.padding(vertical = 4.dp))
                        }
                        
                        OutlinedButton(
                            onClick = { handleGoogleSignIn() }, 
                            modifier = Modifier.fillMaxWidth(), 
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(painterResource(R.drawable.ic_google), null, modifier = Modifier.size(18.dp), tint = Color.Unspecified)
                                    Text("Get Started with Google", modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("What should we call you?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Your name will personalize your experience.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        TextField(
                            value = name, onValueChange = { name = it },
                            placeholder = { Text("Your name") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_person), null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (nameValid) onComplete(name.trim()) })
                        )
                        Button(onClick = { if (nameValid) onComplete(name.trim()) }, enabled = nameValid,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Continue", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
