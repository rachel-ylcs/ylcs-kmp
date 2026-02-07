package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import love.yinlin.compose.ui.floating.BasicSheet
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.floating.FAB

@Stable
abstract class SubScreen(val parent: BasicScreen) {
    /**
     * 首次进入子页面的初始化事件
     */
    open suspend fun initialize() {}

    /**
     * 切换到子页面的刷新事件
     */
    open suspend fun update() {}

    /**
     * 子页面的内容
     */
    @Composable
    abstract fun Content()

    /**
     * 子页面的浮窗
     */
    @Composable
    open fun Floating() {}

    /**
     * FAB 按钮
     */
    open val fab: FAB = FAB.Empty

    // 对话框槽
    private val dialogList = mutableListOf<Dialog<*>>()

    // 面板槽
    private val sheetList = mutableListOf<BasicSheet<*>>()

    protected infix fun <D : Dialog<*>> land(instance: D): D {
        dialogList += instance
        return instance
    }

    protected infix fun <S : BasicSheet<*>> land(instance: S): S {
        sheetList += instance
        return instance
    }

    // 浮窗槽
    val slot: ScreenSlot get() = parent.slot

    val Throwable?.warningTip: Throwable? get() = with(parent) { this@warningTip.warningTip }
    val Throwable?.errorTip: Throwable? get() = with(parent) { this@errorTip.errorTip }

    @Composable
    internal fun SubComposedFloating() {
        // Sheet Land
        for (instance in sheetList) instance.Land()

        // Dialog Land
        for (instance in dialogList) instance.Land()

        // Custom Floating Land
        Floating()
    }

    /**
     * 普通变量监听
     *
     * @param state 非状态形式的变量
     * @param action 变量变化回调
     */
    fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = parent.monitor(state, action)

    /**
     * 启动协程
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = parent.launch(block = block)

    /**
     * 弹出导航栈顶层页面
     */
    fun pop() = parent.pop()

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen> navigate(metaConstructor: () -> S, policy: NavigationPolicy = NavigationPolicy.New) =
        parent.navigate(metaConstructor, policy)

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1> navigate(metaConstructor: (A1) -> S, arg1: A1, policy: NavigationPolicy = NavigationPolicy.New) =
        parent.navigate(metaConstructor, arg1, policy)

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(metaConstructor: (A1, A2) -> S, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.New) =
        parent.navigate(metaConstructor, arg1, arg2, policy)

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(metaConstructor: (A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.New) =
        parent.navigate(metaConstructor, arg1, arg2, arg3, policy)
}