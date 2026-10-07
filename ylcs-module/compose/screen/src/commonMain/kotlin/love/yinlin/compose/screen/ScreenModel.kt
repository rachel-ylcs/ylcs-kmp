package love.yinlin.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import love.yinlin.extension.cleaning
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Stable
abstract class ScreenModel : ViewModel() {
    private var boundManager: ScreenManager? = null
    private var boundKey: ScreenKey? = null
    private var initialized: Boolean = false
    private var uninitialized: Boolean = false
    private var currentResumeArgs: ScreenArgs? = null

    internal val manager: ScreenManager get() = requireNotNull(boundManager) { "The screen has not been attached to a manager." }
    internal val screenKey: ScreenKey get() = requireNotNull(boundKey) { "The screen has not been attached to a manager." }

    internal val navEntry: NavEntry<ScreenKey> by lazy(LazyThreadSafetyMode.NONE) {
        val key = screenKey
        NavEntry(key = key, contentKey = key.id.toString()) {
            ModelContent()
        }
    }

    // 初始化
    protected open fun initialize() { }

    // 唤醒
    protected open fun resume() = withResume { }

    // 清理
    protected open fun uninitialize() { }

    // 页面内容
    @Composable
    abstract fun ModelContent()

    final override fun onCleared() {
        if (uninitialized) return
        uninitialized = true
        cleaning({ super.onCleared() }, ::uninitialize)
    }

    final override fun addCloseable(closeable: AutoCloseable) = super.addCloseable(closeable)

    internal fun attach(manager: ScreenManager, key: ScreenKey) {
        require(boundManager == null) { "The screen is already attached to a manager." }
        boundKey = key
        boundManager = manager
    }

    internal fun initializeOnce() {
        if (initialized) return
        initialized = true
        initialize()
    }

    @PublishedApi
    internal fun requireResumeArgs(argumentCount: Int): ScreenArgs {
        val args = requireNotNull(currentResumeArgs) { "withResume must be called during a Resume callback." }
        require(args.size >= argumentCount) { "Expected at least $argumentCount Resume arguments, but received ${args.size}." }
        return args
    }

    internal fun dispatchResume(args: ScreenArgs) {
        val previous = currentResumeArgs
        currentResumeArgs = args
        cleaning({ currentResumeArgs = previous }, ::resume)
    }

    @PublishedApi
    internal fun activeManager(): ScreenManager {
        val owner = manager
        require(owner.isActive(screenKey.id)) { "The screen entry is no longer active." }
        return owner
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

    inline fun <reified S : ScreenModel> navigate(metaConstructor: () -> S, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, policy)

    inline fun <reified S : ScreenModel, reified A1> navigate(metaConstructor: (A1) -> S, arg1: A1, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, arg1, policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2> navigate(metaConstructor: (A1, A2) -> S, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, arg1, arg2, policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3> navigate(metaConstructor: (A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, arg1, arg2, arg3, policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4> navigate(metaConstructor: (A1, A2, A3, A4) -> S, arg1: A1, arg2: A2, arg3: A3, arg4: A4, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, arg1, arg2, arg3, arg4, policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4, reified A5> navigate(metaConstructor: (A1, A2, A3, A4, A5) -> S, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(metaConstructor, arg1, arg2, arg3, arg4, arg5, policy)

    fun navigate(key: String, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, policy)

    inline fun <reified A1> navigate(key: String, arg1: A1, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, arg1, policy)

    inline fun <reified A1, reified A2> navigate(key: String, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, arg1, arg2, policy)

    inline fun <reified A1, reified A2, reified A3> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, arg1, arg2, arg3, policy)

    inline fun <reified A1, reified A2, reified A3, reified A4> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, arg4: A4, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, arg1, arg2, arg3, arg4, policy)

    inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        activeManager().navigate(key, arg1, arg2, arg3, arg4, arg5, policy)

    // 执行所属导航栈的返回操作，保留最后一个页面
    fun pop(): Boolean = activeManager().pop()

    protected inline fun withResume(block: () -> Unit) {
        requireResumeArgs(0)
        block()
    }

    protected inline fun <reified A1> withResume(block: (A1) -> Unit) {
        val args = requireResumeArgs(1)
        block(args[0])
    }

    protected inline fun <reified A1, reified A2> withResume(block: (A1, A2) -> Unit) {
        val args = requireResumeArgs(2)
        block(args[0], args[1])
    }

    protected inline fun <reified A1, reified A2, reified A3> withResume(block: (A1, A2, A3) -> Unit) {
        val args = requireResumeArgs(3)
        block(args[0], args[1], args[2])
    }

    protected inline fun <reified A1, reified A2, reified A3, reified A4> withResume(block: (A1, A2, A3, A4) -> Unit) {
        val args = requireResumeArgs(4)
        block(args[0], args[1], args[2], args[3])
    }

    protected inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> withResume(block: (A1, A2, A3, A4, A5) -> Unit) {
        val args = requireResumeArgs(5)
        block(args[0], args[1], args[2], args[3], args[4])
    }
}
