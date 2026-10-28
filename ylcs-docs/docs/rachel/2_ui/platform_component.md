# 平台原生组件

`compose:platform-view` 将 Android View、Swing Component、UIKit UIView 和 DOM 元素接入 Compose。平台组件由稳定的 state、参数快照和 `PlatformViewHost` 组成；state 可以由组合位置持有，也可以由 Screen/ViewModel 持有。

## 平台宿主

| 平台 | 原生类型 | Compose 宿主 |
| --- | --- | --- |
| Android | `android.view.View` | `AndroidView` |
| JVM Desktop | `java.awt.Component` | `SwingPanel` |
| iOS | `UIView` | `UIKitView` |
| Web | `HTMLElement` | `HtmlElementView` |

每个平台提供 `PlatformViewState<V, P>`，继承公共 `BasicPlatformViewState<V, P>`。工厂通过 `PlatformViewEnvironment` 获取当前宿主环境；Android 包含 SDK 工厂传入的 Context、LifecycleOwner 和可空 ActivityResultRegistry，其他平台包含 LifecycleOwner。环境属于该视图，不能长期保存在跨宿主的 state 中。

Windows/Linux/macOS Native 当前可使用公共状态管理层，但尚未提供原生控件嵌入的宿主实现；它们与 JVM 的 Swing 宿主不同。

## 定义平台组件

以下示例位于 Android 源集。消费端仅传入声明式参数、state 和事件回调，不接触工厂与清理逻辑：

```kotlin
@Stable
data class NativeLabelParameters(val text: String, val onClick: () -> Unit)

@Stable
class NativeLabelState : PlatformViewState<TextView, NativeLabelParameters>() {
    override fun factory(environment: PlatformViewEnvironment): TextView = TextView(environment.context)

    override fun attach(view: TextView) {
        view.setOnClickListener {
            onView(view) { parameters.onClick() }
        }
    }

    override fun synchronize(view: TextView, previous: NativeLabelParameters?, next: NativeLabelParameters) {
        if (previous?.text != next.text) view.text = next.text
    }

    override fun detach(view: TextView) {
        view.setOnClickListener(null)
    }
}

@Composable
fun NativeLabel(
    text: String,
    modifier: Modifier = Modifier,
    state: NativeLabelState = rememberPlatformViewState { NativeLabelState() },
    onClick: () -> Unit = {},
) {
    PlatformViewHost(state, NativeLabelParameters(text, onClick), modifier)
}
```

`factory(environment)` 创建一个视图；`attach(view)` 安装该视图的监听和绑定；`synchronize(view, previous, next)` 在首次挂载或参数快照改变时同步属性。普通重组不会重新创建视图。参数快照应采用值语义，初次同步时 `previous` 为 null。组件实现自行比较各字段，因此回调变化不会重新执行加载网页等昂贵操作。

在 `synchronize` 中只读取传入的参数快照，避免读取其他可观察状态而让平台 SDK 的 update 订阅无关变化。state 构造参数属于初始配置；每次传给 Host 的参数属于可变输入。

`parameters` 始终指向当前宿主最近提交的参数，事件回调通过它获取最新 lambda。`onView(view)` 将事件投递到 UI 线程，并检查是否仍然属于同一个活动视图，已经退场的旧视图不能继续更新 state 或调用外部回调。原生对象的读取也应放在此检查内部。

`withView { ... }` 用于向当前活动视图发送命令，未挂载时不执行。需要在挂载前接收的命令或输入，应先保存在 state 中，在首次 `synchronize` 时应用。以 `launch(view) { ... }` 启动的子任务在宿主退场或 state 释放时取消；阻塞解码仍需显式切换到 IO/CPU 上下文。

## state 与视图的生命周期

| 操作 | 含义 |
| --- | --- |
| `attach(view)` | 绑定一个新建的原生视图，安装监听器 |
| `detach(view)` | 释放该原生视图的监听、任务及专属资源 |
| `release()` | 永久结束 state 的所有权，不能再次挂载 |
| `releaseResources()` | state 结束且所有视图实际释放后，清理 state 所有的播放器、缓存等资源 |
| `isActive` | 当前存在可用的活动宿主；宿主退场或 state 释放后为 false |

同一 state 同时只能有一个活动宿主。宿主环境改变或重新进入组件树会创建新视图并重新同步参数；Android 的旧 Activity Context 不会跟随 state 被复用。组件默认不启用原生视图池。

离开组件树先使绑定失效、取消子任务，原生 SDK 确认移除视图后才调用 `detach(view)`。JVM Swing 等待组件实际从显示层级移除。退出动画仍持有的组合和视图继续保留，不在页面开始 pop 时提前销毁它们。

`release()` 会立即禁止新挂载、命令和回调，但不会提前销毁仍被宿主使用的原生视图。SDK 的延迟释放结束后，框架才调用 `releaseResources()`，并清空原生引用。清理异常不会阻止框架清空其余绑定。原生监听、权限 launcher、强引用的弱委托和平台观察者仍由组件在 `detach` 中逐项解除；可使用 `cleanup(...)` 确保某一步失败后其他步骤仍执行。

## 组合持有与外部持有

已有 Screen、ViewModel、controller 或父 state 时，内部 state 优先作为其成员保存，由所有者显式调用 `release()`；Composable 只负责使用该成员挂载视图。宿主临时移除时只释放视图，重新挂载继续使用同一个 state。实际原生视图仍由 SDK 工厂创建，不在所有者构造时创建或缓存平台 Context。

没有独立上层所有者的纯组件可以使用 `rememberPlatformViewState { ... }`。此时组合位置拥有 state，离开组件树或组合被放弃时自动调用 `release()`。

由 Screen/ViewModel 持有时，Host 只借用 state。临时隐藏、切换宿主或 Activity 重建会释放旧视图，但不释放 state；仍可在新宿主重新挂载。所有者在结束时显式释放：

```kotlin
private val browser = WebViewState("https://example.com")

override fun uninitialize() {
    browser.release()
}

@Composable
override fun Content() {
    WebView(state = browser, modifier = Modifier.fillMaxSize())
}
```

Screen 在 `uninitialize()` 中调用 `release()`；直接持有 state 的 ViewModel 则在其清理回调中调用。不要将 Activity、UIView 或 DOM 元素放入可保存状态；Android 系统杀进程后的恢复仍需另存必要的 URL 等小型可序列化数据。

WebView 保留现有 API：通过 `state.url = newUrl` 发起导航，通过 `goBack()`、`goForward()`、`evaluateJavaScript()` 执行命令；原生状态通过 `loadingState`、`title`、`icon`、`canGoBack`、`canGoForward` 和 `error` 读取。网页内部跳转产生的输出不能直接反馈成新的加载命令。

## 从旧接口迁移

旧的 `PlatformView`、`rememberPlatformView`、`HostView`、`Monitor` 和独立的更新/释放接口由新的 state + Host API 替代。创建逻辑迁到 `factory`，监听安装迁到 `attach`，按字段的差异同步迁到 `synchronize`，原生视图清理迁到 `detach`。公开 `release` 表示释放 state，不能用于手动销毁仍被 Host 持有的视图。

iOS 默认使用协作交互且不强制 overlay；确有需要的组件可以覆盖 `properties`。相机预览与视频层不拦截 Compose 控件触摸，WebView 使用非协作交互。Android、Swing、UIKit 和 DOM 的混合绘制及输入行为仍需各平台验证。
