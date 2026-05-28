package com.hrishi.yourcartalks.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun initialize(onReady: () -> Unit) {
        shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                isReady = true
                onReady()
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) return
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            @Suppress("DEPRECATION")
            override fun onStart(uttId: String?) {}
            override fun onDone(uttId: String?) {
                onDone?.invoke()
            }
            @Suppress("DEPRECATION")
            override fun onError(uttId: String?) {}
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "greeting")
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun shutdown() {
        isReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
