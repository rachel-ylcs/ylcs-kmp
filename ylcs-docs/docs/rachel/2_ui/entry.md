# 应用入口与生命周期

`PlatformApplication` 把共享的 Compose 根节点接到各平台宿主。共享应用只描述内容、主题和启动服务；平台壳负责创建实例、提供系统上下文、接收外部链接并在正确时机清理。

## 两阶段启动

应用初始化故意分成两段：

| 阶段 | 发生时机 | 适合的工作 |
| --- | --- | --- |
| `initApplication` | 应用实例创建后 | 建立自引用、解析启动图、同步服务初始化、发起异步服务 |
| `initPoolLater` | Activity、窗口或 UIViewController 已就绪 | 权限/选择器注册、依赖 UI 宿主的准备工作 |

对应的关闭也分成 `destroyPoolBefore` 和 `destroyPool`。前者在 UI 宿主消失前运行，后者在整个应用退出时运行。服务的调用顺序均与依赖拓扑相反。

## 共享应用

```kotlin
class MainApplication(
    context: PlatformContext
) : PlatformApplication<MainApplication>(appReference, context) {
    override val themeMode = ThemeMode.SYSTEM

    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenHome> {
            screen(::ScreenHome)
            screen(::ScreenSettings)
        }
    }
}
```

`ComposedLayout` 会安装 `Theme`、背景和 `LocalPlatformContext`，平台入口通常不应绕开它直接调用 `Content()`。

## Android

Android 使用框架提供的 `ComposeApplication` 和 `ComposeActivity`：

```kotlin
class AndroidApplication : ComposeApplication() {
    override fun buildInstance() = MainApplication(this)
}

class MainActivity : ComposeActivity()
```

Manifest 必须把二者设为实际的 application/activity。`ComposeApplication` 在 `onCreate` 中完成第一阶段；`ComposeActivity` 设置当前 Activity、启用 edge-to-edge、执行第二阶段并安装 Compose 内容。

外部 `Intent` 应在应用实例覆写的 `onIntent` 中转换成公共 `Uri`，再交给 `DeepLink.openUri`。启动 Intent 和 `onNewIntent` 都会走同一入口。不要把 Activity 保存到业务单例；`PlatformContextProvider.activity` 会随宿主更新。

Android 的 `Application.onTerminate` 在真实设备上并非可靠的进程退出通知，因此持久数据应在修改时提交，不能只依赖最终 `destroy()`。

## Desktop

Desktop 可以直接构造并运行：

```kotlin
fun main() {
    object : MainApplication(PlatformContext.Instance) {
        override val title = "My App"
        override val initSize = DpSize(1200.dp, 700.dp)
        override val minSize = DpSize(360.dp, 640.dp)
    }.run()
}
```

可覆写项包括图标、初始/最小尺寸、圆角、标题栏、Compose/Swing 混合参数和 `ApplicationScope.MultipleWindow`。动态窗口状态通过 `controller` 修改，例如 `title`、`alwaysOnTop`、最大化、最小化和托盘；构造属性只表示初值。

框架使用无装饰窗口并在 `TopBar` 中实现拖动和窗口操作。主窗口第一次真正进入组合时执行第二阶段初始化；关闭时先清理 UI 相关服务，再退出窗口循环，最后销毁应用服务和主协程域。

## Web

JS 与 WasmJS 共用入口：

```kotlin
fun main() = MainApplication(PlatformContext.Instance).run()
```

`run()` 通过 `ComposeViewport` 渲染，页面装载后进入第二阶段。浏览器刷新就是完整的新应用生命周期；需要跨刷新保留的数据必须写入 Web 平台持久化实现，而不是只放在应用对象中。

## iOS

Kotlin 侧暴露一个可由 Swift 持有的实例：

```kotlin
class IOSDelegateApplication : MainApplication(PlatformContext.Instance) {
    fun onIOSDeepLink(uri: NSURL) = DeepLink.openUri(uri.toUri())
}
```

SwiftUI 侧先启动实例，再创建控制器：

```swift
@main
struct MyApp: App {
    let instance: IOSDelegateApplication

    init() {
        let app = IOSDelegateApplication()
        instance = app
        app.run()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView(instance: instance)
                .ignoresSafeArea(.all)
                .onOpenURL { instance.onIOSDeepLink(uri: $0) }
        }
    }
}
```

`ComposeView` 的 `makeUIViewController` 返回 `instance.buildUIViewController()`。必须一直持有同一个 Kotlin 实例；每次 SwiftUI 更新都新建应用会重复启动服务并丢失全局状态。

## 公共平台操作

`PlatformApplication` 统一提供：

- `backHome()`：Android 回桌面，Desktop 最小化；其他平台按实际实现。
- `openUri(uri)`：交给系统打开链接。
- `copyText(text)`：写入剪贴板。
- `implicitFileUri(uri)`：把公共 URI 包装成平台可解释的文件 URI。

它们返回布尔值时要处理失败，不要假设系统一定存在浏览器、剪贴板或对应协议处理器。

## 生命周期归属

| 状态或工作 | 正确归属 |
| --- | --- |
| 应用级数据库、配置、网络引擎 | `Startup` |
| 页面首次加载和页面任务 | `Screen.initialize` / `viewModelScope` |
| 已存在页面重新置顶 | `Screen.resume` |
| 组合可见期间的副作用 | `LaunchedEffect` / `DisposableEffect` |
| 平台宿主句柄 | `PlatformContextProvider` |

把工作放在正确生命周期，是多平台行为一致的前提。最常见的问题来自在 Composable 重组时创建服务，或在应用启动时访问尚未存在的 Activity/UIViewController。
