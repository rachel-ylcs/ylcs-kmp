package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.ui.floating.BasicSheet
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.tool.NavigationBack
import love.yinlin.extension.Array
import love.yinlin.extension.parseJson
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmName

@Stable
abstract class BasicScreen : ViewModel() {
    // 依赖 ViewModel 在 Navigation 创建时的属性注入
    lateinit var manager: ScreenManager
        internal set
    // 依赖 ViewModel 在 Navigation 创建时的属性注入
    internal lateinit var uniqueId: String
    // 依赖 Navigation 唤醒时的属性注入
    @PublishedApi
    internal var lastResumeArgs: String? = null

    final override fun addCloseable(closeable: AutoCloseable) = super.addCloseable(closeable)

    final override fun onCleared() {
        // 1. 清理回调
        finalize()
        // 2. 释放数据源
        (this as? DataSource)?.onDataSourceClean()
        // 3. 注销屏幕
        uniqueId.let { manager.unregisterScreen(it) }
        // 4. ViewModel 回收
        super.onCleared()
    }

    /**
     * 首次进入页面的初始化事件
     */
    open suspend fun initialize() { }

    /**
     * 从导航栈中移入栈顶时的恢复事件
     */
    open suspend fun resume() = withResume { }

    /**
     * 页面销毁时的清理事件
     */
    protected open fun finalize() { }

    /**
     * 返回事件
     */
    protected open fun onBack() = pop()

    /**
     * 页面的内容
     */
    @Composable
    protected abstract fun BasicContent()

    /**
     * 页面的浮窗
     */
    @Composable
    protected open fun Floating() { }

    /**
     * FAB 按钮
     */
    protected open val fab: FAB = FAB.Empty

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

    /**
     * 浮窗槽
     */
    val slot = ScreenSlot(viewModelScope)

    val Throwable?.warningTip: Throwable? get() = this?.also { slot.tip.warning(it.message) }
    val Throwable?.errorTip: Throwable? get() = this?.also { slot.tip.error(it.message) }

    @Composable
    internal fun ComposedUI() {
        NavigationBack(onBack = ::onBack)

        BasicContent()

        val immersivePadding = rememberImmersivePadding()
        CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
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

    /**
     * 普通变量监听
     *
     * @param state 非状态形式的变量
     * @param action 变量变化回调
     */
    fun <T> monitor(state: () -> T, action: suspend (T) -> Unit) = launch { snapshotFlow(state).collectLatest(action) }

    /**
     * 启动协程
     */
    fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(context = context, block = block)

    /**
     * 弹出导航栈顶层页面
     */
    fun pop() = manager.pop()

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen> navigate(metaConstructor: () -> S, policy: NavigationPolicy = NavigationPolicy.New) {
        manager.navigate(metaConstructor, policy)
    }

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1> navigate(metaConstructor: (A1) -> S, arg1: A1, policy: NavigationPolicy = NavigationPolicy.New) {
        manager.navigate(metaConstructor, arg1, policy)
    }

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(metaConstructor: (A1, A2) -> S, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.New) {
        manager.navigate(metaConstructor, arg1, arg2, policy)
    }

    /**
     * 导航切换页面
     */
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(metaConstructor: (A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.New) {
        manager.navigate(metaConstructor, arg1, arg2, arg3, policy)
    }

    /**
     * 唤醒回调
     */
    @PublishedApi
    internal inline fun withResumeArgs(block: (JsonArray) -> Unit) {
        val args = lastResumeArgs ?: return
        lastResumeArgs = null
        block(args.parseJson.Array)
    }

    @JvmName("withResume0")
    suspend inline fun withResume(crossinline block: suspend () -> Unit) =
        withResumeArgs { block() }

    @JvmName("withResume1a")
    suspend inline fun <reified A1 : Any> withResume(crossinline block: suspend (A1) -> Unit) =
        withResumeArgs { block(it.a(0)) }

    @JvmName("withResume1n")
    suspend inline fun <reified A1> withResume(crossinline block: suspend (A1?) -> Unit) =
        withResumeArgs { block(it.n(0)) }

    @JvmName("withResume2aa")
    suspend inline fun <reified A1 : Any, reified A2 : Any> withResume(crossinline block: suspend (A1, A2) -> Unit) =
        withResumeArgs { block(it.a(0), it.a(1)) }

    @JvmName("withResume2an")
    suspend inline fun <reified A1 : Any, reified A2> withResume(crossinline block: suspend (A1, A2?) -> Unit) =
        withResumeArgs { block(it.a(0), it.n(1)) }

    @JvmName("withResume2na")
    suspend inline fun <reified A1, reified A2 : Any> withResume(crossinline block: suspend (A1?, A2) -> Unit) =
        withResumeArgs { block(it.n(0), it.a(1)) }

    @JvmName("withResume2nn")
    suspend inline fun <reified A1, reified A2> withResume(crossinline block: suspend (A1?, A2?) -> Unit) =
        withResumeArgs { block(it.n(0), it.n(1)) }

    @JvmName("withResume3aaa")
    suspend inline fun <reified A1 : Any, reified A2 : Any, reified A3 : Any> withResume(crossinline block: suspend (A1, A2, A3) -> Unit) =
        withResumeArgs { block(it.a(0), it.a(1), it.a(2)) }

    @JvmName("withResume3aan")
    suspend inline fun <reified A1 : Any, reified A2 : Any, reified A3> withResume(crossinline block: suspend (A1, A2, A3?) -> Unit) =
        withResumeArgs { block(it.a(0), it.a(1), it.n(2)) }

    @JvmName("withResume3ana")
    suspend inline fun <reified A1 : Any, reified A2, reified A3 : Any> withResume(crossinline block: suspend (A1, A2?, A3) -> Unit) =
        withResumeArgs { block(it.a(0), it.n(1), it.a(2)) }

    @JvmName("withResume3ann")
    suspend inline fun <reified A1 : Any, reified A2, reified A3> withResume(crossinline block: suspend (A1, A2?, A3?) -> Unit) =
        withResumeArgs { block(it.a(0), it.n(1), it.n(2)) }

    @JvmName("withResume3naa")
    suspend inline fun <reified A1, reified A2 : Any, reified A3 : Any> withResume(crossinline block: suspend (A1?, A2, A3) -> Unit) =
        withResumeArgs { block(it.n(0), it.a(1), it.a(2)) }

    @JvmName("withResume3nan")
    suspend inline fun <reified A1, reified A2 : Any, reified A3> withResume(crossinline block: suspend (A1?, A2, A3?) -> Unit) =
        withResumeArgs { block(it.n(0), it.a(1), it.n(2)) }

    @JvmName("withResume3nna")
    suspend inline fun <reified A1, reified A2, reified A3 : Any> withResume(crossinline block: suspend (A1?, A2?, A3) -> Unit) =
        withResumeArgs { block(it.n(0), it.n(1), it.a(2)) }

    @JvmName("withResume3nnn")
    suspend inline fun <reified A1, reified A2, reified A3> withResume(crossinline block: suspend (A1?, A2?, A3?) -> Unit) =
        withResumeArgs { block(it.n(0), it.n(1), it.n(2)) }
}