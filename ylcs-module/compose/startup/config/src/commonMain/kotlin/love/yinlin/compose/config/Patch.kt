package love.yinlin.compose.config

abstract class Patch(val name: String, val version: Int? = null) {
    open val enabled: Boolean = true
    abstract fun attach(): Boolean
}

typealias Patches = List<Patch>

fun patches(vararg patches: Patch): Patches = listOf(*patches)