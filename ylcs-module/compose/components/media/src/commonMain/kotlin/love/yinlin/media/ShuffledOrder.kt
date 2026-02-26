package love.yinlin.media

internal class ShuffledOrder(size: Int = 0, start: Int? = null) {
    var indices: List<Int> = List(size) { it }.shuffled()
        private set
    var begin: Int = start ?: indices.firstOrNull() ?: -1
        private set

    fun internalSet(items: List<Int>, start: Int? = null) {
        indices = items
        if (start != null) begin = start
    }

    override fun toString(): String = "ShuffledOrder($begin) [${indices.joinToString(",")}]"
}