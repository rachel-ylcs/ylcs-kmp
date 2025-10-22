package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.ui.floating.FloatingDialogConfirm
import love.yinlin.compose.ui.floating.FloatingDialogInfo
import love.yinlin.compose.ui.floating.FloatingDialogLoading
import love.yinlin.compose.ui.floating.Tip

@Stable
class ScreenSlot(scope: CoroutineScope) {
    val tip = Tip(scope)
    val info = FloatingDialogInfo()
    val confirm = FloatingDialogConfirm()
    val loading = FloatingDialogLoading()
}