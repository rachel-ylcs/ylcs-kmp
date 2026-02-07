package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
abstract class Sheet : BasicSheet<Unit>() {
    fun open() = openFloating(Unit)

    protected open suspend fun initialize() {}

    @Composable
    protected abstract fun Content()

    final override suspend fun initialize(args: Unit) = initialize()

    @Composable
    final override fun Content(args: Unit) = Content()
}