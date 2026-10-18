# 平台原生组件

`compose:platform-view` 将 Android View、Swing Component、UIKit UIView 和 DOM 元素接入 Compose。公共 `BasicPlatformView` 管理宿主引用，平台 `PlatformView` 实现负责创建和渲染原生对象。

## 平台宿主

| 平台 | 原生类型 | Compose 宿主 |
| --- | --- | --- |
| Android | `android.view.View` | `AndroidView` |
| JVM Desktop | `java.awt.Component` | `SwingPanel` |
| iOS | `UIView` | `UIKitView` |
| Web | `HTMLElement` | `HtmlElementView` |

Android 的 `build` 接收 Context、LifecycleOwner 和可空 ActivityResultRegistry。其他三个平台使用无参数 `build()`。iOS 当前配置 `NonCooperative` 交互模式，并将原生视图作为 overlay 放置。

## 内部状态模式

参数能够完整描述原生控件、事件能够通过回调输出时，可使用 `rememberPlatformView`：

```kotlin
@Composable
fun NativeLabel(text: String, modifier: Modifier = Modifier) {
    val view = rememberPlatformView(text) { textState ->
        NativeLabelView(textState)
    }
    view.HostView(modifier)
}
```

`NativeLabelView` 是使用方在平台源集中实现的类。带参数重载将参数包装为 `State<S>`，实例通过 `remember` 保留，`update(view)` 读取 `textState.value` 获取最新输入。最多支持四个参数状态。

更新阶段应读取参数 State 中的最新值，避免原生控件持续使用初始值。

## 外部状态模式

具有导航历史、播放进度或主动命令的控件应暴露稳定状态对象。例如：

```kotlin
val state = remember { WebViewState("https://example.com") }

WebView(
    state = state,
    modifier = Modifier.fillMaxSize(),
    config = WebViewConfig(enableJavaScript = true)
)

PrimaryButton("后退", enabled = state.canGoBack) {
    state.goBack()
}
```

WebView 类型位于 `love.yinlin.compose.ui`。`WebViewState` 提供 `url`、`loadingState`、标题、图标、前进与返回状态、错误及命令方法。

当前 API 使用 `state.url = newUrl` 发起导航，使用 `goBack()`、`goForward()` 和 `evaluateJavaScript()` 执行命令。URL 也可能由原生页面内部跳转改变，不能另外维护一份外部 URL 并在每次回调后重复加载。

## 更新、复用与释放

| 接口 | 职责 |
| --- | --- |
| `Updatable<V>` | 将最新受控属性写入宿主 |
| `Resettable<V>` | 在宿主复用时重置瞬态状态 |
| `Releasable<V>` | 解除监听、停止任务并释放原生资源 |

Android、iOS 与 Web 将相应回调传给平台宿主。Desktop 在 `DisposableEffect` 中调用释放逻辑。`hostRelease` 完成后清空缓存宿主引用；释放操作应可靠且可重复处理。

同一个状态或包装实例应对应一个活动宿主，不应同时挂载到多个组合位置。

## 等待宿主

原生宿主的创建时机可能晚于参数变化。`Monitor` 将宿主引用也纳入 effect key：

```kotlin
view.Monitor(command) { host ->
    applyCommand(host, command)
}
```

它会在参数变化或宿主建立时运行。单独使用 `LaunchedEffect(command)` 并读取空的 `host`，可能遗漏后续宿主创建事件。

## 实现约束

创建阶段负责原生实例与固定监听，更新阶段负责可重复写入的受控属性，宿主回调负责上报原生状态。文件、媒体、网页和系统监听等资源应在相应释放路径关闭。

绘制与输入行为需分别验证 Android View、Desktop Swing 混合、iOS overlay 与 Web DOM 的平台差异。

## 源码入口

- `ylcs-module/compose/platform-view/src/commonMain/kotlin/love/yinlin/compose/ui/BasicPlatformView.kt`
- `ylcs-module/compose/platform-view/src/commonMain/kotlin/love/yinlin/compose/ui/PlatformViewWrapper.kt`
- `ylcs-module/compose/components/webview/src/commonMain/kotlin/love/yinlin/compose/ui/`
