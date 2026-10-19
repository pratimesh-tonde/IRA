package com.example.agent

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class IraBackgroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var partialWakeLock: PowerManager.WakeLock? = null
    private var isTorchOn = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningWakeWord = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val SILENT_CHANNEL_ID = "ira_silent_bg_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.ira.ACTION_START_BG"
        const val ACTION_STOP = "com.example.ira.ACTION_STOP_BG"
        const val ACTION_TOGGLE_TORCH = "com.example.ira.ACTION_TOGGLE_TORCH"
        const val ACTION_WAKE_AND_LISTEN = "com.example.ira.ACTION_WAKE_AND_LISTEN"
        const val ACTION_TOGGLE_WAKE_WORD = "com.example.ira.ACTION_TOGGLE_WAKE_WORD"

        const val EXTRA_WAKE_WORD_ACTIVATED = "com.example.ira.EXTRA_WAKE_WORD_ACTIVATED"
        const val EXTRA_WAKE_COMMAND = "com.example.ira.EXTRA_WAKE_COMMAND"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isWakeWordActive = MutableStateFlow(true)
        val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

        private val _lastWakeEvent = MutableStateFlow("Ready for 'IRA'")
        val lastWakeEvent: StateFlow<String> = _lastWakeEvent.asStateFlow()

        val WAKE_WORDS = listOf(
            "hey ira", "ira", "hello ira", "ok ira", "hi ira", "listen ira",
            "आईरा", "इरा", "हे इरा", "सुनो आईरा", "ऐक इरा", "आयरा", "आरा", "namaste ira"
        )

        fun start(context: Context) {
            val intent = Intent(context, IraBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IraBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggleWakeWord(context: Context) {
            val intent = Intent(context, IraBackgroundService::class.java).apply {
                action = ACTION_TOGGLE_WAKE_WORD
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        partialWakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IRA:BackgroundPersistentLock"
        )
        partialWakeLock?.setReferenceCounted(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfService()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_TORCH -> {
                toggleTorchInternal()
                updateNotification()
                return START_STICKY
            }
            ACTION_WAKE_AND_LISTEN -> {
                wakeDeviceAndTriggerAgent(command = "")
                updateNotification()
                return START_STICKY
            }
            ACTION_TOGGLE_WAKE_WORD -> {
                _isWakeWordActive.value = !_isWakeWordActive.value
                if (_isWakeWordActive.value) {
                    startContinuousWakeWordListening()
                } else {
                    stopWakeWordListening()
                }
                updateNotification()
                return START_STICKY
            }
            else -> {
                startForegroundWithProperType()
                _isRunning.value = true
                partialWakeLock?.acquire(60 * 60 * 1000L) // 1-hour safe wake lock

                if (_isWakeWordActive.value) {
                    startContinuousWakeWordListening()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithProperType() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            val hasAudioPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasAudioPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification, serviceType)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startContinuousWakeWordListening() {
        if (isListeningWakeWord) return
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission || !SpeechRecognizer.isRecognitionAvailable(this)) {
            _lastWakeEvent.value = "Wake Word Active (Tap to trigger on lack of recognizer)"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningWakeWord = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListeningWakeWord = false
                    }

                    override fun onError(error: Int) {
                        isListeningWakeWord = false
                        // Automatically restart wake word recognition after brief delay
                        if (_isWakeWordActive.value && _isRunning.value) {
                            mainHandler.postDelayed({
                                restartWakeWordListeningSafe()
                            }, 1200)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningWakeWord = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            evaluateSpokenPhrases(matches)
                        }

                        if (_isWakeWordActive.value && _isRunning.value) {
                            mainHandler.postDelayed({
                                restartWakeWordListeningSafe()
                            }, 500)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            evaluateSpokenPhrases(matches)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            isListeningWakeWord = true
        } catch (e: Exception) {
            isListeningWakeWord = false
        }
    }

    private fun restartWakeWordListeningSafe() {
        if (!_isRunning.value || !_isWakeWordActive.value) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            isListeningWakeWord = true
        } catch (e: Exception) {
            isListeningWakeWord = false
        }
    }

    private fun stopWakeWordListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListeningWakeWord = false
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun evaluateSpokenPhrases(phrases: List<String>) {
        for (raw in phrases) {
            val lower = raw.lowercase(Locale.ROOT).trim()
            for (wakeWord in WAKE_WORDS) {
                val idx = lower.indexOf(wakeWord)
                if (idx >= 0) {
                    val after = lower.substring(idx + wakeWord.length).trim()
                    val before = lower.substring(0, idx).trim()
                    val command = when {
                        after.isNotBlank() -> after
                        before.isNotBlank() -> before
                        else -> ""
                    }
                    _lastWakeEvent.value = "Wake Word: '$wakeWord' -> '$command'"
                    wakeDeviceAndTriggerAgent(command)
                    return
                }
            }
        }
    }

    fun wakeDeviceAndTriggerAgent(command: String) {
        // 1. Wake up the screen if in lock mode
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val screenWakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "IRA:LockScreenWakeTrigger"
        )
        screenWakeLock?.acquire(10000L) // 10s bright screen wake

        // 2. Haptic vibration feedback
        vibrateHaptic()

        // 3. Launch MainActivity over the lock screen
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAKE_WORD_ACTIVATED, true)
            if (command.isNotBlank()) {
                putExtra(EXTRA_WAKE_COMMAND, command)
            }
        }
        startActivity(wakeIntent)

        updateNotification()
    }

    private fun vibrateHaptic() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun toggleTorchInternal() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
                val statusText = if (isTorchOn) "Flashlight ON" else "Flashlight OFF"
                Toast.makeText(applicationContext, "IRA: $statusText", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Torch error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Silent, non-intrusive notification: satisfies Android foreground service with zero popup banner on top
        return NotificationCompat.Builder(this, SILENT_CHANNEL_ID)
            .setContentTitle("IRA")
            .setContentText("Background service active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setSilent(true)
            .setSound(null)
            .setVibrate(null)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            // Remove previous high-importance channel if present
            try {
                manager?.deleteNotificationChannel("ira_background_channel")
            } catch (_: Exception) {}

            val channel = NotificationChannel(
                SILENT_CHANNEL_ID,
                "IRA Background Assistant (Silent)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Silent background assistant and wake-word listener"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopSelfService() {
        _isRunning.value = false
        stopWakeWordListening()
        serviceScope.cancel()
        if (partialWakeLock?.isHeld == true) {
            partialWakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        stopWakeWordListening()
        serviceScope.cancel()
        if (partialWakeLock?.isHeld == true) {
            partialWakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
