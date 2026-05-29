package com.hrishi.yourcartalks

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.hrishi.yourcartalks.data.GreetingMessages
import com.hrishi.yourcartalks.data.GreetingNameMode
import com.hrishi.yourcartalks.data.ThemeMode
import com.hrishi.yourcartalks.data.TtsMethod
import com.hrishi.yourcartalks.tts.TextToSpeechManager
import com.hrishi.yourcartalks.tts.sherpa.SherpaOnnxManager
import com.hrishi.yourcartalks.tts.sherpa.SherpaModelConfig
import com.hrishi.yourcartalks.ui.screens.SetupScreen
import com.hrishi.yourcartalks.ui.theme.AutoColors
import com.hrishi.yourcartalks.ui.theme.CarSilhouetteIcon
import com.hrishi.yourcartalks.ui.theme.ChevronLeftIcon
import com.hrishi.yourcartalks.ui.theme.ChevronRightIcon
import com.hrishi.yourcartalks.ui.theme.DownloadIcon
import com.hrishi.yourcartalks.ui.theme.KeyIcon
import com.hrishi.yourcartalks.ui.theme.MoonIcon
import com.hrishi.yourcartalks.ui.theme.Outfit
import com.hrishi.yourcartalks.ui.theme.PageDotIndicator
import com.hrishi.yourcartalks.ui.theme.PencilIcon
import com.hrishi.yourcartalks.ui.theme.PersonIcon
import com.hrishi.yourcartalks.ui.theme.RadioIndicator
import com.hrishi.yourcartalks.ui.theme.ShieldIcon
import com.hrishi.yourcartalks.ui.theme.SunIcon
import com.hrishi.yourcartalks.ui.theme.SystemIcon
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
                    false -> SetupScreen { carName, driverName, nameMode, theme ->
                        lifecycleScope.launch {
                            prefs.saveCarName(carName)
                            prefs.saveDriverName(driverName)
                            prefs.saveNameMode(nameMode)
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
    val currentNameMode by prefs.nameMode.collectAsState(initial = GreetingNameMode.RANDOM)
    val selectedGreeting by prefs.selectedGreeting.collectAsState(initial = "")

    val maleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.MALE) }
    val femaleManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.FEMALE) }
    val kokoroManager = remember { SherpaOnnxManager(context, SherpaOnnxManager.KOKORO) }
    var maleDownloaded by remember { mutableStateOf(maleManager.isModelDownloaded()) }
    var femaleDownloaded by remember { mutableStateOf(femaleManager.isModelDownloaded()) }
    var kokoroDownloaded by remember { mutableStateOf(kokoroManager.isModelDownloaded()) }
    var maleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var femaleDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var kokoroDownloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    val greetingOptions = remember { listOf("Random (default)") + GreetingMessages.all() }

    val initialGreetingPage = if (selectedGreeting.isNotBlank()) {
        val idx = GreetingMessages.all().indexOf(selectedGreeting)
        if (idx >= 0) idx + 1 else 0
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x08E8A020),
                        1f to Color.Transparent
                    )
                )
                .align(Alignment.TopCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderRow()
            }

            item {
                NameCard(
                    label = "CAR NAME",
                    value = carName,
                    placeholder = "(not set)",
                    onSave = { scope.launch { prefs.saveCarName(it) } },
                    icon = { KeyIcon(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSurface) }
                )
            }

            item {
                NameCard(
                    label = "DRIVER NAME",
                    value = driverName,
                    placeholder = "Tap to set your name",
                    onSave = { scope.launch { prefs.saveDriverName(it) } },
                    icon = { PersonIcon(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSurface) }
                )
            }

            item {
                Text(
                    text = "TEXT-TO-SPEECH ENGINE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            TtsMethod.entries.forEach { method ->
                item(key = method) {
                    val isDownloaded = when (method) {
                        TtsMethod.SYSTEM -> true
                        TtsMethod.SHERPA_MALE -> maleDownloaded
                        TtsMethod.SHERPA_FEMALE -> femaleDownloaded
                        TtsMethod.KOKORO -> kokoroDownloaded
                    }
                    TtsOptionCard(
                        method = method,
                        selected = currentMethod == method,
                        onClick = { scope.launch { prefs.setTtsMethod(method) } },
                        testEnabled = isDownloaded,
                        testText = buildTestText(driverName, carName, selectedGreeting, currentNameMode),
                        onTest = { method, text ->
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
                        }
                    )
                }
            }

            if (currentMethod == TtsMethod.SHERPA_MALE) {
                item {
                    ModelDownloadCard(
                        config = SherpaOnnxManager.MALE,
                        isDownloaded = maleDownloaded,
                        downloadState = maleDownloadState,
                        onDownload = { maleDownloadState = it },
                        onDownloaded = { maleDownloaded = true },
                        onRetry = { maleDownloadState = DownloadState.Idle },
                        manager = maleManager,
                        scope = scope
                    )
                }
            }

            if (currentMethod == TtsMethod.SHERPA_FEMALE) {
                item {
                    ModelDownloadCard(
                        config = SherpaOnnxManager.FEMALE,
                        isDownloaded = femaleDownloaded,
                        downloadState = femaleDownloadState,
                        onDownload = { femaleDownloadState = it },
                        onDownloaded = { femaleDownloaded = true },
                        onRetry = { femaleDownloadState = DownloadState.Idle },
                        manager = femaleManager,
                        scope = scope
                    )
                }
            }

            if (currentMethod == TtsMethod.KOKORO) {
                item {
                    ModelDownloadCard(
                        config = SherpaOnnxManager.KOKORO,
                        isDownloaded = kokoroDownloaded,
                        downloadState = kokoroDownloadState,
                        onDownload = { kokoroDownloadState = it },
                        onDownloaded = { kokoroDownloaded = true },
                        onRetry = { kokoroDownloadState = DownloadState.Idle },
                        manager = kokoroManager,
                        scope = scope
                    )
                }
            }

            item {
                Text(
                    text = "GREETING STYLE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                GreetingStylePager(
                    options = greetingOptions,
                    initialPage = initialGreetingPage,
                    coroutineScope = scope,
                    onPageSelected = { msg ->
                        scope.launch { prefs.saveSelectedGreeting(msg) }
                    }
                )
            }

            item {
                Text(
                    text = "GREETING NAME",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                NameModeSelector(
                    currentMode = currentNameMode,
                    onModeSelected = { scope.launch { prefs.saveNameMode(it) } }
                )
            }

            item {
                Text(
                    text = "APP THEME",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                ThemeSegmentedControl(
                    currentTheme = currentTheme,
                    onThemeSelected = { scope.launch { prefs.setThemeMode(it) } }
                )
            }

            item {
                ServiceStatusBanner()
            }
        }
    }
}

private fun buildTestText(
    driverName: String,
    carName: String,
    selectedGreeting: String,
    nameMode: GreetingNameMode
): String {
    return when (nameMode) {
        GreetingNameMode.DRIVER_NAME -> {
            if (driverName.isNotBlank()) {
                val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                val formatted = GreetingMessages.format(msg, "your car")
                "Hello $driverName. $formatted"
            } else {
                val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                GreetingMessages.format(msg, carName)
            }
        }
        GreetingNameMode.CAR_NAME -> {
            val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
            GreetingMessages.format(msg, carName)
        }
        GreetingNameMode.BOTH -> {
            val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
            val formatted = GreetingMessages.format(msg, carName)
            if (driverName.isNotBlank()) "Hello $driverName. $formatted" else formatted
        }
        GreetingNameMode.RANDOM -> {
            if (driverName.isNotBlank() && kotlin.random.Random.nextBoolean()) {
                val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                val formatted = GreetingMessages.format(msg, "your car")
                "Hello $driverName. $formatted"
            } else {
                val msg = if (selectedGreeting.isNotBlank()) selectedGreeting else GreetingMessages.random()
                GreetingMessages.format(msg, carName)
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarSilhouetteIcon(
            modifier = Modifier.size(width = 28.dp, height = 18.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "YourCarTalks",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, AutoColors.success),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = AutoColors.success, radius = size.width / 2f)
                }
                Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = AutoColors.success,
                )
            }
        }
    }
}

@Composable
private fun NameCard(
    label: String,
    value: String,
    placeholder: String,
    onSave: (String) -> Unit,
    icon: @Composable () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var editText by remember(value) { mutableStateOf(value) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (editing) editText.ifBlank { placeholder }
                        else value.ifBlank { placeholder },
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (value.isBlank() && !editing)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (editing) {
                    TextButton(onClick = { editing = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            onSave(editText)
                            editing = false
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    IconButton(
                        onClick = {
                            editText = value
                            editing = true
                        }
                    ) {
                        PencilIcon(modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (editing) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        }
    }
}

@Composable
private fun TtsOptionCard(
    method: TtsMethod,
    selected: Boolean,
    onClick: () -> Unit,
    testEnabled: Boolean,
    testText: String,
    onTest: (TtsMethod, String) -> Unit
) {
    val (label, subtitle) = when (method) {
        TtsMethod.SYSTEM -> "System TTS" to "Device default engine"
        TtsMethod.SHERPA_MALE -> "Sherpa-ONNX Male" to "Offline AI \u00B7 English Male \u00B7 80 MB"
        TtsMethod.SHERPA_FEMALE -> "Sherpa-ONNX Female" to "Offline AI \u00B7 English Female \u00B7 80 MB"
        TtsMethod.KOKORO -> "Kokoro Isabella" to "Offline AI \u00B7 British Female \u00B7 330 MB"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AutoColors.selectedBg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioIndicator(
                selected = selected,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (testEnabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clickable { onTest(method, testText) }
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TEST",
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelDownloadCard(
    config: SherpaModelConfig,
    isDownloaded: Boolean,
    downloadState: DownloadState,
    onDownload: (DownloadState) -> Unit,
    onDownloaded: () -> Unit,
    onRetry: () -> Unit,
    manager: SherpaOnnxManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: icon + title + badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadIcon(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Voice Model (${config.gender})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isDownloaded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x204CAF50),
                        border = BorderStroke(1.dp, AutoColors.success)
                    ) {
                        Text(
                            text = "DOWNLOADED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontFamily = Outfit,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            color = AutoColors.success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    if (!isDownloaded) {
                        Button(
                            onClick = {
                                scope.launch {
                                    onDownload(DownloadState.Downloading(0, 0))
                                    val result = manager.downloadAndExtract { progress ->
                                        if (progress.phase == "Extracting model...") {
                                            onDownload(DownloadState.Extracting)
                                        } else {
                                            onDownload(DownloadState.Downloading(progress.bytesRead, progress.totalBytes))
                                        }
                                    }
                                    onDownload(
                                        if (result.isSuccess) {
                                            onDownloaded()
                                            DownloadState.Done
                                        } else {
                                            DownloadState.Error(result.exceptionOrNull()?.message ?: "Download failed")
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            DownloadIcon(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2f
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Model (${config.gender})",
                                fontFamily = Outfit,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
                is DownloadState.Downloading -> {
                    val progressText = if (state.total > 0) {
                        "${state.read / 1024 / 1024} MB / ${state.total / 1024 / 1024} MB"
                    } else {
                        "${state.read / 1024 / 1024} MB"
                    }
                    LinearProgressIndicator(
                        progress = { if (state.total > 0) state.read.toFloat() / state.total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF1F1F1F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloading... $progressText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is DownloadState.Extracting -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF1F1F1F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracting model...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DownloadState.Done -> {
                    // Already handled by the badge, but show text
                }
                is DownloadState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutoColors.destructive
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onRetry()
                            scope.launch {
                                onDownload(DownloadState.Downloading(0, 0))
                                val result = manager.downloadAndExtract { progress ->
                                    if (progress.phase == "Extracting model...") {
                                        onDownload(DownloadState.Extracting)
                                    } else {
                                        onDownload(DownloadState.Downloading(progress.bytesRead, progress.totalBytes))
                                    }
                                }
                                onDownload(
                                    if (result.isSuccess) {
                                        onDownloaded()
                                        DownloadState.Done
                                    } else {
                                        DownloadState.Error(result.exceptionOrNull()?.message ?: "Download failed")
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Retry", fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GreetingStylePager(
    options: List<String>,
    initialPage: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onPageSelected: (String) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, options.size - 1),
        pageCount = { options.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        val msg = if (pagerState.currentPage == 0) "" else options[pagerState.currentPage]
        onPageSelected(msg)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    userScrollEnabled = true
                ) { page ->
                    val displayText = if (page == 0) "Random (default)" else {
                        GreetingMessages.preview(options[page], "Your Car")
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            val prev = (pagerState.currentPage - 1 + options.size) % options.size
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(prev)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        ChevronLeftIcon(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    options.forEachIndexed { index, _ ->
                        PageDotIndicator(
                            active = index == pagerState.currentPage,
                            modifier = Modifier.size(6.dp).padding(horizontal = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = (pagerState.currentPage + 1) % options.size
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(next)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        ChevronRightIcon(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap arrows to preview",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ThemeSegmentedControl(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = currentTheme == mode
                val icon = @Composable {
                    when (mode) {
                        ThemeMode.LIGHT -> SunIcon(
                            modifier = Modifier.size(14.dp),
                            color = if (selected) Color.Black else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 1.5f
                        )
                        ThemeMode.DARK -> MoonIcon(
                            modifier = Modifier.size(14.dp),
                            color = if (selected) Color.Black else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 1.5f
                        )
                        ThemeMode.SYSTEM -> SystemIcon(
                            modifier = Modifier.size(14.dp),
                            color = if (selected) Color.Black else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 1.5f
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onThemeSelected(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        icon()
                        Text(
                            text = when (mode) {
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.SYSTEM -> "System"
                            },
                            fontFamily = Outfit,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (selected) Color.Black else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameModeSelector(
    currentMode: GreetingNameMode,
    onModeSelected: (GreetingNameMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GreetingNameMode.entries.forEach { mode ->
            val selected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) AutoColors.selectedBg else Color.Transparent)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        GreetingNameMode.CAR_NAME -> "Car"
                        GreetingNameMode.DRIVER_NAME -> "Driver"
                        GreetingNameMode.BOTH -> "Both"
                        GreetingNameMode.RANDOM -> "Random"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusBanner() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShieldIcon(
                modifier = Modifier.size(20.dp),
                color = AutoColors.success,
                strokeWidth = 1.5f
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SERVICE STATUS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp,
                )
                Text(
                    text = "Running \u00B7 Listening for Android Auto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val r = size.width * 0.4f * scale
                    drawCircle(color = AutoColors.success.copy(alpha = 0.4f), radius = r * 1.5f)
                    drawCircle(color = AutoColors.success, radius = r * 0.6f)
                }
            }
        }
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

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val read: Long, val total: Long) : DownloadState()
    data object Extracting : DownloadState()
    data object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}
