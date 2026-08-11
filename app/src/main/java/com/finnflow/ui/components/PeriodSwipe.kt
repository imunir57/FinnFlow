package com.finnflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** How far a horizontal drag must travel before releasing it counts as a period change. */
private val SwipeThreshold = 56.dp

/** A fling this fast commits the change even if it never reached [SwipeThreshold]. */
private const val FlingVelocity = 450f

/** Fraction of the width the content may follow the finger before it starts resisting. */
private const val FreeDragFraction = 0.32f

/** How much of the drag still registers once past the free zone — the rubber-band pull. */
private const val ResistedDragFactor = 0.30f

/** How far the panel fades out as it travels a full width, so the exit reads as a hand-off. */
private const val FadeAtFullWidth = 0.45f

/**
 * Horizontal fling that steps a screen's period — the gesture equivalent of the ‹ › arrows.
 *
 * The content tracks the finger while the drag is in progress, so the gesture answers before it
 * is released: without that the swipe is invisible until the data swaps, and a drag that fell
 * short of the threshold gives no clue why nothing happened. Releasing either carries the panel
 * off screen and brings the new period in from the opposite edge, or springs it back.
 *
 * Dragging the content towards the start edge asks for the *next* period, matching how the
 * arrows read on screen, and the two are mirrored under RTL so the gesture never contradicts
 * them. Around a scrolling child it costs that child nothing: a vertical drag is consumed by the
 * list first, which cancels this detector before it passes its own slop.
 */
@Composable
fun PeriodSwipeBox(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val next = rememberUpdatedState(onNext)
    val previous = rememberUpdatedState(onPrevious)

    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // A period the gesture cannot step (Stats' custom range) must not leave the panel parked
    // off-centre if the mode changed mid-drag.
    LaunchedEffect(enabled) {
        if (!enabled && offset.value != 0f) offset.snapTo(0f)
    }

    val gesture = if (!enabled) Modifier else Modifier.pointerInput(rtl, threshold) {
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onDragCancel = {
                scope.launch { offset.animateTo(0f, settleSpring()) }
            },
            onDragEnd = {
                val velocity = tracker.calculateVelocity().x
                val travelled = offset.value
                // Direction comes from the release, not the accumulated offset: a flick back
                // across centre should follow the flick.
                val committed = abs(travelled) >= threshold || abs(velocity) >= FlingVelocity
                val towardsStart = if (abs(velocity) >= FlingVelocity) velocity < 0f
                                   else travelled < 0f
                if (committed && width > 0) {
                    val forward = if (rtl) !towardsStart else towardsStart
                    scope.launch {
                        val exit = if (towardsStart) -width.toFloat() else width.toFloat()
                        offset.animateTo(
                            targetValue = exit,
                            animationSpec = tween(150, easing = FastOutLinearInEasing),
                            initialVelocity = velocity
                        )
                        if (forward) next.value() else previous.value()
                        offset.snapTo(-exit)
                        offset.animateTo(0f, settleSpring())
                    }
                } else {
                    scope.launch { offset.animateTo(0f, settleSpring()) }
                }
            }
        ) { change, dragAmount ->
            tracker.addPosition(change.uptimeMillis, change.position)
            val free = width * FreeDragFraction
            // Past the free zone the panel keeps moving but lags the finger, so the limit is
            // felt rather than hit as a wall.
            val resisted = if (abs(offset.value) < free || sign(dragAmount) != sign(offset.value)) {
                dragAmount
            } else {
                dragAmount * ResistedDragFactor
            }
            scope.launch { offset.snapTo(offset.value + resisted) }
            change.consume()
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .then(gesture)
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = offset.value
                alpha = if (width == 0) 1f
                        else 1f - FadeAtFullWidth * (abs(offset.value) / width).coerceIn(0f, 1f)
            },
            content = content
        )
    }
}

private fun settleSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)
