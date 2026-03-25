package com.familyvoice.reminders.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.familyvoice.reminders.domain.model.RecordingState
import com.familyvoice.reminders.ui.theme.RecordRed
import com.familyvoice.reminders.ui.theme.RecordRedDark
import com.familyvoice.reminders.ui.theme.RecordRedLight

/**
 * The central record button.
 *
 * Gesture contract:
 *  - Press & hold  → [onPressStart]  (begin/resume recording)
 *  - Release       → [onRelease]     (pause recording — keeps buffer)
 *  - Swipe UP      → [onSwipeUp]     (finalise and send to AI)
 *  - Swipe DOWN    → [onSwipeDown]   (discard recording)
 *
 * [swipeThresholdDp] is the minimum vertical travel required to trigger a swipe
 * rather than a plain release. Default 80 dp.
 */
@Composable
fun RecordButton(
    recordingState: RecordingState,
    onPressStart: () -> Unit,
    onRelease: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    swipeThresholdDp: Dp = 80.dp,
) {
    // ── Pulse animation (only while actively recording) ──────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // Scale: pulse when recording, shrink slightly when idle/paused
    val buttonScale by animateFloatAsState(
        targetValue = when (recordingState) {
            RecordingState.Recording   -> pulseScale
            RecordingState.Paused      -> 0.92f
            RecordingState.Processing  -> 0.85f
            RecordingState.Idle        -> 1f
        },
        label = "buttonScale",
    )

    val buttonColor: Color = when (recordingState) {
        RecordingState.Recording  -> RecordRedLight
        RecordingState.Paused     -> RecordRed
        RecordingState.Processing -> RecordRedDark
        RecordingState.Idle       -> RecordRed
    }

    // ── Gesture detection ────────────────────────────────────────────────────
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(buttonScale)
            .background(color = buttonColor, shape = CircleShape)
            .pointerInput(Unit) {
                val swipeThresholdPx = swipeThresholdDp.toPx()

                awaitEachGesture {
                    // 1. Wait for finger down
                    val firstTouch = awaitFirstDown(requireUnconsumed = false)
                    firstTouch.consume()
                    onPressStart()

                    var totalDragY = 0f

                    // 2. Track movement until finger lifts
                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (change.pressed) {
                            totalDragY += change.positionChange().y
                            change.consume()
                        } else {
                            // 3. Finger lifted — classify the gesture
                            change.consume()
                            when {
                                totalDragY < -swipeThresholdPx -> onSwipeUp()
                                totalDragY >  swipeThresholdPx -> onSwipeDown()
                                else                           -> onRelease()
                            }
                            break
                        }
                    }
                }
            },
    ) {
        Icon(
            imageVector        = Icons.Default.Mic,
            contentDescription = "Record",
            tint               = Color.White,
            modifier           = Modifier.size(size * 0.45f),
        )
    }
}
