package com.hrishi.yourcartalks

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.hrishi.yourcartalks.ui.screens.SetupScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = (application as YourCarTalksApp).preferencesManager

        setContent {
            val isSetupComplete by prefs.isSetupComplete.collectAsState(initial = null)

            when (isSetupComplete) {
                null -> Unit
                false -> SetupScreen { carName ->
                    lifecycleScope.launch {
                        prefs.saveCarName(carName)
                        prefs.setSetupComplete()
                        GreetingService.start(this@MainActivity)
                        Toast.makeText(this@MainActivity, "Service started!", Toast.LENGTH_SHORT).show()
                    }
                }
                true -> {
                    GreetingService.start(this)
                    SettingsScreen(
                        onCarNameChanged = { carName ->
                            lifecycleScope.launch { prefs.saveCarName(carName) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onCarNameChanged: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as YourCarTalksApp).preferencesManager
    val carName by prefs.carName.collectAsState(initial = "")
    var editing by remember { mutableStateOf(false) }
    var editText by remember(carName) { mutableStateOf(carName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "YourCarTalks",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Car Name",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (editing) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editing = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onCarNameChanged(editText)
                            editing = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    Text(
                        text = carName.ifBlank { "(not set)" },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        editText = carName
                        editing = true
                    }) {
                        Text("Edit")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Running in background — will greet you when Android Auto connects",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
