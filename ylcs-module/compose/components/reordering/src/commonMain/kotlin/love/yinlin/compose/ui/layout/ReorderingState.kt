package love.yinlin.compose.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.PinnableContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import love.yinlin.compose.extension.mutableOffsetStateOf
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.layout.ReorderingLayout.Info
import love.yinlin.compose.ui.layout.ReorderingLayout.Item
import love.yinlin.extension.cleaning
import kotlin.math.min

/**
 * 基于稳定 key 的插入式排序，直接用于现有 Lazy 容器，不持有业务列表。
 *
 * `onMove(from, to)` 的两个索引均属于本次移动前的整个容器（包含 header），
 * 调用方必须同步执行 `add(to, removeAt(from))`，不需要在向后移动时减一。
 * 连续移动时 from 是拖动项的当前索引，不是手势开始时的索引。
 * `onSettle` 只在有效移动结束后调用一次，参数为手势开始、结束时的索引。
 * 手势取消与松手相同，保留已经完成的移动。
 *
 * 数据源切换、插入或删除前应调用 [cancelDrag]；它终止手势且不触发 onSettle，
 * 不回滚已经由 onMove 修改的数据。key 必须唯一且在移动期间保持不变。
 */
@Stable
class ReorderingState internal constructor(
    internal val layout: ReorderingLayout,
    private val scope: CoroutineScope,
    private val scrollThreshold: Float,
    private val onMove: State<(Int, Int) -> Unit>,
    private val onSettle: State<(Int, Int) -> Unit>,
    private val settleAfterAnimation: Boolean = false,
) {
    private class Gesture(val key: Any, val origin: Offset, val handle: Offset, val initial: Info, val pin: PinnableContainer.PinnedHandle?) {
        var delta by mutableOffsetStateOf(Offset.Zero)
        var released by mutableStateOf(false)
        var processedPosition = origin
        var scrollMotion = Offset.Zero
        var startIndex = -1
        var endIndex = -1
        var hasDragged = false
    }

    private class Move(val key: Any, val from: Int, val to: Int, val targetKey: Any, val before: Info, val time: Long) {
        private val displacedIndices = before.items.filter { it.index in minOf(from, to) .. maxOf(from, to) }.associate { (val index, val key1 = key) -> key1 to shiftedIndex(index) }

        private fun shiftedIndex(index: Int): Int = when {
            index == from -> to
            from < to -> index - 1
            else -> index + 1
        }

        fun applied(info: Info): Boolean {
            if (info === before) return false
            val item = info.item(key)
            if (item != null) return item.index == to
            val target = info.item(targetKey)
            if (target != null) return target.index == shiftedIndex(to)
            return info.items.all { (val index, val key1 = key) -> displacedIndices[key1]?.let { it == index } != false }
        }
    }

    private class Drop(val key: Any, offset: Offset, var commit: (() -> Unit)?, val pin: PinnableContainer.PinnedHandle?) {
        val animation = Animatable(offset, Offset.VectorConverter)
        var job: Job? = null
        var released = false

        fun release() {
            if (!released) {
                released = true
                pin?.release()
            }
        }
    }

    private var gestures by mutableRefStateOf<List<Gesture>>([])
    private var drops by mutableRefStateOf<List<Drop>>([])
    private var pending: Move? = null
    private var driver: Job? = null
    private var generation = Any()
    private val knownIndices = HashMap<Any, Int>()
    internal val keys = HashSet<Any>()
    internal val positions = HashMap<Any, ReorderingItemPosition>()

    val isDragging: Boolean get() = gestures.any { !it.released }

    internal val dragOrientation: Orientation? get() = if (settleAfterAnimation) layout.info.orientation else null

    internal fun isDragging(key: Any): Boolean = gestures.any { it.key == key && !it.released }

    internal fun hasGesture(key: Any): Boolean = gestures.any { it.key == key }

    internal fun isFloating(key: Any): Boolean = hasGesture(key) || drops.any { it.key == key }

    internal fun offset(key: Any): Offset {
        val gesture = gestures.lastOrNull { it.key == key }
        val drop = drops.lastOrNull { it.key == key }
        if (gesture == null && drop == null) return Offset.Zero
        val info = layout.info
        val item = item(info, key) ?: return Offset.Zero
        val base = positions[key]?.position() ?: info.position(item)
        val position = gesture?.let { it.origin + it.delta } ?: (base + drop!!.animation.value)
        return constrain(info, item, position) - base
    }

    internal fun start(key: Any, handleCenter: Offset): Any? {
        if (isDragging || key !in keys) return null
        val info = layout.info
        val item = item(info, key) ?: return null
        if (info.main(item.size) <= 0f) return null
        val previous = gestures.lastOrNull { it.key == key }
        val base = positions[key]?.position() ?: info.position(item)
        val origin = constrain(info, item, previous?.let { it.origin + it.delta } ?: (base + (drops.lastOrNull { it.key == key }?.animation?.value ?: Offset.Zero)))
        val gesture = Gesture(key, origin, handleCenter, info, positions[key]?.pin())
        gestures = gestures + gesture
        ensureDriver()
        return gesture
    }

    internal fun drag(token: Any, amount: Offset) {
        val gesture = token as? Gesture ?: return
        if (gesture.released || gesture !in gestures) return
        val movement = when (dragOrientation) {
            Orientation.Vertical -> Offset(0f, amount.y)
            Orientation.Horizontal -> Offset(amount.x, 0f)
            null -> amount
        }
        if (movement != Offset.Zero) gesture.hasDragged = true
        gesture.delta += movement
    }

    internal fun stop(token: Any) {
        val gesture = token as? Gesture ?: return
        if (gesture !in gestures) return
        gesture.released = true
    }

    private fun ensureDriver() {
        if (driver?.isActive == true) return
        val owner = generation
        driver = scope.launch {
            cleaning({
                if (generation === owner) {
                    driver = null
                    if (gestures.isNotEmpty()) cancelDrag()
                }
            }) {
                var previousTime = 0L
                while (gestures.isNotEmpty()) {
                    val time = withFrameNanos { it }
                    val seconds = if (previousTime == 0L) 0f else ((time - previousTime) / 1000000000f).coerceIn(0f, 0.032f)
                    previousTime = time
                    val gesture = gestures.first()
                    val info = layout.info
                    if (info.totalItemsCount != gesture.initial.totalItemsCount || info.orientation != gesture.initial.orientation ||
                        info.mainAxisReversed != gesture.initial.mainAxisReversed || info.crossAxisReversed != gesture.initial.crossAxisReversed) {
                        cancelDrag()
                        break
                    }

                    val previousMove = pending
                    if (previousMove != null) {
                        if (!previousMove.applied(info)) {
                            if (time - previousMove.time > 1000000000L) cancelDrag()
                            continue
                        }
                        pending = null
                    }
                    knownIndices.keys.retainAll(positions.keys)
                    for ((val index, val key) in info.items) knownIndices[key] = index
                    val item = item(info, gesture.key)
                    if (item == null) {
                        finish(gesture, null)
                        continue
                    }
                    if (gesture.startIndex == -1) {
                        for (drop in drops) {
                            if (generation !== owner) break
                            commit(drop)
                        }
                        if (generation !== owner) break
                        drops.firstOrNull { it.key == gesture.key }?.let { removeDrop(it) }
                        gesture.startIndex = item.index
                        gesture.endIndex = item.index
                        layout.stopScroll()
                        if (layout.info !== info) continue
                    }

                    val position = constrain(info, item, gesture.origin + gesture.delta)
                    val movement = position - gesture.processedPosition + gesture.scrollMotion
                    gesture.processedPosition = position
                    gesture.scrollMotion = Offset.Zero
                    val target = ReorderingHitTest.target(info, item, position, movement, keys)
                    if (target != null) {
                        move(gesture, item, target, info, time)
                        continue
                    }
                    if (gesture.released) {
                        finish(gesture, item)
                        continue
                    }

                    val direction = scrollDirection(info, gesture)
                    if (direction != 0) {
                        val physicalDirection = if (info.mainAxisReversed) -direction else direction
                        val main = info.main(info.position(item))
                        val limit = if (physicalDirection > 0) main + info.main(item.size) - 1f else info.viewportSize - main - 1f
                        val outside = info.main(position) >= info.viewportSize || info.main(position) + info.main(item.size) <= 0f
                        if (limit <= 1f || outside) {
                            val edge = ReorderingHitTest.edgeTarget(info, item, position, direction, keys)
                            if (edge != null) {
                                move(gesture, item, edge, info, time)
                                continue
                            }
                        }
                        val handle = info.main(position + gesture.handle)
                        val distance = if (physicalDirection < 0) handle else info.viewportSize - handle
                        val threshold = min(scrollThreshold, info.viewportSize / 2f)
                        val speed = (1f - distance.coerceAtLeast(0f) / threshold).coerceIn(0f, 1f)
                        val amount = min(info.viewportSize * 2.5f * speed * seconds, limit.coerceAtLeast(0f))
                        if (amount > 0f) {
                            val consumed = layout.scrollBy(amount * direction)
                            gesture.scrollMotion = info.axes(if (info.mainAxisReversed) -consumed else consumed)
                        }
                    }
                }
            }
        }
    }

    private fun move(gesture: Gesture, from: Item, to: Item, info: Info, time: Long) {
        layout.prepareMove(from.index, to.index)
        pending = Move(from.key, from.index, to.index, to.key, info, time)
        gesture.endIndex = to.index
        for (entry in knownIndices.entries) {
            entry.setValue(when {
                entry.value == from.index -> to.index
                from.index < to.index && entry.value in from.index + 1 .. to.index -> entry.value - 1
                to.index < from.index && entry.value in to.index ..< from.index -> entry.value + 1
                else -> entry.value
            })
        }
        onMove.value(from.index, to.index)
    }

    private fun item(info: Info, key: Any): Item? = info.item(key) ?: knownIndices[key]?.let { positions[key]?.item(info, key, it) }

    private fun constrain(info: Info, item: Item, position: Offset): Offset {
        if (!settleAfterAnimation) return position
        val crossSize = if (info.orientation == Orientation.Vertical) item.size.width else item.size.height
        return info.axes(
            info.main(position).coerceIn(0f, (info.viewportSize - info.main(item.size)).coerceAtLeast(0f)),
            info.cross(position).coerceIn(0f, (info.crossAxisSize - crossSize).coerceAtLeast(0f)),
        )
    }

    private fun scrollDirection(info: Info, gesture: Gesture): Int {
        if (!gesture.hasDragged || info.viewportSize <= 0f || scrollThreshold <= 0f) return 0
        val handle = info.main(gesture.origin + gesture.delta + gesture.handle)
        val threshold = min(scrollThreshold, info.viewportSize / 2f)
        val physical = when {
            handle < threshold -> -1
            handle > info.viewportSize - threshold -> 1
            else -> 0
        }
        val direction = if (info.mainAxisReversed) -physical else physical
        return when {
            direction < 0 && info.canScrollBackward -> -1
            direction > 0 && info.canScrollForward -> 1
            else -> 0
        }
    }

    private fun finish(gesture: Gesture, item: Item?) {
        val from = gesture.startIndex
        val to = gesture.endIndex
        val callback: (() -> Unit)? = if (from != -1 && from != to) ({ onSettle.value(from, to) }) else null
        gestures = gestures.drop(1)
        if (item == null) {
            gesture.pin?.release()
            callback?.invoke()
            return
        }
        val info = layout.info
        val position = constrain(info, item, gesture.origin + gesture.delta)
        val base = positions[gesture.key]?.position() ?: info.position(item)
        val drop = Drop(gesture.key, position - base, if (settleAfterAnimation) callback else null, gesture.pin)
        drops = drops + drop
        if (!settleAfterAnimation) callback?.invoke()
        if (drop !in drops) return
        drop.job = scope.launch {
            cleaning({
                if (drop in drops) drops = drops - drop
                drop.release()
            }) {
                drop.animation.animateTo(Offset.Zero, spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = Offset.VisibilityThreshold,
                ))
                commit(drop)
            }
        }
    }

    private fun commit(drop: Drop) {
        val callback = drop.commit
        drop.commit = null
        callback?.invoke()
    }

    private fun removeDrop(drop: Drop) {
        drops = drops - drop
        drop.release()
        drop.job?.cancel()
    }

    // 终止当前拖动及回落动画，不提交 onSettle；已完成的 onMove 不会回滚
    fun cancelDrag() {
        generation = Any()
        val previousGestures = gestures
        gestures = []
        for (gesture in previousGestures) gesture.pin?.release()
        pending = null
        driver?.cancel()
        driver = null
        val previous = drops
        drops = []
        for (drop in previous) {
            drop.commit = null
            drop.release()
            drop.job?.cancel()
        }
        knownIndices.clear()
    }

    internal fun unregister(key: Any, position: ReorderingItemPosition) {
        if (positions[key] === position) {
            positions -= key
            keys -= key
            if (!isFloating(key)) knownIndices -= key
        }
    }
}
