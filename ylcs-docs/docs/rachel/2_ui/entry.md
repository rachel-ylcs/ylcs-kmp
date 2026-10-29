# 应用入口与生命周期

`PlatformApplication` 将共享应用连接到 Android、JVM Desktop、Native Desktop、Web 与 iOS 宿主。共享层定义主题、根内容和启动服务，平台层负责系统上下文、界面容器与平台事件。

## 共享应用结构

```kotlin
open class MainApplication(context: PlatformContext) :
    PlatformApplication<MainApplication>(appReference, context) {

    override val themeMode = ThemeMode.SYSTEM

    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenHome> {
            screen(::ScreenHome)
            screen(::ScreenDetails)
        }
    }
}
```

`appReference` 使用[快速开始](../1_start/entry.md)中的延迟引用。`ComposedLayout()` 安装主题、根背景、`LocalPlatformContext` 和 `localProvider` 中的附加 Local。标准平台入口已经调用该方法。

## 启动阶段

| 阶段 | 行为 |
| --- | --- |
| `initApplication(scope)` | 初始化应用引用并调用 `initPool`，构建同步服务、调度异步服务 |
| `initPoolLater(scope)` | 在平台选定的界面阶段调度各服务的 `initLater` |
| `destroyPoolBefore()` | 按服务列表逆拓扑顺序执行 `destroyBefore` |
| `destroyPool()` | 按服务列表逆拓扑顺序执行 `destroy` |

异步服务不会阻塞全部根内容的首次组合。第二阶段也不代表所有系统句柄均已可用，例如 iOS 控制器视图可能尚未挂接窗口。服务的等待关系和可空读取方式见[应用服务](service.md)。

## Android

```kotlin
class AndroidApplication : ComposeApplication() {
    override fun buildInstance() = MainApplication(this)
}

class MainActivity : ComposeActivity()
```

Manifest 应指定这两个宿主类型。`ComposeApplication.onCreate()` 创建共享实例并执行第一阶段初始化。

`ComposeActivity.onCreate()` 设置当前 Activity、启用 edge-to-edge、调度第二阶段初始化、转交启动 Intent，并安装 Compose 内容。后续 Intent 经 `onNewIntent` 转交给共享应用的平台实现。

Android 源集可覆写 `PlatformApplication.onIntent(intent)`，将外部地址解析为公共 `Uri` 后调用 `DeepLink.navigate(uri)`。应验证 scheme、路径与参数，再执行具体导航。

Activity 销毁时调用 `destroyPoolBefore()`；应用宿主的 `onTerminate()` 调用 `destroyPool()` 并取消应用协程域。持久化数据不应依赖进程最终退出回调，应在数据变化时完成必要写入。

## Desktop

Desktop 源集可以定义平台专属子类：

```kotlin
class DesktopApplication : MainApplication(PlatformContext.Instance) {
    override val title = "示例应用"
    override val initSize = DpSize(1200.dp, 700.dp)
    override val minSize = DpSize(360.dp, 640.dp)
}

fun main() {
    DesktopApplication().run()
}
```

平台配置包括 `commandArgs`、标题、图标、初始尺寸、最小尺寸、圆角、Swing 混合参数与标题栏。运行期主窗口状态由 `WindowController` 管理，独立窗口通过 `openWindow(ApplicationWindow)` 注册；当前类型没有 `MultipleWindow()` 覆写入口。

第一阶段初始化发生在窗口循环建立前，主窗口首次进入组合时通过 `LaunchFlag` 执行第二阶段初始化。退出主窗口前执行 `destroyPoolBefore()`，窗口循环结束后执行 `destroyPool()` 并取消主协程域。

## Native Desktop

Windows、Linux、macOS 的 Native 入口同样调用 `PlatformApplication.run()`，通过移植的 `NativeWindow` 与 SDL 事件循环运行，不依赖 JVM。模块需要启用桌面 Native target，并安装 `libs.plugins.nativeBridge`；构建和分发方式见[构建插件](../1_start/build.md)与 [Native Compose 说明](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/native-compose/README.md)。

Native 主窗口在 `onOpen` 中连接 `WindowController` 并填入平台句柄，首次组合时调度第二阶段服务，在 `onClose` 中清空句柄并解除控制器绑定。除标题、图标与尺寸外，还支持 `initPosition`、`initPlacement`、`transparent`、`undecorated`、`focusable`、`resizable`、`clickThrough` 等初始设置；运行期状态通过控制器更新。`transparent` 是创建参数，不能作为普通运行期状态修改。

## 独立窗口

`ApplicationWindow.Window(content)` 负责建立一个平台窗口，`Content()` 负责窗口内容。JVM 可使用官方 Compose `Window`，Native 可使用 `love.yinlin.compose.NativeWindow` 扩展。内容自动继承应用的主题、字体与 `LocalPlatformContext`。窗口激活后可使用其 `scope`；离开组合时取消该作用域并调用 `uninitialize()`。

每次打开使用新实例，系统关闭事件应调用该实例的 `close()`。主窗口退出时应用停止接受新窗口并关闭其管理的所有窗口。Android 使用 `openWindow(window, owner: LifecycleOwner)`，可由服务生命周期持有系统悬浮窗，详见[浮层与系统悬浮窗](floating.md)。Web 和 iOS 当前没有对应的 `openWindow` 实现。

## Web

```kotlin
fun main() {
    MainApplication(PlatformContext.Instance).run()
}
```

JS 与 WasmJS 共用 `webMain` 实现，使用 `ComposeViewport` 渲染到 `document.body`，并可通过 `enableAccessibility` 配置可访问性。

当前实现将第二阶段初始化挂接到 `window.onload`，将服务清理挂接到 `window.onclose`。这些钩子的实际触发取决于脚本装载方式和浏览器生命周期，不能作为持久化写入的唯一保证。刷新后会创建新的应用实例，需要跨刷新保留的数据应交给持久化服务。

## iOS

Kotlin 侧为 Swift 提供具体子类：

```kotlin
class IOSDelegateApplication : MainApplication(PlatformContext.Instance)
```

Swift 宿主持有该实例，先调用 `run()` 初始化应用，再由 `UIViewControllerRepresentable.makeUIViewController` 返回 `instance.buildUIViewController()`。仓库宿主示例位于 `ylcs-app/iosApp`，Kotlin 桥接类位于 `ylcs-app/app/portal/src/iosMain`。

`buildUIViewController()` 创建 `CMPUIViewController` 父容器，将 Compose 子控制器挂载到容器边界，并在容器 `viewDidLoad()` 后调度 `initPoolLater()`。`PlatformContextProvider.controller` 保存父容器，与返回给 Swift 的控制器相同。

视图加载不等于已关联 `UIWindow`。需要窗口的能力应处理 `viewIfLoaded?.window` 为空的情况。Swift 宿主应持续使用同一个应用实例，避免在界面更新过程中重复调用启动流程。

当前 iOS 入口公开 `run()` 和 `buildUIViewController()`；不能据此推断其具有与 Desktop 完全相同的退出清理流程。

## 公共平台操作

| 接口 | 作用 |
| --- | --- |
| `backHome()` | Android 返回系统桌面；Desktop 最小化；其他平台按实现处理 |
| `openUri(uri)` | 请求系统或浏览器打开地址 |
| `copyText(text)` | 请求写入平台剪贴板 |
| `implicitFileUri(uri)` | 创建平台可解释的文件 URI 包装 |

返回值反映当前实现能报告的结果。例如 Web 剪贴板写入是异步浏览器请求，公共 Boolean 返回值不代表用户权限与最终写入已经确认。

## 入口中的服务与主题组合

Gallery 将主题状态和一次性图片初始化放在共享应用中，平台入口只建立宿主。以下是实际的共享应用实现：

示例来源：[GalleryApplication.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/gallery/src/commonMain/kotlin/love/yinlin/GalleryApplication.kt#L15)。

```kotlin
@Stable
abstract class GalleryApplication<out A : GalleryApplication<A>>(self: BaseLazyReference<A>, context: PlatformContext) : PlatformApplication<A>(self, context) {
    override val themeMode: ThemeMode get() = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT
    override val mainFontResource = Res.font.font
    override val colorSystem: ColorSystem get() = if (galleryPalette == GalleryPalette.Framework) ColorSystem.Default else GalleryColors
    override val toolingTheme = ToolingTheme(enableBallonTip = true)

    init {
        sync { ImageManager.initialize(rawContext, "RachelGallery") }
    }

    @Composable
    final override fun Content() = App()
}
```

`darkMode`、`galleryPalette`、`GalleryColors` 与 `App()` 来自 Gallery 工程，代码片段需要该上下文。`sync` 在启动池初始化时执行；可复用的业务服务还应建立对应销毁责任，不能从这一演示推断全部图片管理器生命周期都已自动处理。

## 源码入口

- `ylcs-module/compose/app/src/commonMain/kotlin/love/yinlin/compose/Application.kt`
- `ylcs-module/compose/app/src/androidMain/kotlin/love/yinlin/compose/ComposeActivity.kt`
- `ylcs-module/compose/app/src/desktopMain/kotlin/love/yinlin/compose/PlatformApplication.desktop.kt`
- `ylcs-module/compose/app/src/webMain/kotlin/love/yinlin/compose/PlatformApplication.web.kt`
- `ylcs-module/compose/app/src/iosMain/kotlin/love/yinlin/compose/CMPUIViewController.kt`
