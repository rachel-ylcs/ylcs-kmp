package love.yinlin.platform.ffi

import java.lang.foreign.Arena
import java.nio.charset.StandardCharsets
import kotlin.use

inline fun <R> useMemory(block: (Arena) -> R) = Arena.ofConfined().use(block)
fun Arena.astr(str: String): Address = allocateFrom(str, StandardCharsets.UTF_8)
fun Arena.wstr(str: String): Address = allocateFrom(str, StandardCharsets.UTF_16LE)

val Address.isNull get() = this == Address.NULL
val Address.isNotNull get() = this != Address.NULL

val Long.asAddress: Address get() = Address.ofAddress(this)
val Address.asLong: Long get() = this.address()
