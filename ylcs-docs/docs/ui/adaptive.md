---
description: 基于窗口尺寸的自适应布局，以及 Android View、Swing、UIKit 与 DOM 的 Compose 宿主。
---

# 自适应与平台视图

<p class="rc-lead">自适应布局的依据是当前窗口容器，而不是操作系统名称。原生视图则通过稳定的 state、参数快照与 host 接入，避免把平台句柄放进可保存状态。</p>

## 设备模型

`Device` 根据窗口内容容器的尺寸描述布局空间：

```kotlin
@Stable
@Serializable
data class Device(val size: Size, val type: Type)
```

| 容器宽度 | `Device.Size` |
| --- | --- |
| 小于等于小断点 | `SMALL` |
| 小断点与大断点之间 | `MEDIUM` |
| 大于大断点 | `LARGE` |

`Device(width, height)` 按宽高比判断形态：

| 条件 | `Device.Type` |
| --- | --- |
| 高度明显大于宽度 | `PORTRAIT` |
| 宽度明显大于高度 | `LANDSCAPE` |
| 其余 | `SQUARE` |

`SQUARE` 表示中间比例区间，不要求宽高相等。只接收宽度的构造器会将宽度等级映射为对应形态。

## 在 Compose 中使用

```kotlin title="设备状态"
val device by rememberDevice()
val size by rememberDeviceSize()
val type by rememberDeviceType()
```

这些函数读取 `LocalWindowInfo.current.containerDpSize` 并返回派生 `State`。`rememberDevice` 使用完整宽高，不采用仅宽度构造器的形态推断。

```kotlin title="布局分支"
@Composable
fun AccountContent() {
    val device by rememberDevice()

    when (device.type) {
        Device.Type.PORTRAIT -> AccountSingleColumn()
        Device.Type.SQUARE -> AccountCompactLayout()
        Device.Type.LANDSCAPE -> AccountWideLayout(
            showAside = device.size == Device.Size.LARGE,
        )
    }
}
```

!!! note "窗口容器不是嵌套约束"
    上述分类对应窗口容器，不直接反映任意嵌套组件的局部约束。局部面板需要独立响应可用空间时，应使用其布局约束计算。

## 面板的形态分支

`BasicSheet` 直接根据 `Device.Type` 选择停靠位置：

```kotlin title="BasicSheet.kt"
override fun alignment(device: Device): Alignment = when (device.type) {
    Device.Type.PORTRAIT -> Alignment.BottomCenter
    Device.Type.LANDSCAPE, Device.Type.SQUARE -> Alignment.CenterEnd
}
```

宽度等级与形态分类分开使用。这里不读取 `Device.Size`，因此不能把“桌面平台”或 `LARGE` 宽度直接当作右侧面板的判断条件。

## 布局验证

- 宽度断点两侧；
- 宽高比例边界；
- 窗口连续缩放；
- 字体缩放与输入法出现后的可用空间；
- 滚动、空数据、错误与禁用状态；
- 安全区与多窗口。

窗口跨越断点时，应保留滚动、焦点与业务状态。需要迁移同一段组合内容时可以使用 `rememberMovableContent`，见 [Compose 核心](../core/compose-core.md)。

## 平台原生组件

`compose:platform-view` 将 Android View、Swing Component、UIKit UIView 与 DOM 元素接入 Compose。

| 平台 | 原生类型 | Compose 宿主 |
| --- | --- | --- |
| Android | `android.view.View` | `AndroidView` |
| JVM Desktop | `java.awt.Component` | `SwingPanel` |
| iOS | `UIView` | `UIKitView` |
| Web | `HTMLElement` | `HtmlElementView` |

每个平台提供 `PlatformViewState<V, P>`，继承公共 `BasicPlatformViewState<V, P>`。工厂通过 `PlatformViewEnvironment` 获取当前宿主环境；环境属于该视图，不能长期保存在跨宿主的 state 中。

Windows、Linux、macOS Native 当前可使用公共状态管理层，但尚未提供原生控件嵌入的宿主实现。

## 定义一个平台组件

```kotlin title="Android 源集"
@Stable
data class NativeLabelParameters(
    val text: String,
    val onClick: () -> Unit,
)

@Stable
class NativeLabelState : PlatformViewState<TextView, NativeLabelParameters>() {
    override fun factory(environment: PlatformViewEnvironment): TextView =
        TextView(environment.context)

    override fun attach(view: TextView) {
        view.setOnClickListener {
            onView(view) { parameters.onClick() }
        }
    }

    override fun synchronize(
        view: TextView,
        previous: NativeLabelParameters?,
        next: NativeLabelParameters,
    ) {
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
    PlatformViewHost(
        state = state,
        parameters = NativeLabelParameters(text, onClick),
        modifier = modifier,
    )
}
```

| 方法 | 责任 |
| --- | --- |
| `factory(environment)` | 创建一个原生视图 |
| `attach(view)` | 安装监听与绑定 |
| `synchronize(view, previous, next)` | 按字段同步参数 |
| `detach(view)` | 释放监听、任务与专属资源 |

普通重组不会重新创建视图。参数快照应采用值语义；首次同步时 `previous` 为 null。组件实现自行比较字段，因此回调变化不会重新执行加载网页等昂贵操作。

`onView(view)` 将事件投递到 UI 线程，并检查是否仍属于同一个活动视图。已经退场的旧视图不能继续更新 state 或调用外部回调。

## state 与视图生命周期

| 操作 | 含义 |
| --- | --- |
| `attach(view)` | 绑定一个新建视图，安装监听器 |
| `detach(view)` | 释放该视图的监听、任务与资源 |
| `release()` | 永久结束 state 的所有权，不能再次挂载 |
| `releaseResources()` | 所有视图实际释放后清理 state 资源 |
| `isActive` | 当前是否存在活动宿主 |

同一 state 同时只能有一个活动宿主。宿主环境改变或重新进入组件树会创建新视图并重新同步参数；Android 的旧 Activity Context 不会跟随 state 被复用。组件默认不启用原生视图池。

## 组合持有与外部持有

已有 Screen、ViewModel、controller 或父 state 时，内部 state 应作为其成员保存，由所有者显式调用 `release()`；Composable 只负责挂载视图。

```kotlin title="页面持有"
private val browser = WebViewState("https://example.com")

override fun uninitialize() {
    browser.release()
}

@Composable
override fun Content() {
    WebView(state = browser, modifier = Modifier.fillMaxSize())
}
```

没有独立上层所有者的纯组件可以使用 `rememberPlatformViewState { ... }`。此时组合位置拥有 state，离开组件树时自动调用 `release()`。

!!! warning "不要保存原生对象"
    不要将 Activity、UIView 或 DOM 元素放入可保存状态。Android 系统杀进程后的恢复仍需另存必要的 URL 等小型可序列化数据。

## WebView

```kotlin title="业务页面"
@Stable
class ScreenWebpage(url: String) : Screen() {
    private val state = WebViewState(url)

    override val title: String by derivedStateOf { state.title }

    override fun uninitialize() {
        state.release()
    }

    override fun onBack() {
        if (state.canGoBack) state.goBack()
        else pop()
    }
}
```

WebView 通过 `state.url = newUrl` 发起导航，通过 `goBack()`、`goForward()`、`evaluateJavaScript()` 执行命令；原生状态通过 `loadingState`、`title`、`icon`、`canGoBack`、`canGoForward` 与 `error` 读取。网页内部跳转产生的输出不能直接反馈成新的加载命令。

JVM 的 WebView 当前仅显示不支持占位，桌面 Native 没有启用该模块。业务如果需要系统浏览器打开，应在平台层显式处理。

## 从旧接口迁移

旧的 `PlatformView`、`rememberPlatformView`、`HostView`、`Monitor` 与独立更新/释放接口由 state + Host API 替代：

| 旧概念 | 新位置 |
| --- | --- |
| 创建视图 | `factory` |
| 安装监听 | `attach` |
| 按字段同步 | `synchronize` |
| 释放原生视图 | `detach` |
| 释放 state | `release` |

公开 `release` 表示释放 state，不能用于手动销毁仍被 Host 持有的视图。

## 下一步

进入 [Native Compose](../native/index.md)，了解桌面原生 Compose 的桥接、窗口与分发。
