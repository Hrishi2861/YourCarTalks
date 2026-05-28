package com.hrishi.yourcartalks.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hrishi.yourcartalks.data.ThemeMode

enum class SetupStep {
    CAR_NAME,
    BATTERY_OPT,
    AUTO_START,
    NOTIFICATION,
    BLUETOOTH,
    THEME,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onComplete: (String, ThemeMode) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(SetupStep.CAR_NAME) }
    var carName by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var batteryOptDone by remember { mutableStateOf(false) }
    var notificationDone by remember { mutableStateOf(false) }
    var bluetoothDone by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationDone = granted
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        bluetoothDone = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "YourCarTalks",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentStep.ordinal + 1f) / SetupStep.entries.size },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (currentStep) {
            SetupStep.CAR_NAME -> CarNameStep(
                carName = carName,
                onCarNameChange = { carName = it },
                onNext = { if (carName.isNotBlank()) currentStep = SetupStep.BATTERY_OPT }
            )

            SetupStep.BATTERY_OPT -> BatteryOptStep(
                isDone = batteryOptDone,
                onRequest = {
                    requestBatteryOptimization(context)
                    checkBatteryOpt(context) { batteryOptDone = it }
                },
                onNext = { currentStep = SetupStep.AUTO_START }
            )

            SetupStep.AUTO_START -> AutoStartStep(
                onOpenSettings = { openAutoStartSettings(context) },
                onNext = {
                    currentStep = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        SetupStep.NOTIFICATION
                    } else {
                        notificationDone = true
                        SetupStep.THEME
                    }
                }
            )

            SetupStep.NOTIFICATION -> NotificationStep(
                isDone = notificationDone,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationDone = true
                    }
                },
                onNext = {
                    currentStep = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SetupStep.BLUETOOTH
                    } else {
                        bluetoothDone = true
                        SetupStep.THEME
                    }
                }
            )

            SetupStep.BLUETOOTH -> BluetoothStep(
                isDone = bluetoothDone,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        bluetoothDone = true
                    }
                },
                onNext = { currentStep = SetupStep.THEME }
            )

            SetupStep.THEME -> ThemeStep(
                selectedTheme = selectedTheme,
                onThemeChange = { selectedTheme = it },
                onNext = { currentStep = SetupStep.COMPLETE }
            )

            SetupStep.COMPLETE -> CompleteStep(
                carName = carName,
                onFinish = { onComplete(carName, selectedTheme) }
            )
        }
    }
}

@Composable
private fun CarNameStep(
    carName: String,
    onCarNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Welcome!",
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "What's your car's name?",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = carName,
        onValueChange = onCarNameChange,
        label = { Text("Car Name") },
        placeholder = { Text("e.g., Tesla, Prius, Mustang") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { if (carName.isNotBlank()) onNext() }),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onNext,
        enabled = carName.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun BatteryOptStep(
    isDone: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Disable Battery Optimization",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "YourCarTalks needs to run in the background to detect Android Auto connections. " +
                "Please disable battery optimization for this app.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (isDone) {
        AssistChip(
            onClick = {},
            label = { Text("Done") },
            leadingIcon = { Text("✓") }
        )
    } else {
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Battery Settings")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun AutoStartStep(
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Enable Auto-Start",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Some manufacturers require apps to be granted auto-start permission. " +
                "Please enable it in your system settings to ensure YourCarTalks " +
                "works reliably after a reboot.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Look for \"Auto-Start\", \"Startup Manager\", or \"Protected Apps\" " +
                "in your system settings and enable it for YourCarTalks.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = onOpenSettings,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open App Settings")
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun NotificationStep(
    isDone: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Notification Permission",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "YourCarTalks needs notification permission to keep a foreground service " +
                "running. This allows it to monitor Android Auto connections in the background.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (isDone) {
        AssistChip(
            onClick = {},
            label = { Text("Granted") },
            leadingIcon = { Text("✓") }
        )
    } else {
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        enabled = isDone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun BluetoothStep(
    isDone: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Bluetooth Permission",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "YourCarTalks needs Bluetooth permission to use the foreground service " +
                "that detects Android Auto connections on Android 12+.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (isDone) {
        AssistChip(
            onClick = {},
            label = { Text("Granted") },
            leadingIcon = { Text("✓") }
        )
    } else {
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        enabled = isDone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun ThemeStep(
    selectedTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Choose Theme",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Select your preferred app appearance.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    ThemeMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedTheme == mode,
                onClick = { onThemeChange(mode) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "Follow your device's theme"
                        ThemeMode.LIGHT -> "Always use light mode"
                        ThemeMode.DARK -> "Always use dark mode"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Next")
    }
}

@Composable
private fun CompleteStep(
    carName: String,
    onFinish: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "All Set!",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "YourCarTalks will now greet you with\n" +
                "\"Welcome to Your $carName\"\n" +
                "every time you connect to Android Auto.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start Service")
    }
}

private fun requestBatteryOptimization(context: Context) {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun checkBatteryOpt(context: Context, onResult: (Boolean) -> Unit) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    onResult(pm.isIgnoringBatteryOptimizations(context.packageName))
}

private fun openAutoStartSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
