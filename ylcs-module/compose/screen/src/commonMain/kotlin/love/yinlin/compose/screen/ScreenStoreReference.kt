package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewmodel.ViewModelStoreProvider

@Stable
internal class ScreenStoreReference(private val token: ViewModelStoreProvider.ReferenceToken) : AutoCloseable {
    private var closed: Boolean = false

    override fun close() {
        if (closed) return // lifecycle未检查是否已经关闭
        closed = true
        token.close()
    }
}
