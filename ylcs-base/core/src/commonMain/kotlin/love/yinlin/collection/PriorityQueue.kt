package love.yinlin.collection

expect class PriorityQueue<E : Any>(comparator: Comparator<E>) {
    val size: Int
    val isEmpty: Boolean
    val isNotEmpty: Boolean
    val front: E
    val frontOrNull: E?

    fun push(element: E)
    fun push(elements: Collection<E>)
    fun pop(): E?
    fun clear()

    operator fun iterator(): Iterator<E>
}