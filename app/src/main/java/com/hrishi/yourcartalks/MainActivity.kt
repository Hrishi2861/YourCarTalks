package com.hrishi.yourcartalks

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.hrishi.yourcartalks.data.GreetingMessages
import com.hrishi.yourcartalks.data.ThemeMode
import com.hrishi.yourcartalks.data.TtsMethod
import com.hrishi.yourcartalks.tts.TextToSpeechManager
import com.hrishi.yourcartalks.tts.sherpa.SherpaOnnxManager
import com.hrishi.yourcartalks.tts.sherpa.SherpaModelConfig
import com.hrishi.yourcartalks.ui.screens.SetupScreen
import com.hrishi.yourcartalks.ui.theme.YourCarTalksTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = (application as YourCarTalksApp).preferencesManager

        setContent {
            val isSetupComplete by prefs.isSetupComplete.collectAsState(initial = null)
            val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            YourCarTalksTheme(themeMode) {
                when (isSetupComplete) {
                    null -> Unit
                    false -> SetupScreen { carName, driverName, theme ->
                        lifecycleScope.launch {
                            prefs.saveCarName(carName)
                            prefs.saveDriverName(driverName)
                            prefs.setThemeMode(theme)
                            prefs.setSetupComplete()
                            GreetingService.start(this@MainActivity)
                            Toast.makeText(this@MainActivity, "Service started!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true -> {
                        GreetingService.start(this)
                        SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as YourCarTalksApp
    val prefs = app.preferencesManager
    val scope = rememberCoroutineScope()

    val carName by prefs.carName.collectAsState(initial = "")
    val driverName by prefs.driverName.collectAsState(initial = "")
    val currentMethod by prefs.ttsMethod.collectAsState(initial = TtsMethod.SYSTEM)
    val currentTheme by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val selectedGreeting by prefs.selectedGreeting.collectAsState(initial = "")

    var editing by remember { mutableStateOf(false) }
    var editText by remember(carName) { mutableStateOf(carName) }
    var editingDriverName by remember { mutableStateOf(false) }
    var driverNameEditText by remember(driverName) { mutableStateOf(driverName) }

    val maleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.MALE) }
    val femaleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.FEMALE) }
    val kokoroManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.KOKORO) }
    var maleDownloaded by remember { mutableStateOf(maleManager.isModelDownloaded()) }
    var femaleDownloaded by remember { mutableStateOf(femaleManager.isModelDownloaded()) }
    var kokoroDownloaded by remember { mutableStateOf(kokoroManager.isModelDownloaded()) }
    var maleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var femaleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var kokoroDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        // Car Name
        Card(modifier = Modifier.fillMaxWidth()) {
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
                            scope.launch { prefs.saveCarName(editText) }
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

        // Driver Name
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Driver Name",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (editingDriverName) {
                    OutlinedTextField(
                        value = driverNameEditText,
                        onValueChange = { driverNameEditText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingDriverName = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch { prefs.saveDriverName(driverNameEditText) }
                            editingDriverName = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    Text(
                        text = driverName.ifBlank { "Tap to set your name" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (driverName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        driverNameEditText = driverName
                        editingDriverName = true
                    }) {
                        Text(if (driverName.isBlank()) "Add" else "Edit")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TTS Method
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Text-to-Speech Engine",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                TtsMethod.entries.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMethod == method,
                            onClick = {
                                scope.launch { prefs.setTtsMethod(method) }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (method) {
                                TtsMethod.SYSTEM -> "System TTS (default)"
                                TtsMethod.SHERPA_MALE -> "Sherpa-ONNX Male"
                                TtsMethod.SHERPA_FEMALE -> "Sherpa-ONNX Female"
                                TtsMethod.KOKORO -> "Kokoro British Female (Isabella)"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TestButton(
                            method = method,
                            context = context,
                            maleManager = maleManager,
                            femaleManager = femaleManager,
                            kokoroManager = kokoroManager,
                            isDownloaded = when (method) {
                                TtsMethod.SHERPA_MALE -> maleDownloaded
                                TtsMethod.SHERPA_FEMALE -> femaleDownloaded
                                TtsMethod.KOKORO -> kokoroDownloaded
                                else -> true
                            },
                            carName = carName,
                            driverName = driverName,
                            selectedGreeting = selectedGreeting
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sherpa Male Model Download
        if (currentMethod == TtsMethod.SHERPA_MALE) {
            ModelDownloadCard(
                config = SherpaOnnxManager.MALE,
                manager = maleManager,
                isDownloaded = maleDownloaded,
                downloadState = maleDownloadState,
                onDownload = { state ->
                    maleDownloadState = state
                },
                onDownloaded = {
                    maleDownloaded = true
                },
                scope = scope
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sherpa Female Model Download
        if (currentMethod == TtsMethod.SHERPA_FEMALE) {
            ModelDownloadCard(
                config = SherpaOnnxManager.FEMALE,
                manager = femaleManager,
                isDownloaded = femaleDownloaded,
                downloadState = femaleDownloadState,
                onDownload = { state ->
                    femaleDownloadState = state
                },
                onDownloaded = {
                    femaleDownloaded = true
                },
                scope = scope
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Kokoro Model Download
        if (currentMethod == TtsMethod.KOKORO) {
            ModelDownloadCard(
                config = SherpaOnnxManager.KOKORO,
                manager = kokoroManager,
                isDownloaded = kokoroDownloaded,
                downloadState = kokoroDownloadState,
                onDownload = { state ->
                    kokoroDownloadState = state
                },
                onDownloaded = {
                    kokoroDownloaded = true
                },
                scope = scope
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Greeting Style
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Greeting Style",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val messages = remember { GreetingMessages.all() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGreeting.isBlank(),
                        onClick = {
                            scope.launch { prefs.saveSelectedGreeting("") }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Random (default)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    messages.forEach { message ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGreeting == message,
                                onClick = {
                                    scope.launch { prefs.saveSelectedGreeting(message) }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = GreetingMessages.preview(message, "Your Car"),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = {
                                scope.launch { prefs.setThemeMode(mode) }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Service Status
        Card(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun TestButton(
    method: TtsMethod,
    context: android.content.Context,
    maleManager: SherpaOnnxManager,
    femaleManager: SherpaOnnxManager,
    kokoroManager: SherpaOnnxManager,
    isDownloaded: Boolean,
    carName: String,
    driverName: String,
    selectedGreeting: String
) {
    Button(
        onClick = {
            val text = if (driverName.isNotBlank() && kotlin.random.Random.nextBoolean()) {
                val messagePart = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                val formatted = GreetingMessages.format(messagePart, "your car")
                "Hello $driverName. $formatted"
            } else {
                val messagePart = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                GreetingMessages.format(messagePart, carName)
            }
            when (method) {
                TtsMethod.SYSTEM -> {
                    val ttsManager = TextToSpeechManager(context)
                    requestAudioFocusAndSpeak(context, ttsManager, text)
                }
                TtsMethod.SHERPA_MALE -> {
                    if (maleManager.isModelDownloaded()) {
                        requestAudioFocusAndSpeakSherpa(context, maleManager, text)
                    }
                }
                TtsMethod.SHERPA_FEMALE -> {
                    if (femaleManager.isModelDownloaded()) {
                        requestAudioFocusAndSpeakSherpa(context, femaleManager, text)
                    }
                }
                TtsMethod.KOKORO -> {
                    if (kokoroManager.isModelDownloaded()) {
                        requestAudioFocusAndSpeakSherpa(context, kokoroManager, text)
                    }
                }
            }
        },
        enabled = when (method) {
            TtsMethod.SYSTEM -> true
            TtsMethod.SHERPA_MALE -> isDownloaded
            TtsMethod.SHERPA_FEMALE -> isDownloaded
            TtsMethod.KOKORO -> isDownloaded
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text("Test", style = MaterialTheme.typography.labelSmall)
    }
}

private fun requestAudioFocusAndSpeak(
    context: android.content.Context,
    ttsManager: TextToSpeechManager,
    text: String
) {
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { }
        .build()

    val result = audioManager.requestAudioFocus(focusRequest)
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        ttsManager.initialize {
            ttsManager.speak(text) {
                audioManager.abandonAudioFocusRequest(focusRequest)
                ttsManager.shutdown()
            }
        }
    }
}

private fun requestAudioFocusAndSpeakSherpa(
    context: android.content.Context,
    manager: SherpaOnnxManager,
    text: String
) {
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { }
        .build()

    val result = audioManager.requestAudioFocus(focusRequest)
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        manager.speak(text) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }
}

@Composable
private fun ModelDownloadCard(
    config: SherpaModelConfig,
    manager: SherpaOnnxManager,
    isDownloaded: Boolean,
    downloadState: DownloadState,
    onDownload: (DownloadState) -> Unit,
    onDownloaded: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Voice Model (${config.gender})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    if (!isDownloaded) {
                        Text(
                            text = "Download English ${config.gender} TTS model. Required for offline AI voice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isDownloaded) {
                        Button(
                            onClick = {
                                scope.launch {
                                    onDownload(DownloadState.Downloading(0, 0))
                                    val result = manager.downloadAndExtract { progress ->
                                        if (progress.phase == "Extracting model...") {
                                            onDownload(DownloadState.Extracting)
                                        } else {
                                            onDownload(
                                                DownloadState.Downloading(
                                                    progress.bytesRead,
                                                    progress.totalBytes
                                                )
                                            )
                                        }
                                    }
                                    onDownload(
                                        if (result.isSuccess) {
                                            onDownloaded()
                                            DownloadState.Done
                                        } else {
                                            DownloadState.Error(
                                                result.exceptionOrNull()?.message
                                                    ?: "Download failed"
                                            )
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Download Model (${config.gender})")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Requires Wi-Fi. Download once, use offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is DownloadState.Downloading -> {
                    val progressText = if (state.total > 0) {
                        "${state.read / 1024 / 1024} MB / ${state.total / 1024 / 1024} MB"
                    } else {
                        "${state.read / 1024 / 1024} MB"
                    }
                    if (state.total > 0) {
                        LinearProgressIndicator(
                            progress = { state.read.toFloat() / state.total.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading... $progressText",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is DownloadState.Extracting -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extracting model...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DownloadState.Done -> {
                    Text(
                        text = "Download complete! AI voice is ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is DownloadState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        onDownload(DownloadState.Idle)
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val read: Long, val total: Long) : DownloadState()
    data object Extracting : DownloadState()
    data object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}
