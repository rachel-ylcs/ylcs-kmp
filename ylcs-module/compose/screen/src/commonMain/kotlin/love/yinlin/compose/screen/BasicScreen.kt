package love.yinlin.compose.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import love.yinlin.compose.ui.floating.*
import love.yinlin.compose.ui.tool.NavigationBack

@Stable
abstract class BasicScreen : ScreenModel() {
    // 返回事件
    protected open fun onBack() { pop() }
    // 页面内容
    @Composable
    protected abstract fun BasicContent()

    // 浮窗
    @Composable
    protected open fun Floating() { }

    // FAB
    protected open val fab: FAB = FAB.Empty

    // 对话框槽
    private val dialogList: MutableList<Dialog<*>> = []

    // 面板槽
    private val sheetList: MutableList<BasicSheet<*>> = []

    protected infix fun <D : Dialog<*>> land(instance: D): D {
        dialogList += instance
        return instance
    }

    protected infix fun <S : BasicSheet<*>> land(instance: S): S {
        sheetList += instance
        return instance
    }

    // 浮窗槽
    val slot = ScreenSlot(viewModelScope)

    val Throwable?.warningTip: Throwable? get() = this?.also { slot.tip.warning(it.message) }
    fun Throwable?.warningTipOrPop() {
        if (this == null) pop()
        else slot.tip.warning(this.message)
    }
    fun Throwable?.warningTipOrSuccess(ok: String) {
        if (this == null) slot.tip.success(ok)
        else slot.tip.warning(this.message)
    }
    val Throwable?.errorTip: Throwable? get() = this?.also { slot.tip.error(it.message) }
    fun Throwable?.errorTipOrPop() {
        if (this == null) pop()
        else slot.tip.error(this.message)
    }
    fun Throwable?.errorTipOrSuccess(ok: String) {
        if (this == null) slot.tip.success(ok)
        else slot.tip.error(this.message)
    }

    @Composable
    final override fun ModelContent() {
        NavigationBack(onBack = ::onBack)

        Box {
            BasicContent()

            // FAB Layout
            fab.Land()

            // Sheet Land
            for (instance in sheetList) instance.Land()

            // Dialog Land
            for (instance in dialogList) instance.Land()

            // Custom Floating Land
            Floating()

            // Default Dialog Land
            with(slot) {
                info.Land()
                confirm.Land()
                loading.Land()
                tip.Land()
            }
        }
    }
}