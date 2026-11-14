package com.example.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fires [onStep] immediately on press down, and if the user keeps holding past
 * [initialDelayMs], emits [onRepeatStep] every [repeatIntervalMs] until release
 * or cancellation while preserving Material ripple, press state, and TalkBack click semantics.
 */
fun Modifier.holdToRepeatClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    contentDescription: String,
    initialDelayMs: Long = 380L,
    repeatIntervalMs: Long = 90L,
    onStep: () -> Unit,
    onRepeatStep: () -> Unit = onStep
): Modifier = composed {
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnRepeatStep by rememberUpdatedState(onRepeatStep)

    this
        .indication(interactionSource = interactionSource, indication = indication)
        .semantics(mergeDescendants = false) {
            onClick(label = contentDescription) {
                currentOnStep()
                true
            }
        }
        .pointerInput(initialDelayMs, repeatIntervalMs) {
            val slopPx = viewConfiguration.touchSlop * 2.5f
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    currentOnStep()

                    val repeatJob = launch {
                        delay(initialDelayMs)
                        while (isActive) {
                            currentOnRepeatStep()
                            delay(repeatIntervalMs)
                        }
                    }

                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed || change.isConsumed) {
                            cancelled = change == null || change.isConsumed
                            break
                        }
                        if ((change.position - down.position).getDistance() > slopPx) {
                            cancelled = true
                            break
                        }
                    }

                    repeatJob.cancel()
                    if (cancelled) {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                    } else {
                        interactionSource.tryEmit(PressInteraction.Release(press))
                    }
                }
            }
        }
}
