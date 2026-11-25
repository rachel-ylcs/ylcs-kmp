---
description: 从共享应用到页面导航，再到 Android、Desktop、Web、iOS 与桌面 Native 入口的完整最小实现。
---

# 编写第一个应用

<p class="rc-lead">下面这个应用只做三件事：显示一句问候、注册一个启动任务、从首页导航到详情页。它足够小，可以完整读懂；又覆盖了 Rachel 应用的关键生命周期。</p>

## 第 1 步：定义共享应用

共享应用负责主题参数、根内容和应用级服务。`LazyStateReference` 让平台入口在启动时注入唯一实例，其他地方通过委托读取。

```kotlin title="HelloApplication.kt"
import androidx.compose.runtime.Composable
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.extension.LazyStateReference
import love.yinlin.compose.ui.text.Text
import love.yinlin.foundation.PlatformContext

private val appReference = LazyStateReference<HelloApplication>()
val app: HelloApplication by appReference

class HelloApplication(
    context: PlatformContext
) : PlatformApplication<HelloApplication>(appReference, context) {

    @Composable
    override fun Content() {
        Text("Hello Rachel")
    }
}
```

| 组成 | 责任 |
| --- | --- |
| `PlatformApplication` | 继承 `Application` 与 `StartupPool`，组合主题、根内容与启动服务 |
| `LazyStateReference` | 只接受首次初始化，保证全局读取到同一个实例 |
| `ComposedLayout()` | 标准入口已调用，负责安装主题、根背景与 `LocalPlatformContext` |

!!! warning "不要在平台入口启动前读取 `app`"
    `appReference` 由平台启动流程初始化。提前读取会失败，也不要用它判断应用是否已经启动。

## 第 2 步：注册启动任务

一次性任务在应用构造阶段注册。任务内容不会在声明位置立即执行，而是由启动池解析依赖后调度。

```kotlin title="启动任务"
private val prepareDirectories by async(id = "prepareDirectories") {
    val cachePath = PlatformFileSystem.cachePath(rawContext, "hello")
    cachePath.mkdir()
}

private val prepareSession by async(
    id = "prepareSession",
    dependencies = ["prepareDirectories"],
) {
    // 在目录准备完成后执行会话初始化。
}
```

!!! tip "显式声明依赖"
    声明顺序不替代依赖关系。异步任务应通过 `dependencies` 明确依赖 ID，避免依赖目录或文件时出现竞争。

应用级服务的完整生命周期、同步与异步服务的区别见[应用服务](../ui/lifecycle.md)。

## 第 3 步：加入页面与导航

把根内容替换为导航宿主，并注册页面工厂。根页面必须能以零参数创建。

```kotlin title="带导航的应用"
@Composable
override fun Content() {
    ScreenManager.Navigation<ScreenHome> {
        screen(::ScreenHome)
        screen(::ScreenDetails)
    }
}

class ScreenHome : Screen() {
    override val title = "首页"

    @Composable
    override fun Content() {
        PrimaryButton("查看详情") {
            navigate(::ScreenDetails, "demo")
        }
    }
}

class ScreenDetails(private val id: String) : Screen() {
    override val title = "详情"

    @Composable
    override fun Content() {
        Text(id)
    }
}
```

`Screen` 在 `BasicScreen` 之上提供标题栏、内容区与底栏；`BasicScreen` 负责返回处理、页面浮层与 FAB。页面生命周期、导航策略与参数恢复见[导航与页面](../ui/navigation.md)。

## 第 4 步：接入平台入口

=== "Desktop"

    ```kotlin title="desktopMain/Main.kt"
    class DesktopApplication : HelloApplication(PlatformContext.Instance) {
        override val title = "Hello Rachel"
    }

    fun main() {
        DesktopApplication().run()
    }
    ```

=== "Android"

    ```kotlin title="androidMain"
    class AndroidApplication : ComposeApplication() {
        override fun buildInstance() = HelloApplication(this)
    }

    class MainActivity : ComposeActivity()
    ```

    Manifest 中的 application 与 activity 应指向上述类型。Activity 负责 edge-to-edge、启动 Intent 与第二阶段服务初始化。

=== "Web"

    ```kotlin title="webMain/Main.kt"
    fun main() {
        HelloApplication(PlatformContext.Instance).run()
    }
    ```

    JS 与 WasmJS 共用 Web 实现，渲染到 `document.body`。刷新会创建新的应用实例，跨刷新数据应交给持久化服务。

=== "iOS"

    ```kotlin title="iosMain"
    class IOSApplication : HelloApplication(PlatformContext.Instance)
    ```

    Swift 宿主持有该实例，先调用 `run()`，再通过 `buildUIViewController()` 获取控制器。视图加载不等于已经关联窗口，需要窗口的能力应处理空值。

=== "Native Desktop"

    ```kotlin title="nativeMain/Main.kt"
    private val applicationReference = LazyReference<NativeHelloApplication>()

    private class NativeHelloApplication(
        override val commandArgs: Array<String>
    ) : HelloApplication(applicationReference, PlatformContext.Instance) {
        override val title = "Hello Rachel"
    }

    fun main(args: Array<String>) = NativeHelloApplication(args).run()
    ```

    Native 入口不依赖 JVM，需要启用桌面 Native 目标并安装 Native Bridge 插件。详见 [Native Compose](../native/index.md)。

## 第 5 步：运行

```powershell title="运行示例"
.\gradlew.bat :ylcs-app:gallery:galleryRun
```

如果你在自己的模块中实现上述示例，请使用该模块实际注册的运行任务。任务名称以模块的 `runs { }` 配置为准。

!!! note "运行前检查"
    - 模块已注册到 `settings.gradle.kts`；
    - 源集继承已显式建立；
    - 依赖位于正确的 `lib` 位置；
    - 平台入口位于对应源集，而不是 `commonMain`。

## 常见错误

| 现象 | 常见原因 |
| --- | --- |
| 启动即崩溃，提示引用未初始化 | 在平台入口调用 `run()` 前读取了应用委托 |
| Android 编译通过但无法启动 | Manifest 未指向 `ComposeApplication` 与 `ComposeActivity` |
| iOS 链接失败 | 检查 Kotlin framework 构建阶段的日志，并确认 Xcode 架构与 Kotlin 目标匹配 |
| 页面找不到 | 页面未在导航 builder 中注册，或根页面需要构造参数 |
| Web 刷新后状态丢失 | 需要跨刷新保留的数据没有进入持久化服务 |

## 下一步

应用可以运行后，继续阅读[多平台配置](platforms.md)，把平台差异收敛到清晰的源集与入口中。
