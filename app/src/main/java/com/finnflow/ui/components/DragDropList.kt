package com.finnflow.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel

/**
 * Drag-to-reorder for a [androidx.compose.foundation.lazy.LazyColumn], driven from a per-row
 * handle: touching the handle starts the drag immediately, with no long press to discover.
 *
 * Two callbacks rather than one: [onMove] fires many times during a single drag, every time the
 * dragged row's midpoint crosses a neighbour, and is meant to move the item in memory only;
 * [onDragFinished] fires once when the finger lifts, which is where the new order gets written.
 * Persisting on every crossing would issue a database write per row passed.
 *
 * [draggableItemCount] is how many items at the *start* of the list can be dragged. Everything
 * after that — an archived section, a footer — keeps its place and cannot be dropped onto,
 * which is what lets indices here be read directly as indices into the reorderable list.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    draggableItemCount: Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragFinished: () -> Unit
): DragDropState {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)
    val currentCount by rememberUpdatedState(draggableItemCount)
    val scrollChannel = remember { Channel<Float>(Channel.CONFLATED) }

    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            draggableItemCount = { currentCount },
            onMove = { from, to -> currentOnMove(from, to) },
            onDragFinished = { currentOnDragFinished() },
            scrollChannel = scrollChannel
        )
    }

    // Dragging past either edge feeds a scroll delta through here, so a long list can be
    // reordered without lifting the finger.
    LaunchedEffect(state) {
        while (true) {
            lazyListState.scrollBy(scrollChannel.receive())
        }
    }
    return state
}

class DragDropState internal constructor(
    private val state: LazyListState,
    private val draggableItemCount: () -> Int,
    private val onMove: (Int, Int) -> Unit,
    private val onDragFinished: () -> Unit,
    private val scrollChannel: Channel<Float>
) {
    /** Index of the row under the finger, or null when nothing is being dragged. */
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    /** A long-press that never crossed a neighbour changed nothing, so it must not write. */
    private var movedDuringDrag = false

    /** How far the dragged row is drawn from where the list would otherwise place it. */
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggedDistance - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    /**
     * Starts a drag on a known row.
     *
     * The handle sits inside the item, so the index comes for free and no hit-test is needed —
     * only the row's current offset, which anchors everything the drag measures afterwards.
     */
    internal fun onDragStart(index: Int) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    internal fun onDragInterrupted() {
        val shouldCommit = movedDuringDrag
        draggingItemIndex = null
        draggedDistance = 0f
        draggingItemInitialOffset = 0
        movedDuringDrag = false
        if (shouldCommit) onDragFinished()
    }

    internal fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        val dragging = draggingItemLayoutInfo ?: return
        val startOffset = dragging.offset + draggingItemOffset
        val endOffset = startOffset + dragging.size
        // The midpoint decides, so a row swaps once it is more than half way past its
        // neighbour rather than the instant its edge touches.
        val middleOffset = startOffset + dragging.size / 2f

        val target = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != dragging.index &&
                    item.index < draggableItemCount() &&
                    middleOffset.toInt() in item.offset..(item.offset + item.size)
        }

        if (target != null) {
            onMove(dragging.index, target.index)
            draggingItemIndex = target.index
            movedDuringDrag = true
            return
        }

        val overscroll = when {
            draggedDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
            draggedDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
            else -> 0f
        }
        if (overscroll != 0f) scrollChannel.trySend(overscroll)
    }
}

/**
 * Attach to the drag handle of the row at [index].
 *
 * Drags start on touch rather than after a long press. The handle is a small target nested
 * inside the list, so it claims the pointer before the list's own scroll gesture sees it —
 * a swipe anywhere else on the row still scrolls as usual.
 */
fun Modifier.dragHandle(dragDropState: DragDropState, index: Int): Modifier =
    pointerInput(dragDropState, index) {
        detectDragGestures(
            onDragStart = { dragDropState.onDragStart(index) },
            onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.onDrag(dragAmount)
            },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }

/**
 * Wraps one row so it follows the finger while dragged and animates into place otherwise.
 *
 * [content] receives whether this row is the one being dragged, so it can lift itself visually.
 */
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val isDragging = index == dragDropState.draggingItemIndex
    val dragModifier = if (isDragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
    } else {
        Modifier.animateItem()
    }

    Column(modifier = modifier.then(dragModifier)) {
        content(isDragging)
    }
}
