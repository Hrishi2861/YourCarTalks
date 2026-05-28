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
                    false -> SetupScreen { carName, theme ->
                        lifecycleScope.launch {
                            prefs.saveCarName(carName)
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
    val currentMethod by prefs.ttsMethod.collectAsState(initial = TtsMethod.SYSTEM)
    val currentTheme by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    var editing by remember { mutableStateOf(false) }
    var editText by remember(carName) { mutableStateOf(carName) }

    val maleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.MALE) }
    val femaleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.FEMALE) }
    var maleDownloaded by remember { mutableStateOf(maleManager.isModelDownloaded()) }
    var femaleDownloaded by remember { mutableStateOf(femaleManager.isModelDownloaded()) }
    var maleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var femaleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

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
                            isDownloaded = when (method) {
                                TtsMethod.SHERPA_MALE -> maleDownloaded
                                TtsMethod.SHERPA_FEMALE -> femaleDownloaded
                                else -> true
                            },
                            carName = carName
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
    isDownloaded: Boolean,
    carName: String
) {
    Button(
        onClick = {
            val text = if (carName.isNotBlank()) "Welcome to your $carName" else "Welcome to your car"
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
            }
        },
        enabled = when (method) {
            TtsMethod.SYSTEM -> true
            TtsMethod.SHERPA_MALE -> isDownloaded
            TtsMethod.SHERPA_FEMALE -> isDownloaded
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
