package com.example.somalitraffic.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

object ArrivalSoundManager {
    private const val TAG = "ArrivalSoundManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechText: String? = null

    /**
     * Initializes TTS engine lazily
     */
    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    // Try to set Somali or English language
                    val somaliLocale = Locale("so", "SO")
                    val result = tts?.setLanguage(somaliLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.US
                    }
                    pendingSpeechText?.let { text ->
                        speakText(text)
                        pendingSpeechText = null
                    }
                } else {
                    Log.e(TAG, "TTS Initialization failed: $status")
                }
            }
        }
    }

    /**
     * Plays full arrival alert: Bajaj Horn Chime + Voice TTS Announcement + Vibration
     */
    fun playDriverArrivalAlert(
        context: Context,
        riderName: String? = null,
        pickupLocation: String? = null
    ) {
        initTts(context)

        CoroutineScope(Dispatchers.Default).launch {
            // 1. Trigger distinct Bajaj double horn sound & vibration
            playBajajHornSequence(context)

            // 2. Play system notification chime
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
                ringtone?.play()
            } catch (e: Exception) {
                Log.w(TAG, "Ringtone play error: ${e.message}")
            }

            delay(700)

            // 3. Play voice announcement
            val spokenMessage = if (!pickupLocation.isNullOrBlank()) {
                "Bajaajlihii wuu soo gaaray $pickupLocation! Fadlan soo bax."
            } else {
                "Bajaajlihii wuu soo gaaray gurigaaga! Fadlan soo bax."
            }

            speakText(spokenMessage)
        }
    }

    /**
     * Plays loud rhythmic Bajaj Horn sound effect (Tuut-Tuut-Tuut)
     */
    fun playBajajHornSequence(context: Context) {
        // Haptic feedback / vibration
        triggerVibration(context)

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                // Honk 1
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
                delay(260)
                toneGen.stopTone()
                delay(120)

                // Honk 2
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
                delay(260)
                toneGen.stopTone()
                delay(120)

                // Honk 3 (Longer)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 380)
                delay(420)
                toneGen.stopTone()
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "ToneGenerator error: ${e.message}")
                try {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context.applicationContext, defaultUri)
                    r?.play()
                } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Plays single quick Bajaj Horn beep
     */
    fun playSingleHornBeep(context: Context) {
        triggerVibration(context, durationMs = 180)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
                delay(320)
                toneGen.stopTone()
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "Single horn error: ${e.message}")
            }
        }
    }

    private fun speakText(text: String) {
        try {
            if (isTtsInitialized && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ARRIVAL_ALERT_UTTERANCE")
            } else {
                pendingSpeechText = text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Speak text error: ${e.message}")
        }
    }

    private fun triggerVibration(context: Context, durationMs: Long = 400) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200, 100, durationMs),
                    -1
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200, 100, durationMs),
                        -1
                    )
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator failed: ${e.message}")
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsInitialized = false
        } catch (e: Exception) {
            Log.w(TAG, "TTS release error: ${e.message}")
        }
    }
}
