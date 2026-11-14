package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class HapticFeedbackHelper(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun vibrateClick(durationMs: Long = 22L, amplitude: Int = 180) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .build()
                v.vibrate(effect, attrs)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                @Suppress("DEPRECATION")
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }
}

@Composable
fun rememberHapticHelper(): HapticFeedbackHelper {
    val context = LocalContext.current.applicationContext
    return remember(context) { HapticFeedbackHelper(context) }
}
