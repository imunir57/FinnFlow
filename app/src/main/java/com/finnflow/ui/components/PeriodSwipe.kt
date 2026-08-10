package com.finnflow.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** How far a horizontal drag must travel before it counts as a period change. */
private val SwipeThreshold = 56.dp

/**
 * Horizontal fling that steps a screen's period — the gesture equivalent of the ‹ › arrows.
 *
 * Dragging the content towards the start edge asks for the *next* period, matching how the
 * arrows read on screen, and the two are mirrored under RTL so the gesture never contradicts
 * them. Placed on a scrolling screen's parent it costs the child nothing: a vertical drag is
 * consumed by the list first, which cancels this detector before it passes its own slop.
 */
@Composable
fun periodSwipe(onNext: () -> Unit, onPrevious: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val next = rememberUpdatedState(onNext)
    val previous = rememberUpdatedState(onPrevious)

    return remember(threshold, rtl) {
        Modifier.pointerInput(Unit) {
            var travelled = 0f
            detectHorizontalDragGestures(
                onDragStart = { travelled = 0f },
                onDragCancel = { travelled = 0f },
                onDragEnd = {
                    if (abs(travelled) >= threshold) {
                        val forward = if (rtl) travelled > 0f else travelled < 0f
                        if (forward) next.value() else previous.value()
                    }
                }
            ) { change, dragAmount ->
                travelled += dragAmount
                change.consume()
            }
        }
    }
}
