package love.yinlin.foundation.parser.arguments

sealed interface Argument {
    val size: Int
    val first: String
    val firstOrNull: String?
    operator fun contains(value: String): Boolean
    operator fun get(index: Int): String
    fun toList(): List<String>

    data object Empty : Argument {
        override val size: Int = 0
        override val first: Nothing get() = throw ArgumentStateException("Argument has no value.")
        override val firstOrNull: String? = null
        override fun contains(value: String): Boolean = false
        override fun get(index: Int): Nothing = throw ArgumentStateException("Argument has no value.")
        override fun toList(): List<String> = []
        override fun toString(): String = "Argument.Empty"
    }

    class Single internal constructor(val value: String) : Argument {
        override val size: Int = 1
        override val first: String = value
        override val firstOrNull: String = value
        override fun contains(value: String): Boolean = this.value == value
        override fun get(index: Int): String = if (index == 0) value else throw ArgumentStateException("Argument value index $index is outside 0 until $size.")
        override fun toList(): List<String> = [value]
        override fun equals(other: Any?): Boolean = (other as? Single)?.value == value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = value
    }

    class Values internal constructor(val values: List<String>) : Argument {
        override val size: Int = values.size
        override val first: String = values[0]
        override val firstOrNull: String = values[0]
        override fun contains(value: String): Boolean = value in values
        override fun get(index: Int): String = values.getOrNull(index) ?: throw ArgumentStateException("Argument value index $index is outside 0 until $size.")
        override fun toList(): List<String> = values
        override fun equals(other: Any?): Boolean = (other as? Values)?.values == values
        override fun hashCode(): Int = values.hashCode()
        override fun toString(): String = values.toString()
    }

    companion object {
        private fun build(vararg values: String) = when (values.size) {
            0 -> Empty
            1 -> Single(values[0])
            else -> Values(values.toList())
        }
        private fun build(values: List<String>) = when (values.size) {
            0 -> Empty
            1 -> Single(values[0])
            else -> Values(values.toList())
        }
        operator fun invoke(): Argument = Empty
        operator fun invoke(value: String): Argument = Single(value)
        operator fun invoke(values: List<String>): Argument = build(values)
        operator fun invoke(vararg values: String): Argument = build(*values)
        operator fun of(): Argument = Empty
        operator fun of(value: String): Argument = Single(value)
        operator fun of(vararg values: String): Argument = build(*values)
    }
}
