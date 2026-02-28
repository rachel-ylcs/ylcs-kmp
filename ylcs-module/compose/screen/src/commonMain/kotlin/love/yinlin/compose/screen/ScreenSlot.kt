package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.ui.floating.DialogConfirm
import love.yinlin.compose.ui.floating.DialogInfo
import love.yinlin.compose.ui.floating.DialogLoading
import love.yinlin.compose.ui.floating.Tip

@Stable
class ScreenSlot(scope: CoroutineScope) {
    val tip = Tip(scope)
    val info = DialogInfo()
    val confirm = DialogConfirm()
    val loading = DialogLoading()
}