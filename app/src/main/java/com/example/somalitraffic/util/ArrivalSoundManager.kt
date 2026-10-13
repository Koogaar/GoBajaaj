package com.example.somalitraffic.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ArrivalSoundManager — plays HORN SOUND ONLY (no TTS, no music).
 * Triggered when driver marks arrival at pickup.
 */
object ArrivalSoundManager {
    private const val TAG = "ArrivalSoundManager"

    /**
     * Plays full arrival alert: Bajaj Horn sequence + Vibration.
     * No TTS / no music — horn only as requested.
     */
    fun playDriverArrivalAlert(
        context: Context,
        riderName: String? = null,
        pickupLocation: String? = null
    ) {
        playBajajHornSequence(context)
    }

    /**
     * Plays loud rhythmic Bajaj Horn sequence (Tuut-Tuut-Tuuuut)
     */
    fun playBajajHornSequence(context: Context) {
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

                // Honk 3 (Longer — Bajaaj horn signature)
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 500)
                delay(560)
                toneGen.stopTone()
                delay(80)

                // Short final honk
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 160)
                delay(200)
                toneGen.stopTone()
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "ToneGenerator error: ${e.message}")
            }
        }
    }

    /**
     * Single quick Bajaj Horn beep
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

    /**
     * Navigation Cue: Short double-beep tone alerting driver that turn is approaching.
     * 100% audio tones, no speech / no music as requested.
     */
    fun playNavigationTurnBeep(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(160)
                toneGen.stopTone()
                delay(60)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
                delay(180)
                toneGen.stopTone()
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "Turn beep error: ${e.message}")
            }
        }
    }

    /**
     * Rerouting Alert: Short chime when route recalculates.
     */
    fun playRerouteBeep(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                delay(190)
                toneGen.stopTone()
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "Reroute beep error: ${e.message}")
            }
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

    /** No TTS to release */
    fun release() {}
}
