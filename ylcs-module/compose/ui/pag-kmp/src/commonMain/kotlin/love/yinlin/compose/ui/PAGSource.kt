package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
sealed class PAGSource(internal val block: (PAGSourceScope.() -> Unit)?) {
    @Stable
    class File(val path: String, block: (PAGSourceScope.() -> Unit)? = null) : PAGSource(block)
    @Stable
    class Data(val data: ByteArray, block: (PAGSourceScope.() -> Unit)? = null) : PAGSource(block)
    @Stable
    class Asset(val path: String, block: (PAGSourceScope.() -> Unit)? = null) : PAGSource(block)
}