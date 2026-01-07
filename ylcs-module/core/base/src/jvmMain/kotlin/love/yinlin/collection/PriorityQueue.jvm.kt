package love.yinlin.collection

actual class PriorityQueue<E : Any> actual constructor(private val comparator: Comparator<E>) {
    private val queue = java.util.PriorityQueue<E>(java.util.Comparator<E> { a, b -> comparator.compare(a, b) })

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