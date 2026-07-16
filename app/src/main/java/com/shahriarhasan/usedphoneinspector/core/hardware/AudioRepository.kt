package com.shahriarhasan.usedphoneinspector.core.hardware

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SpeakerSample { SPOKEN, LOW_TONE, MID_TONE, HIGH_TONE, STEREO }

interface SpeakerTestController {
    fun play(sample: SpeakerSample, volume: Float = 0.7f)
    fun stop()
}

@Singleton
class AndroidSpeakerTestController @Inject constructor(
    @ApplicationContext context: Context,
) : SpeakerTestController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var track: AudioTrack? = null
    private var playbackJob: Job? = null
    private var ttsReady = false
    private val textToSpeech = TextToSpeech(context) { status ->
        ttsReady = status == TextToSpeech.SUCCESS
    }

    override fun play(sample: SpeakerSample, volume: Float) {
        stop()
        if (sample == SpeakerSample.SPOKEN) {
            if (ttsReady) {
                textToSpeech.language = Locale.getDefault()
                textToSpeech.speak(
                    contextSampleText(textToSpeech.language),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "speaker-inspection",
                )
            }
            return
        }
        playbackJob = scope.launch {
            val stereo = sample == SpeakerSample.STEREO
            val samples = generateSamples(
                frequency = when (sample) {
                    SpeakerSample.LOW_TONE -> 180.0
                    SpeakerSample.MID_TONE -> 1_000.0
                    SpeakerSample.HIGH_TONE -> 6_000.0
                    else -> 750.0
                },
                stereo = stereo,
            )
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(if (stereo) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track = audioTrack
            audioTrack.write(samples, 0, samples.size)
            audioTrack.setVolume(volume.coerceIn(0f, 1f))
            audioTrack.play()
        }
    }

    override fun stop() {
        playbackJob?.cancel()
        textToSpeech.stop()
        track?.runCatching {
            stop()
            release()
        }
        track = null
    }

    private fun generateSamples(frequency: Double, stereo: Boolean): ShortArray {
        val frames = SAMPLE_RATE * 2
        val channels = if (stereo) 2 else 1
        return ShortArray(frames * channels) { index ->
            val frame = index / channels
            val channel = index % channels
            val stereoGain = if (!stereo || (frame < frames / 2 && channel == 0) ||
                (frame >= frames / 2 && channel == 1)) 1.0 else 0.0
            (kotlin.math.sin(2.0 * Math.PI * frequency * frame / SAMPLE_RATE) * Short.MAX_VALUE * 0.25 * stereoGain)
                .toInt().toShort()
        }
    }

    private fun contextSampleText(locale: Locale?): String =
        if (locale?.language == "bn") "স্পিকার পরীক্ষার শব্দ পরিষ্কারভাবে শুনুন" else "Listen for clear sound from the speaker"

    private companion object { const val SAMPLE_RATE = 44_100 }
}

enum class RecorderStatus { IDLE, RECORDING, RECORDED, PLAYING, ERROR }

data class MicrophoneState(
    val status: RecorderStatus = RecorderStatus.IDLE,
    val elapsedMillis: Long = 0,
    val amplitude: Int = 0,
    val filePath: String? = null,
    val errorCode: String? = null,
)

interface MicrophoneTestController {
    val hasMicrophone: Boolean
    val state: StateFlow<MicrophoneState>
    fun start(maxDurationMillis: Long = 10_000)
    fun stop()
    fun play()
    fun delete()
    fun retainAsEvidence(): File?
    fun release()
}

@Singleton
class AndroidMicrophoneTestController @Inject constructor(
    @ApplicationContext private val context: Context,
) : MicrophoneTestController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(MicrophoneState())
    override val state: StateFlow<MicrophoneState> = mutableState
    override val hasMicrophone: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var meterJob: Job? = null

    override fun start(maxDurationMillis: Long) {
        if (!hasMicrophone) {
            mutableState.value = MicrophoneState(status = RecorderStatus.ERROR, errorCode = "UNSUPPORTED")
            return
        }
        delete()
        val directory = File(context.cacheDir, "audio").apply { mkdirs() }
        val output = File(directory, "microphone-${UUID.randomUUID()}.m4a")
        try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(output.absolutePath)
                setMaxDuration(maxDurationMillis.toInt())
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stop()
                }
                prepare()
                start()
            }
            recorder = mediaRecorder
            mutableState.value = MicrophoneState(status = RecorderStatus.RECORDING, filePath = output.absolutePath)
            val startedAt = System.currentTimeMillis()
            meterJob = scope.launch {
                while (isActive && recorder != null) {
                    val elapsed = System.currentTimeMillis() - startedAt
                    val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    mutableState.value = mutableState.value.copy(elapsedMillis = elapsed, amplitude = amplitude)
                    delay(100)
                }
            }
        } catch (_: SecurityException) {
            output.delete()
            mutableState.value = MicrophoneState(status = RecorderStatus.ERROR, errorCode = "PERMISSION_DENIED")
        } catch (_: RuntimeException) {
            output.delete()
            mutableState.value = MicrophoneState(status = RecorderStatus.ERROR, errorCode = "INITIALIZATION_FAILED")
        }
    }

    override fun stop() {
        meterJob?.cancel()
        val currentFile = mutableState.value.filePath
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        if (currentFile != null && File(currentFile).length() > 0) {
            mutableState.value = mutableState.value.copy(status = RecorderStatus.RECORDED, amplitude = 0)
        } else {
            currentFile?.let(::File)?.delete()
            mutableState.value = MicrophoneState(status = RecorderStatus.ERROR, errorCode = "RECORDING_INTERRUPTED")
        }
    }

    override fun play() {
        val path = mutableState.value.filePath ?: return
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            setDataSource(path)
            setOnCompletionListener {
                mutableState.value = mutableState.value.copy(status = RecorderStatus.RECORDED)
                it.release()
                player = null
            }
            prepare()
            start()
        }
        mutableState.value = mutableState.value.copy(status = RecorderStatus.PLAYING)
    }

    override fun delete() {
        meterJob?.cancel()
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        player?.release()
        player = null
        mutableState.value.filePath?.let(::File)?.delete()
        mutableState.value = MicrophoneState()
    }

    override fun retainAsEvidence(): File? {
        val source = mutableState.value.filePath?.let(::File)?.takeIf(File::exists) ?: return null
        val targetDir = File(context.filesDir, "evidence").apply { mkdirs() }
        val target = File(targetDir, "microphone-${UUID.randomUUID()}.m4a")
        source.copyTo(target)
        delete()
        return target
    }

    override fun release() = delete()
}

