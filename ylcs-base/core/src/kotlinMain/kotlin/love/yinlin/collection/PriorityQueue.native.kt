package love.yinlin.collection

actual class PriorityQueue<E : Any> actual constructor(private val comparator: Comparator<E>) {
    private val heap = mutableListOf<E>()

    actual val size: Int get() = heap.size
    actual val isEmpty: Boolean get() = heap.isEmpty()
    actual val isNotEmpty: Boolean get() = heap.isNotEmpty()
    actual val front: E get() = heap.first()
    actual val frontOrNull: E? get() = heap.firstOrNull()

    actual fun push(element: E) {
        heap.add(element)
        var child = heap.size - 1
        var parent = (child - 1) / 2
        while (child > 0 && comparator.compare(heap[child], heap[parent]) < 0) {
            val temp = heap[child]
            heap[child] = heap[parent]
            heap[parent] = temp
            child = parent
            parent = (child - 1) / 2
        }
    }

    actual fun push(elements: Collection<E>) {
        for (element in elements) push(element)
    }

    actual fun pop(): E? {
        if (heap.isEmpty()) return null
        val result = heap[0]
        heap[0] = heap.last()
        heap.removeAt(heap.size - 1)
        var parent = 0
        while (true) {
            var candidate = parent
            val left = 2 * parent + 1
            val right = 2 * parent + 2
            if (left < heap.size && comparator.compare(heap[left], heap[candidate]) < 0) candidate = left
            if (right < heap.size && comparator.compare(heap[right], heap[candidate]) < 0) candidate = right
            if (candidate == parent) break
            val temp = heap[parent]
            heap[parent] = heap[candidate]
            heap[candidate] = temp
            parent = candidate
        }
        return result
    }

    actual fun clear() { heap.clear() }

    actual operator fun iterator(): Iterator<E> = heap.iterator()

    actual fun reverse(): Iterator<E> = heap.reversed().iterator()
}