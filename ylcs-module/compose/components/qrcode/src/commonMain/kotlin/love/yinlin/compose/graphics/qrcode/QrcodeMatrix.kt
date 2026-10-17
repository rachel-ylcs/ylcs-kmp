package love.yinlin.compose.graphics.qrcode

import androidx.compose.runtime.Stable

@Stable
internal class QrcodeMatrix(
    val size: Int,
    private val modules: BooleanArray,
    private val functions: BooleanArray,
) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    fun isFunction(x: Int, y: Int): Boolean = functions[y * size + x]
}
