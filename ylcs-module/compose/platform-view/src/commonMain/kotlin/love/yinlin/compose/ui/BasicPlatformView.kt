package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.concurrent.Lock

/**
 * 对于 PlatformView 目前有两类模式
 * 1. 内部状态模式
 * 当 PlatformView 实现较为简单时, 定义 PlatformViewWrapper 的 private 类实现 PlatformView。
 * 在实际 [Composable] 函数中使用 [rememberPlatformView] 创建 PlatformViewWrapper。
 * 状态可以通过参数传递，交互也可以通过事件回调传递。
 * 2. 外部状态模式
 * 自定义 [Stable] 的 继承自 PlatformViewState 的类，作为参数传递给 [Composable] 函数，要求使用者从外部传入状态对象。
 * 外部状态模式的内部状态分为两类：可感知状态和失控状态。
 * 其中可感知状态可以通过 [Composable] 参数传递，而失控状态或回调必须由 PlatformViewState 对象调用。
 *
 * 例如：对于 WebView 的平台 View 实现，采用外部状态模式，因为 WebView 存在失控状态。
 * 此外，WebView 还需要支持 goBack、evaluateJs 等操作，这也表明 WebView 只能通过外部模式实现。
 * 考虑 WebView 的两个状态：WebViewSettings 是可感知状态，而 url 是失控状态。
 * 因为 WebViewSettings 只能由用户决定，它的变化可以通过状态提升传递到外层。
 * 所以 WebViewSettings 放到 [Composable] 参数, 当然也可以作为 PlatformViewState 的参数，具体取决于 PlatformViewState 是否需要它来更新。
 * 而 url 作为失控状态，不能作为 [Composable] 函数的参数，只能作为 PlatformViewState 的 getter/setter。
 * 因为 url 不仅可以由用户指定，在 WebView 内部的页面切换也会变动 HostView 的内部 url。
 * 如果将 url 作为 [Composable] 参数，并在 WebView 的实现中监听并 load。当其值变动时确实能切换 url，但无法知晓 HostView 的实际 url。
 * HostView 内部的 url 可能因为加载失败仍然保留原 url，也有可能内部点击切换到了新的 url。这会导致自己的 url 和内部的 url 不一致。
 * 也不能根据 HostView 的 url 变化来更改外部 url 的值，这样又会调用 WebView 的 url 加载，陷入状态更新嵌套循环。
 *
 * 初始可以传递一个默认的 url 作为 [Composable] 函数的参数，但切记它的更新不应该导致 WebView 的重组。
 * 1. 用户需要获取 url 的值，必须通过外部 PlatformViewState 调用 getter，这样能够保证信息源一致。
 * 如果需要将这个值作为状态表现（例如外部有个 Text 以 url 作为内容展示），应该在 PlatformView 中通过 [androidx.compose.runtime.mutableStateOf] 定义 url 的状态 stateUrl。
 * 然后在 HostView 的地址回调中更新 stateUrl，这样用户通过 url getter 获取的就是 stateUrl 的值，它始终保持与 HostView 的实际 url 一致。
 * 2. 用户需要更改 url 的值，必须通过外部的 PlatformViewState 去调用 setter、loadUrl 等方法，这样能够保证信息源一致。
 * 即用户应该将新值传递给 HostView，等待 HostView 更新内部值从而导致 stateUrl 的状态变化，而不是直接修改 stateUrl 的值。
 * 此时即使 WebView 发生了错误导致 url 没更新，或被内部点击切换到了其他 url，通过 getter 拿到的 url 仍然是正确的值。
 */

@Stable
abstract class BasicPlatformView<T : Any> {
    @Composable
    abstract fun HostView(modifier: Modifier = Modifier)

    private var hostView: T? by mutableRefStateOf(null)
    private val hostLock = Lock()

    protected fun hostFactory(builder: () -> T): T = hostLock.synchronized {
        hostView ?: builder().also { hostView = it }
    }

    @Suppress("UNCHECKED_CAST")
    protected val hostUpdate: (T) -> Unit = (this as? Updatable<*>)?.let { ::update as (T) -> Unit } ?: {}

    @Suppress("UNCHECKED_CAST")
    protected val hostReset: ((T) -> Unit)? = (this as? Resettable<*>)?.let { ::reset as (T) -> Unit }

    @Suppress("UNCHECKED_CAST")
    protected fun hostRelease(view: T) = hostLock.synchronized {
        (this as? Releasable<*>)?.let { ::release as (T) -> Unit }?.invoke(view)
        hostView = null
    }

    val host: T? get() = hostView
    fun host(block: (T) -> Unit) = hostView?.let(block)

    /**
     * 等同于 LaunchedEffect, 但会监听自身创建。
     * 因为重组是无序的，使用 Monitor 可以避免 HostView 未被创建时就触发了 key 变化而后续不触发的问题。
     */
    @Composable
    fun Monitor(key1: Any?, block: suspend CoroutineScope.(T) -> Unit) {
        LaunchedEffect(hostView, key1) {
            hostView?.let { block(it) }
        }
    }

    @Composable
    fun Monitor(key1: Any?, key2: Any?, block: suspend CoroutineScope.(T) -> Unit) {
        LaunchedEffect(hostView, key1, key2) {
            hostView?.let { block(it) }
        }
    }

    @Composable
    fun Monitor(vararg keys: Any?, block: suspend CoroutineScope.(T) -> Unit) {
        LaunchedEffect(hostView, *keys) {
            hostView?.let { block(it) }
        }
    }
}