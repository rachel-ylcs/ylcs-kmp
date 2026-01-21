package love.yinlin.collection

import java.util.Comparator as JvmComparator
import java.util.PriorityQueue as JvmPriorityQueue

actual class PriorityQueue<E : Any> actual constructor(private val comparator: Comparator<E>) {
    private val queue = JvmPriorityQueue<E>(JvmComparator<E> { a, b -> comparator.compare(a, b) })

    actual val size: Int get() = queue.size
    actual val isEmpty: Boolean get() = queue.isEmpty()
    actual val isNotEmpty: Boolean get() = queue.isNotEmpty()
    actual val front: E get() = queue.peek()!!
    actual val frontOrNull: E? get() = queue.peek()

    actual fun push(element: E) { queue.add(element) }
    actual fun push(elements: Collection<E>) { queue.addAll(elements) }
    actual fun pop(): E? = queue.poll()
    actual fun clear() = queue.clear()

    actual operator fun iterator(): Iterator<E> = queue.iterator()
    actual fun reverse(): Iterator<E> = queue.reversed().iterator()
}