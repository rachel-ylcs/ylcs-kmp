package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.ui.floating.*

@Stable
class ScreenSlot(scope: CoroutineScope) {
    val tip = Tip(scope)
    val info = DialogInfo()
    val confirm = DialogConfirm()
    val loading = DialogLoading()
}