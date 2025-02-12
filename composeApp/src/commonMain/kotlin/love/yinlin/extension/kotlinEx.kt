package love.yinlin.extension

abstract class UpdateAlways {
	override fun equals(other: Any?) = false
	override fun hashCode(): Int = 0
	override fun toString(): String = this::class.simpleName ?: "UpdateAlways"
}