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
import kotlin.math.roundToInt

/**
 * Drag-to-reorder for a [androidx.compose.foundation.lazy.LazyColumn], driven from a per-row
 * handle: touching the handle starts the drag immediately, with no long press to discover.
 *
 * Two callbacks rather than one: [onMove] fires many times during a single drag, once per slot
 * the row is dragged past, and is meant to move the item in memory only;
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
    val anchorChannel = remember { Channel<ScrollAnchor>(Channel.CONFLATED) }

    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            draggableItemCount = { currentCount },
            onMove = { from, to -> currentOnMove(from, to) },
            onDragFinished = { currentOnDragFinished() },
            scrollChannel = scrollChannel,
            anchorChannel = anchorChannel
        )
    }

    // A lazy list keeps its scroll position anchored to the first visible item's key, so moving
    // a row into or out of that slot slides the whole viewport by a row. Put it back where it
    // was. (Foundation 1.8's requestScrollToItem does this within the same frame; on 1.7 this
    // is the way.)
    LaunchedEffect(state) {
        while (true) {
            val anchor = anchorChannel.receive()
            lazyListState.scrollToItem(anchor.index, anchor.offset)
        }
    }

    // Dragging past either edge feeds a scroll delta through here, so a long list can be
    // reordered without lifting the finger.
    LaunchedEffect(state) {
        while (true) {
            val delta = scrollChannel.receive()
            state.onAutoScrolled(lazyListState.scrollBy(delta))
        }
    }
    return state
}

/** Where the list was scrolled to before a move disturbed its anchor. */
internal data class ScrollAnchor(val index: Int, val offset: Int)

class DragDropState internal constructor(
    private val state: LazyListState,
    private val draggableItemCount: () -> Int,
    private val onMove: (Int, Int) -> Unit,
    private val onDragFinished: () -> Unit,
    private val scrollChannel: Channel<Float>,
    private val anchorChannel: Channel<ScrollAnchor>
) {
    /** Index of the row under the finger, or null when nothing is being dragged. */
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    /** Where the drag began, and the row height it steps in — both fixed for the whole gesture. */
    private var draggingItemStartIndex = 0
    private var draggingItemSize = 0

    /** Pixels the list auto-scrolled under a stationary finger, which move the row on too. */
    private var autoScrolledDistance = 0f

    /** A long-press that never crossed a neighbour changed nothing, so it must not write. */
    private var movedDuringDrag = false

    /** The handle that started the current drag; only it may end one. */
    private var dragOwner: Any? = null

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
     * only the row's current position and height, which anchor everything the drag measures
     * afterwards. They are read once here and never again: the list reshuffles under the finger,
     * so anything re-read mid-drag would be measuring against a list already halfway moved.
     */
    internal fun onDragStart(index: Int, owner: Any) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?.also {
                dragOwner = owner
                draggingItemIndex = it.index
                draggingItemStartIndex = it.index
                draggingItemInitialOffset = it.offset
                draggingItemSize = it.size
                draggedDistance = 0f
                autoScrolledDistance = 0f
                movedDuringDrag = false
            }
    }

    /** No-op unless [owner] is the handle that started the drag, so a stale row cannot end it. */
    internal fun onDragInterrupted(owner: Any) {
        if (dragOwner !== owner) return
        val shouldCommit = movedDuringDrag
        dragOwner = null
        draggingItemIndex = null
        draggedDistance = 0f
        draggingItemInitialOffset = 0
        draggingItemSize = 0
        autoScrolledDistance = 0f
        movedDuringDrag = false
        if (shouldCommit) onDragFinished()
    }

    /**
     * Auto-scrolling slides the rows past a finger that has not moved, which carries the dragged
     * row the same distance through the list. Counted apart from [draggedDistance] because the
     * drawn offset is measured against the row's live position, which the scroll already shifted.
     */
    internal fun onAutoScrolled(consumed: Float) {
        if (draggingItemIndex != null) autoScrolledDistance += consumed
    }

    internal fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        val dragging = draggingItemLayoutInfo ?: return
        val startOffset = dragging.offset + draggingItemOffset
        val endOffset = startOffset + dragging.size

        // How many slots the row has travelled, measured from where the drag began rather than
        // from the neighbours' current positions. Reading live positions back mid-reorder feeds
        // each move into the next one and the row runs away down the list — the finger stays
        // still while every frame counts another crossing. Rounding is what makes the midpoint
        // decide: the row swaps once it is more than half a slot past its neighbour.
        if (draggingItemSize > 0) {
            val travelled = draggedDistance + autoScrolledDistance
            val slots = (travelled / draggingItemSize).roundToInt()
            val target = (draggingItemStartIndex + slots)
                .coerceIn(0, (draggableItemCount() - 1).coerceAtLeast(0))
            if (target != dragging.index) {
                // Read the anchor before the move, while it still describes where the list sits.
                val anchorIndex = state.firstVisibleItemIndex
                val disturbsAnchor = dragging.index == anchorIndex || target == anchorIndex
                val anchor = ScrollAnchor(anchorIndex, state.firstVisibleItemScrollOffset)

                onMove(dragging.index, target)
                draggingItemIndex = target
                movedDuringDrag = true

                if (disturbsAnchor) anchorChannel.trySend(anchor)
                return
            }
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
 *
 * [index] is deliberately *not* a `pointerInput` key: the first move renumbers this very row,
 * and re-keying would cancel the gesture coroutine underneath the finger. Cancelling that way
 * skips `onDragEnd`/`onDragCancel`, which used to leave the row lifted forever with the new
 * order never written. It is read through [rememberUpdatedState] instead, so the gesture keeps
 * running while still starting from wherever the row currently sits.
 */
@Composable
fun Modifier.dragHandle(dragDropState: DragDropState, index: Int): Modifier {
    val currentIndex by rememberUpdatedState(index)
    // Identifies this handle for the duration of the drag, so the clean-up below only tears down
    // a drag this row actually owns.
    val owner = remember { Any() }
    return this.pointerInput(dragDropState) {
        try {
            detectDragGestures(
                onDragStart = { dragDropState.onDragStart(currentIndex, owner) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(dragAmount)
                },
                onDragEnd = { dragDropState.onDragInterrupted(owner) },
                onDragCancel = { dragDropState.onDragInterrupted(owner) }
            )
        } finally {
            // The row can still be disposed mid-drag — auto-scrolling can carry it out of the
            // viewport. No pointer callback arrives for that, so end the drag here rather than
            // stranding the list in its dragging state.
            dragDropState.onDragInterrupted(owner)
        }
    }
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
