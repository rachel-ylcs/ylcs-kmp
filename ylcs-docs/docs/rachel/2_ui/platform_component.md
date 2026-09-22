# 平台原生组件

`love.yinlin.compose:platform-view` 把 Android View、Swing Component、UIKit UIView 和 Web HTMLElement 接入同一套公共模型。它解决宿主创建/更新/释放问题，但最重要的设计决策仍由组件作者完成：状态究竟属于 Compose，还是属于原生控件。

## 平台映射

| 目标 | `PlatformView<V>` 的 V | Compose 宿主 |
| --- | --- | --- |
| Android | `android.view.View` | `AndroidView` |
| Desktop | `java.awt.Component` | `SwingPanel` |
| iOS | `UIView` | `UIKitView`，NonCooperative 交互并作为 overlay |
| Web | `HTMLElement` | `HtmlElementView` |

平台类负责实现 `build(...)`；公共 `BasicPlatformView` 缓存唯一 host 实例，并向平台宿主提供更新、重置和释放回调。

## 内部状态模式

简单控件只有可提升参数和事件时，可以把实现藏在 Composable 内并使用 `rememberPlatformView`：

```kotlin
@Composable
fun NativeLabel(text: String, modifier: Modifier = Modifier) {
    val view = rememberPlatformView(text) { currentText ->
        NativeLabelView(currentText)
    }
    view.HostView(modifier)
}
```

传入 factory 的是 `State<S>`，对象本身只创建一次，却能在 `update(view)` 中读取最新参数。不要把变化参数直接捕获在无参数 `remember` 的 factory 中，否则原生控件会永久看到第一次的值。

适用条件：原生控件没有必须由调用方主动操作的历史状态，所有可变输入都能由 Compose 单向写入，事件能通过回调向上报告。

## 外部状态模式

WebView 这类控件拥有自己的导航历史、实际 URL、加载进度，并提供 `goBack`、`evaluateJavaScript` 等命令，必须暴露稳定状态对象：

```kotlin
val state = remember { WebViewState("https://example.com") }

WebView(
    state = state,
    config = WebViewConfig(enableJavaScript = true),
    modifier = Modifier.fillMaxSize()
)

PrimaryButton("后退", enabled = state.canGoBack) {
    state.goBack()
}
```

`config` 是由调用方控制、可以在更新阶段写入的状态；当前 URL 是原生控件也会改变的“失控状态”，唯一可信来源必须是 `WebViewState`。如果把 URL 同时作为普通 Compose 参数并监听后每次 `loadUrl`，原生页面内跳转会与外部状态互相覆盖，甚至形成加载循环。

初始 URL 可以作为状态构造参数；之后的导航用 state 方法发给 host，等待 host 回调再更新可观察 URL。先改外部字段、再命令 host 的做法会在加载失败时制造假状态。

## 更新、复用和释放

实现类按需要附加接口：

```kotlin
class MyPlatformView : PlatformView<NativeView>(),
    Updatable<NativeView>,
    Resettable<NativeView>,
    Releasable<NativeView> {

    override fun update(view: NativeView) { /* 写入最新可控参数 */ }
    override fun reset(view: NativeView) { /* Lazy 容器复用前清空瞬态状态 */ }
    override fun release(view: NativeView) { /* 停止任务、解除监听、释放资源 */ }
}
```

- `Updatable`：每次宿主更新时同步外部可控属性。
- `Resettable`：在可复用容器重新使用实例前清理内容。
- `Releasable`：离开组合时解除 listener、关闭媒体/网页等资源。

Desktop 的 `SwingPanel` 没有同样的 reset/release 参数，框架会在 `DisposableEffect` 中调用 release。所有平台都应让 release 幂等。

## 等待 host 建立

重组和原生 host 创建没有固定先后。`Monitor` 把 host 本身也放入 effect key，避免参数先变化、host 后创建时漏掉命令：

```kotlin
view.Monitor(command) { host ->
    applyCommand(host, command)
}
```

直接 `LaunchedEffect(command) { view.host?... }` 可能在 host 仍为空时什么都不做，之后也不会重启。

## 实现检查表

1. 列出每个状态的唯一所有者：Compose、state object 或原生 host。
2. `build` 只创建实例与固定监听，不执行每次更新逻辑。
3. `update` 只写可外部控制的属性，且能重复调用。
4. host 回调更新外部可观察状态，但不再次触发同一命令。
5. listener、协程、媒体、网页和平台句柄在 `release` 中清理。
6. 检查 iOS overlay、Desktop Swing 混合和 Web DOM 的绘制/输入层级。

平台组件的难点不是写四份 factory，而是维护单一事实来源。先画清状态流，再写 `expect/actual`，通常会比在重组问题出现后补条件判断更省力。
