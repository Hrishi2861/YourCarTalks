package com.hrishi.yourcartalks

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.car.app.connection.CarConnection
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hrishi.yourcartalks.data.GreetingMessages
import com.hrishi.yourcartalks.data.GreetingNameMode
import com.hrishi.yourcartalks.data.TtsMethod
import com.hrishi.yourcartalks.tts.TextToSpeechManager
import com.hrishi.yourcartalks.tts.sherpa.SherpaOnnxManager

class GreetingService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "greeting_service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "YourCarTalks"

        fun start(context: Context) {
            Log.d(TAG, "Starting service...")
            if (Build.VERSION.SDK_INT >= 34 &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "BLUETOOTH_CONNECT not granted, skipping service start")
                return
            }
            val intent = Intent(context, GreetingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var ttsManager: TextToSpeechManager? = null
    private var sherpaMale: SherpaOnnxManager? = null
    private var sherpaFemale: SherpaOnnxManager? = null
    private var sherpaKokoro: SherpaOnnxManager? = null
    private var hasGreetedThisSession = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        ttsManager = TextToSpeechManager(this)
        sherpaMale = SherpaOnnxManager(this, SherpaOnnxManager.MALE)
        sherpaFemale = SherpaOnnxManager(this, SherpaOnnxManager.FEMALE)
        sherpaKokoro = SherpaOnnxManager(this, SherpaOnnxManager.KOKORO)

        CarConnection(this).type.observe(this) { type ->
            Log.d(TAG, "Car connection type changed: $type")
            when (type) {
                CarConnection.CONNECTION_TYPE_PROJECTION -> {
                    Log.d(TAG, "Android Auto projection connected")
                    try {
                        startForeground(NOTIFICATION_ID, buildNotification("Connected to Android Auto"))
                    } catch (_: SecurityException) { }
                    if (!hasGreetedThisSession) {
                        hasGreetedThisSession = true
                        speakGreeting()
                    } else {
                        Log.d(TAG, "Already greeted this session, skipping")
                    }
                }
                CarConnection.CONNECTION_TYPE_NATIVE -> {
                    Log.d(TAG, "Android Automotive (native) connected")
                    try {
                        startForeground(NOTIFICATION_ID, buildNotification("Connected to Android Auto"))
                    } catch (_: SecurityException) { }
                }
                CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> {
                    Log.d(TAG, "Car disconnected, resetting")
                    hasGreetedThisSession = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }

        try {
            startForeground(NOTIFICATION_ID, buildNotification("YourCarTalks"))
        } catch (e: SecurityException) {
            Log.w(TAG, "startForeground failed: ${e.message}")
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YourCarTalks")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun speakGreeting() {
        Log.d(TAG, "speakGreeting() called, launching coroutine with 2s delay")
        lifecycleScope.launch {
            Log.d(TAG, "Coroutine started, delaying 2s...")
            delay(2000L)
            Log.d(TAG, "Delay complete, fetching car name")

            val prefs = (application as YourCarTalksApp).preferencesManager
            val carName = prefs.getCarName()
            Log.d(TAG, "Car name: '$carName'")

            if (carName.isBlank()) {
                Log.w(TAG, "Car name is blank, skipping greeting")
                return@launch
            }

            val driverName = prefs.getDriverName()
            val selectedGreeting = prefs.getSelectedGreeting()
            val nameMode = prefs.getNameMode()

            val greeting = when (nameMode) {
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
            Log.d(TAG, "Greeting text: '$greeting'")

            val method = prefs.getTtsMethod()
            when (method) {
                TtsMethod.SHERPA_MALE -> {
                    if (sherpaMale?.isModelDownloaded() == true) {
                        speakWithSherpa(greeting, sherpaMale!!)
                    } else {
                        speakWithSystemTts(greeting)
                    }
                }
                TtsMethod.SHERPA_FEMALE -> {
                    if (sherpaFemale?.isModelDownloaded() == true) {
                        speakWithSherpa(greeting, sherpaFemale!!)
                    } else {
                        speakWithSystemTts(greeting)
                    }
                }
                TtsMethod.KOKORO -> {
                    if (sherpaKokoro?.isModelDownloaded() == true) {
                        speakWithSherpa(greeting, sherpaKokoro!!)
                    } else {
                        speakWithSystemTts(greeting)
                    }
                }
                TtsMethod.SYSTEM -> speakWithSystemTts(greeting)
            }
        }
    }

    private fun speakWithSherpa(text: String, manager: SherpaOnnxManager) {
        Log.d(TAG, "Using Sherpa-ONNX TTS")
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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
                Log.d(TAG, "Sherpa-ONNX utterance completed")
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            Log.w(TAG, "Audio focus not granted")
        }
    }

    private fun speakWithSystemTts(text: String) {
        Log.d(TAG, "Using system TTS")
        ttsManager?.initialize {
            requestAudioFocusAndSpeak(text)
        }
    }

    private fun requestAudioFocusAndSpeak(text: String) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        Log.d(TAG, "Audio focus request result: $result")
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            ttsManager?.speak(text) {
                Log.d(TAG, "TTS utterance completed")
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            Log.w(TAG, "Audio focus not granted")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.service_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        ttsManager?.shutdown()
        ttsManager = null
        sherpaMale?.shutdown()
        sherpaMale = null
        sherpaFemale?.shutdown()
        sherpaFemale = null
        sherpaKokoro?.shutdown()
        sherpaKokoro = null
        super.onDestroy()
    }
}
