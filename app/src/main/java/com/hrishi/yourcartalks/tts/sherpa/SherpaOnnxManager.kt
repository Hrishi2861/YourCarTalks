package com.hrishi.yourcartalks.tts.sherpa

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

data class SherpaModelConfig(
    val name: String,
    val onnxFile: String,
    val downloadUrl: URL,
    val displayName: String,
    val gender: String,
    val modelType: String = "vits",
    val voicesFile: String = "",
    val speakerId: Int = 0
)

class SherpaOnnxManager(private val context: Context, val config: SherpaModelConfig) {

    companion object {
        private const val TAG = "YourCarTalks"
        private const val MODEL_DIR_NAME = "sherpa-onnx-models"

        val MALE = SherpaModelConfig(
            name = "vits-piper-en_GB-alan-medium",
            onnxFile = "en_GB-alan-medium.onnx",
            downloadUrl = URL(
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_GB-alan-medium.tar.bz2"
            ),
            displayName = "Sherpa-ONNX (offline AI)",
            gender = "Male"
        )

        val FEMALE = SherpaModelConfig(
            name = "vits-piper-en_US-lessac-high",
            onnxFile = "en_US-lessac-high.onnx",
            downloadUrl = URL(
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-high.tar.bz2"
            ),
            displayName = "Sherpa-ONNX (offline AI)",
            gender = "Female"
        )

        val KOKORO = SherpaModelConfig(
            name = "kokoro-en-v0_19",
            onnxFile = "model.onnx",
            downloadUrl = URL(
                "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2"
            ),
            displayName = "Kokoro (offline AI)",
            gender = "British Female (Isabella)",
            modelType = "kokoro",
            voicesFile = "voices.bin",
            speakerId = 8
        )
    }

    data class DownloadProgress(
        val bytesRead: Long,
        val totalBytes: Long,
        val phase: String
    )

    private var tts: OfflineTts? = null
    private var track: AudioTrack? = null
    private var isPlaying = false
    private var currentAudioTrack: AudioTrack? = null

    private val modelBaseDir: File
        get() = File(context.filesDir, MODEL_DIR_NAME)

    val modelDir: File
        get() = File(modelBaseDir, config.name)

    val downloadUrl: URL
        get() = config.downloadUrl

    fun isModelDownloaded(): Boolean {
        if (!modelDir.exists()) return false
        if (!File(modelDir, config.onnxFile).exists()) return false
        if (!File(modelDir, "espeak-ng-data/phontab").exists()) return false
        return true
    }

    suspend fun downloadAndExtract(onProgress: ((DownloadProgress) -> Unit)? = null): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                modelBaseDir.mkdirs()

                onProgress?.invoke(DownloadProgress(0, 0, "Downloading model..."))

                val connection = config.downloadUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.connect()

                val totalBytes = connection.contentLengthLong
                val inputStream = BufferedInputStream(connection.inputStream)
                val tempFile = File(modelBaseDir, "model_${config.name}.tar.bz2")
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var lastReported = 0L
                var bytesInBuffer: Int

                while (inputStream.read(buffer).also { bytesInBuffer = it } != -1) {
                    outputStream.write(buffer, 0, bytesInBuffer)
                    bytesRead += bytesInBuffer
                    if (bytesRead - lastReported > 81920) {
                        lastReported = bytesRead
                        onProgress?.invoke(
                            DownloadProgress(bytesRead, totalBytes, "Downloading model...")
                        )
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                onProgress?.invoke(DownloadProgress(totalBytes, totalBytes, "Extracting model..."))

                modelDir.deleteRecursively()
                extractBz2Tar(tempFile, modelBaseDir)

                tempFile.delete()

                if (!isModelDownloaded()) {
                    return@withContext Result.failure(
                        Exception("Model extraction completed but ${config.onnxFile} not found")
                    )
                }

                onProgress?.invoke(DownloadProgress(totalBytes, totalBytes, "Done"))

                Result.success(modelDir)
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    private fun extractBz2Tar(bz2File: File, destDir: File) {
        val fin = java.io.FileInputStream(bz2File)
        val bin = BufferedInputStream(fin)
        val bz2In = BZip2CompressorInputStream(bin)
        val tarIn = TarArchiveInputStream(bz2In)

        var entry: ArchiveEntry? = tarIn.nextEntry
        while (entry != null) {
            val name = entry.name
            val outputFile = File(destDir, name)

            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile?.mkdirs()
                val fos = FileOutputStream(outputFile)
                val buf = ByteArray(8192)
                var len: Int
                while (tarIn.read(buf).also { len = it } != -1) {
                    fos.write(buf, 0, len)
                }
                fos.close()
            }
            entry = tarIn.nextEntry
        }

        tarIn.close()
        bz2In.close()
        bin.close()
        fin.close()
    }

    fun initTts(): Boolean {
        if (tts != null) return true
        if (!isModelDownloaded()) {
            Log.w(TAG, "Model not downloaded")
            return false
        }

        try {
            val modelPath = modelDir.absolutePath

            val modelConfig = if (config.modelType == "kokoro") {
                val kokoroConfig = OfflineTtsKokoroModelConfig(
                    model = "$modelPath/${config.onnxFile}",
                    voices = "$modelPath/${config.voicesFile}",
                    tokens = "$modelPath/tokens.txt",
                    dataDir = "$modelPath/espeak-ng-data",
                    lexicon = "",
                    lang = "en-US",
                    dictDir = "",
                    lengthScale = 1.0f
                )
                OfflineTtsModelConfig(
                    kokoro = kokoroConfig,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            } else {
                val vitsConfig = OfflineTtsVitsModelConfig(
                    model = "$modelPath/${config.onnxFile}",
                    tokens = "$modelPath/tokens.txt",
                    dataDir = "$modelPath/espeak-ng-data"
                )
                OfflineTtsModelConfig(
                    vits = vitsConfig,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            }

            val ttsConfig = OfflineTtsConfig(model = modelConfig)

            tts = OfflineTts(assetManager = null, config = ttsConfig)
            Log.i(TAG, "TTS initialized, sample rate: ${tts?.sampleRate()}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS: ${e.message}", e)
            return false
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (tts == null && !initTts()) {
            Log.w(TAG, "TTS not initialized")
            onDone?.invoke()
            return
        }

        Thread {
            try {
                val audio: GeneratedAudio = tts!!.generate(text = text, sid = config.speakerId, speed = 1.0f)

                if (audio.samples.isEmpty()) {
                    Log.w(TAG, "Generated audio is empty")
                    onDone?.invoke()
                    return@Thread
                }

                val sampleRate = audio.sampleRate
                val samples = audio.samples

                playSamples(samples, sampleRate)

                onDone?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "TTS generation failed: ${e.message}", e)
                onDone?.invoke()
            }
        }.apply { name = "sherpa-tts-playback" }.start()
    }

    private fun playSamples(samples: FloatArray, sampleRate: Int) {
        try {
            val numBytes = samples.size * 4

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .build()

            val track = AudioTrack(
                attrs, format, numBytes, AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            currentAudioTrack = track
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            track.play()

            val durationMs = (samples.size.toLong() * 1000) / sampleRate
            Thread.sleep(durationMs + 100)

            track.stop()
            track.release()
            currentAudioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback failed: ${e.message}", e)
        }
    }

    fun stop() {
        try {
            currentAudioTrack?.let {
                it.pause()
                it.flush()
                it.stop()
                it.release()
                currentAudioTrack = null
            }
        } catch (_: Exception) {
        }
        isPlaying = false
    }

    fun shutdown() {
        stop()
        tts?.let {
            try {
                it.release()
            } catch (_: Exception) {
            }
            tts = null
        }
    }
}
