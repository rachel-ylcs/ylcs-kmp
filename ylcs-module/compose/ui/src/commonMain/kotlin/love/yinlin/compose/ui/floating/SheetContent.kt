package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Stable

@Stable
abstract class SheetContent<A : Any> : BasicSheet<A>() {
    fun open(args: A) = openFloating(args)
}