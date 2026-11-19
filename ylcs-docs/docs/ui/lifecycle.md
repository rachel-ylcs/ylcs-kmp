---
description: 理解 Rachel 应用从平台宿主、服务初始化到页面组合与清理的完整生命周期。
---

# 入口与生命周期

<p class="rc-lead"><code>PlatformApplication</code> 把共享应用连接到 Android、JVM Desktop、Native Desktop、Web 与 iOS。共享层定义主题、根内容与启动服务，平台层负责系统上下文、界面容器与平台事件。</p>

## 共享应用结构

```kotlin title="共享应用"
open class MainApplication(context: PlatformContext) :
    PlatformApplication<MainApplication>(appReference, context) {

    override val themeMode = ThemeMode.SYSTEM

    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenHome> {
            screen(::ScreenHome)
        }
    }
}
```

`ComposedLayout()` 安装主题、根背景、`LocalPlatformContext` 与附加 `Local`。标准平台入口已经调用该方法，`Content()` 内不需要重复配置根主题。

## 启动阶段

| 阶段 | 行为 |
| --- | --- |
| `initApplication(scope)` | 初始化应用引用，调用 `initPool`，构建同步服务并调度异步服务 |
| `initPoolLater(scope)` | 在平台选定的界面阶段调度各服务的 `initLater` |
| `destroyPoolBefore()` | 按逆拓扑顺序执行服务的 `destroyBefore` |
| `destroyPool()` | 按逆拓扑顺序执行服务的 `destroy` |

异步服务不会阻塞根内容的首次组合。第二阶段也不代表所有系统句柄都已可用，例如 iOS 控制器视图可能尚未挂接窗口。

## 应用服务

服务通过 ID 建立依赖，并区分同步构建与异步初始化：

```kotlin title="异步服务"
class SessionStartup(pool: StartupPool) : AsyncStartup(pool) {
    override suspend fun init() = restoreSession()
    override suspend fun initLater() = prepareHostIntegration()

    override fun destroyBefore() = detachHostIntegration()
    override fun destroy() = closeSession()

    class Factory : AsyncStartupFactory<SessionStartup>() {
        override val id: String = StartupID<SessionStartup>()
        override val dependencies: List<String> = [StartupID<StartupKV>()]
        override fun build(pool: StartupPool) = SessionStartup(pool)
    }
}
```

| 类型 | 构造与初始化 |
| --- | --- |
| `SyncStartup` | 构造或工厂构建阶段完成必要初始化 |
| `AsyncStartup` | 在挂起 `init()` 中完成准备 |
| `initLater` | 在平台选定的界面阶段执行 |
| `destroyBefore` | 在主要销毁前执行 |
| `destroy` | 最终清理 |

### 注册与读取

```kotlin title="注册"
private val kvService by startup(StartupKV.Factory(configPath))
val kv get() = kvService.kv

val session by startup(SessionStartup.Factory())
val player by startupLazy(StartupMusicPlayer.Factory())
```

| 读取方式 | 语义 |
| --- | --- |
| `startup(...)` | 要求服务已经存在；异步初始化未完成时不能提前读取 |
| `startupLazy(...)` | 返回可空服务，完成前为 `null` |
| `StateStartupMap` | Compose 应用使用，可通知观察方 |

启动池解析工厂列表后停止接受注册。运行过程中新增 `startup` 或 `startupLazy` 声明会失败。

### 执行规则

1. `initPool` 使用 `DependencyAnalyzer` 拓扑排序，检查未知依赖与依赖环；
2. 按拓扑顺序构建同步服务，同步服务的依赖必须全部属于同步服务；
3. 调度异步服务，各任务等待自己的异步依赖完成后再构建并调用 `init()`；
4. `initPoolLater` 并行调度 `initLater()`，等待该服务已有的初始化任务；
5. 销毁阶段按逆拓扑顺序清理已经创建的服务。

第二阶段不是全体服务的统一屏障。若服务需要另一服务的后期准备结果，应显式设计同步协议。

!!! warning "销毁异常会中断后续清理"
    当前销毁循环遇到异常可能提前结束。关键数据应在变化时写入，不要把全部持久化推迟到应用退出。

### 一次性任务

```kotlin title="async"
private val preparePaths by async(id = "preparePaths") {
    dataPath.mkdir()
    cachePath.mkdir()
}

private val prepareCache by async(
    id = "prepareCache",
    dependencies = ["preparePaths"],
) {
    restoreCache()
}
```

`sync` 注册普通同步动作，`async` 注册挂起动作。声明顺序不替代依赖关系。

## 平台宿主

### Android

```kotlin title="Android 入口"
class AndroidApplication : ComposeApplication() {
    override fun buildInstance() = MainApplication(this)
}

class MainActivity : ComposeActivity()
```

`ComposeApplication.onCreate()` 创建共享实例并执行第一阶段初始化。`ComposeActivity.onCreate()` 设置当前 Activity、启用 edge-to-edge、调度第二阶段初始化、转交启动 Intent 并安装 Compose 内容。

后续 Intent 通过 `onNewIntent` 转交给共享应用的平台实现。平台源集可以覆写 `onIntent(intent)`，将外部地址解析为 `Uri` 后调用 `DeepLink.navigate(uri)`。

Activity 销毁时调用 `destroyPoolBefore()`；应用宿主的 `onTerminate()` 调用 `destroyPool()` 并取消应用协程域。持久化不应依赖进程最终退出回调。

### Desktop

```kotlin title="Desktop 入口"
class DesktopApplication : MainApplication(PlatformContext.Instance) {
    override val title = "示例应用"
    override val initSize = DpSize(1200.dp, 700.dp)
    override val minSize = DpSize(360.dp, 640.dp)
}

fun main() {
    DesktopApplication().run()
}
```

平台配置包括 `commandArgs`、标题、图标、初始尺寸、最小尺寸、圆角、Swing 混合参数与标题栏。主窗口首次进入组合时执行第二阶段初始化；退出主窗口前执行 `destroyPoolBefore()`，窗口循环结束后执行 `destroyPool()`。

### Native Desktop

Windows、Linux、macOS 的 Native 入口同样调用 `run()`，通过移植的 `NativeWindow` 与 SDL 事件循环运行，不依赖 JVM。主窗口在 `onOpen` 中连接 `WindowController` 并填入平台句柄，首次组合时调度第二阶段服务，在 `onClose` 中清空句柄并解除控制器绑定。

除标题、图标与尺寸外，还支持 `initPosition`、`initPlacement`、`transparent`、`undecorated`、`focusable`、`resizable`、`clickThrough` 等初始设置。`transparent` 是创建参数，不能作为普通运行期状态修改。

### Web

```kotlin title="Web 入口"
fun main() {
    MainApplication(PlatformContext.Instance).run()
}
```

JS 与 WasmJS 共用 Web 实现，渲染到 `document.body`。第二阶段初始化挂接到 `window.onload`，清理挂接到 `window.onclose`；这些钩子的实际触发取决于脚本装载方式与浏览器生命周期。刷新会创建新的应用实例。

### iOS

```kotlin title="iOS 入口"
class IOSApplication : MainApplication(PlatformContext.Instance)
```

Swift 宿主持有该实例，先调用 `run()`，再通过 `buildUIViewController()` 获取控制器。`buildUIViewController()` 创建父容器，将 Compose 子控制器挂载到容器边界，并在 `viewDidLoad()` 后调度 `initPoolLater()`。

视图加载不等于已经关联 `UIWindow`。需要窗口的能力应处理 `viewIfLoaded?.window` 为空的情况。

## 独立窗口

`ApplicationWindow.Window(content)` 建立一个平台窗口，`Content()` 负责窗口内容。JVM 可使用官方 Compose `Window`，Native 可使用 `NativeWindow` 扩展。内容自动继承应用的主题、字体与 `LocalPlatformContext`。

| 行为 | 说明 |
| --- | --- |
| 打开 | 每次创建新的 `ApplicationWindow` 实例 |
| 关闭 | 系统关闭事件调用该实例的 `close()` |
| 作用域 | 窗口激活后可用；离开组合时取消并调用 `uninitialize()` |
| 主窗口退出 | 停止接受新窗口并关闭已管理的窗口 |

Android 使用 `openWindow(window, owner: LifecycleOwner)`，可由服务生命周期持有系统悬浮窗。Web 与 iOS 当前没有对应的 `openWindow` 实现。

## 公共平台操作

| 接口 | 作用 |
| --- | --- |
| `backHome()` | Android 返回系统桌面；Desktop 最小化；其他平台按实现处理 |
| `openUri(uri)` | 请求系统或浏览器打开地址 |
| `copyText(text)` | 请求写入平台剪贴板 |
| `implicitFileUri(uri)` | 创建平台可解释的文件 URI 包装 |

返回值反映当前实现能报告的结果。例如 Web 剪贴板写入是异步浏览器请求，公共 Boolean 返回值不代表用户授权与最终写入已经确认。

## 组合示例

```kotlin title="GalleryApplication"
@Stable
abstract class GalleryApplication<out A : GalleryApplication<A>>(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : PlatformApplication<A>(self, context) {

    override val themeMode: ThemeMode
        get() = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT

    override val mainFontResource = Res.font.font

    override val colorSystem: ColorSystem
        get() = if (galleryPalette == GalleryPalette.Framework) ColorSystem.Default else GalleryColors

    override val toolingTheme = ToolingTheme(enableBallonTip = true)

    init {
        sync { ImageManager.initialize(rawContext, "RachelGallery") }
    }

    @Composable
    final override fun Content() = App()
}
```

`darkMode`、`galleryPalette` 与 `GalleryColors` 来自 Gallery 工程。`sync` 在启动池初始化时执行；可复用的业务服务还应建立对应销毁责任。

## 下一步

继续阅读[主题系统](theme.md)，了解如何为这套生命周期提供一致的视觉与尺寸语义。
