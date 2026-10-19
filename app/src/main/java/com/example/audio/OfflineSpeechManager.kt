package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

class OfflineSpeechManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerAvailable = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var amplitudeSimulationJob: Job? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
        isRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startAmplitudeSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopAmplitudeSimulation()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopAmplitudeSimulation()
                }
            })
        }
    }

    fun speak(text: String, languageCode: String = "en", pitch: Float = 1.0f, speechRate: Float = 1.0f) {
        if (!isTtsReady || tts == null) {
            // Fallback: simulate speech animation
            scope.launch {
                _isSpeaking.value = true
                startAmplitudeSimulation()
                delay((text.length * 50L).coerceIn(1500L, 4500L))
                _isSpeaking.value = false
                stopAmplitudeSimulation()
            }
            return
        }

        val locale = when (languageCode.lowercase()) {
            "hi", "hindi" -> Locale("hi", "IN")
            "mr", "marathi" -> Locale("mr", "IN")
            "hi-en", "hinglish" -> {
                // Check if Hindi is available, else use Indian English
                val hiLocale = Locale("hi", "IN")
                if (tts?.isLanguageAvailable(hiLocale) in listOf(TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                    hiLocale
                } else {
                    Locale("en", "IN")
                }
            }
            "gu", "gujarati" -> Locale("gu", "IN")
            "bn", "bengali" -> Locale("bn", "IN")
            "ta", "tamil" -> Locale("ta", "IN")
            "te", "telugu" -> Locale("te", "IN")
            "kn", "kannada" -> Locale("kn", "IN")
            "es", "spanish" -> Locale("es", "ES")
            "fr", "french" -> Locale("fr", "FR")
            "de", "german" -> Locale("de", "DE")
            else -> Locale("en", "IN").takeIf { (tts?.isLanguageAvailable(it) ?: -1) >= 0 } ?: Locale.US
        }

        try {
            val availability = tts?.isLanguageAvailable(locale)
            if (availability != null && availability >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = locale
                // Select the highest quality natural voice for Hindi, Marathi, or English if available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val matchingVoices = tts?.voices?.filter { v ->
                            v.locale.language.equals(locale.language, ignoreCase = true)
                        }
                        val bestVoice = matchingVoices?.maxByOrNull { it.quality } ?: matchingVoices?.firstOrNull()
                        if (bestVoice != null) {
                            tts?.voice = bestVoice
                        }
                    } catch (_: Exception) {}
                }
            } else {
                // Fallback to default or English
                tts?.language = Locale.getDefault().takeIf { (tts?.isLanguageAvailable(it) ?: -1) >= 0 } ?: Locale.US
            }
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate.coerceIn(0.85f, 1.05f))
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "IRA_SPEECH_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            // Simulated speech fallback
            scope.launch {
                _isSpeaking.value = true
                startAmplitudeSimulation()
                delay(2000)
                _isSpeaking.value = false
                stopAmplitudeSimulation()
            }
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopAmplitudeSimulation()
    }

    fun startListening(languageCode: String = "hi", onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (_isListening.value) return

        if (!isRecognizerAvailable) {
            // Fallback: Simulated voice capture prompt
            _isListening.value = true
            startListeningWaveform()
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialTranscript.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS (dB ranges usually -2 to 10)
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        _audioAmplitude.value = norm
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        stopAmplitudeSimulation()
                        onError("Speech recognition error: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        stopAmplitudeSimulation()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: ""
                        _partialTranscript.value = spoken
                        if (spoken.isNotBlank()) {
                            onResult(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _partialTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val localeTag = when (languageCode.lowercase()) {
                "hi", "hindi" -> "hi-IN"
                "mr", "marathi" -> "mr-IN"
                else -> "en-IN"
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "mr-IN", "en-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError(e.localizedMessage ?: "Microphone initialization error")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        stopAmplitudeSimulation()
    }

    private fun startAmplitudeSimulation() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = scope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive && (_isSpeaking.value || _isListening.value)) {
                step += 0.35f
                val wave = ((sin(step) + 1f) / 2f) * 0.7f + Random.nextFloat() * 0.3f
                _audioAmplitude.value = wave.coerceIn(0.1f, 1.0f)
                delay(40)
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun startListeningWaveform() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = scope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive && _isListening.value) {
                step += 0.25f
                val wave = ((sin(step) + 1f) / 2f) * 0.5f + Random.nextFloat() * 0.4f
                _audioAmplitude.value = wave.coerceIn(0.15f, 0.95f)
                delay(50)
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeSimulationJob?.cancel()
        amplitudeSimulationJob = null
        _audioAmplitude.value = 0f
    }

    fun release() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
